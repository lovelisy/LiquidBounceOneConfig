/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.integration.oneconfig.hud

import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.oneconfig.LbText
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.util.ARGB
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import org.polyfrost.compose.minecraft.mc.McPlayerFaces
import org.polyfrost.compose.render.FilterQuality
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Native target HUD.
 *
 * Shows the avatar, the name, the health, the absorption and the armour value of the current combat target, plus the
 * equipped armour pieces with their durability and an animated health bar. The target is the one the kill aura is
 * tracking, or the last entity that was attacked within the last 1.2 seconds. Outside of combat nothing is drawn.
 */
object TargetHud : LbHudSurface("target_hud", "Target HUD", Hud.Category.COMBAT, LbHudPalette.surface(0.68f)) {

    /**
     * Colour of the health bar under the card.
     *
     * The bar keeps the native red by default, but it is an ordinary colour control, so it can be themed together with
     * the accent and the background of the element.
     */
    @Color(title = "Health Bar Color", alpha = true, gradient = true)
    @JvmField
    var healthBarColor: PolyColor = PolyColor(LbHudPalette.HOTBAR_HEALTH_COLOR)

    private fun healthBarArgb(): Int = healthBarColor.rawArgb

    private const val AVATAR_SIZE: Float = 50f
    private const val PADDING: Float = 15f
    private const val GAP: Float = 10f
    private const val BAR_HEIGHT: Float = 8f

    private var lastTarget: LivingEntity? = null
    private var lastTargetMs: Long = 0L
    private var animHealthRatio: Float = 1f

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        val entity = event.entity
        if (entity is LivingEntity && entity.shouldBeAttacked()) {
            lastTarget = entity
            lastTargetMs = System.currentTimeMillis()
        }
    }

    private fun resolveTarget(inEditor: Boolean): LivingEntity? {
        val now = System.currentTimeMillis()

        val auraTarget = if (ModuleKillAura.running) ModuleKillAura.targetTracker.target else null
        if (auraTarget != null && auraTarget.isAlive) {
            lastTarget = auraTarget
            lastTargetMs = now
            return auraTarget
        }

        val cached = lastTarget
        if (cached != null && cached.isAlive && now - lastTargetMs <= TARGET_CACHE_MS) {
            return cached
        }

        return if (inEditor) mc.player else null
    }

    private fun armourSlots(target: LivingEntity): List<ItemStack> = listOf(
        target.getItemBySlot(EquipmentSlot.HEAD),
        target.getItemBySlot(EquipmentSlot.CHEST),
        target.getItemBySlot(EquipmentSlot.LEGS),
        target.getItemBySlot(EquipmentSlot.FEET),
    ).filter { !it.isEmpty }

    private fun absorption(target: LivingEntity): Float = target.absorptionAmount.coerceAtLeast(0f)

    private fun midWidth(target: LivingEntity): Float {
        val nameWidth = textWidth(target.name.string, NAME_SIZE, LbHudFonts.Weight.MEDIUM)
        val statsWidth = (if (absorption(target) > 0f) 3 else 2) *
            (max(STAT_VALUE_MIN_WIDTH, textWidth("00", STAT_SIZE)) + STAT_ICON_SIZE + STAT_ICON_GAP)
        return max(nameWidth, statsWidth)
    }

    private fun armourWidth(slots: List<ItemStack>): Float =
        if (slots.isEmpty()) {
            0f
        } else {
            5f + (slots.size - 1) * armourSlotWidth() +
                ARMOUR_SLOT_SIZE + ARMOUR_DOT_OFFSET + ARMOUR_DOT_SPAN
        }

    private fun armourSlotWidth(): Float = ARMOUR_SLOT_SIZE + ARMOUR_SLOT_GAP

    private fun mainHeight(): Float = max(AVATAR_SIZE + AVATAR_TOP * 2f, NAME_TOP + nameLineHeight() + STAT_TOP_OFFSET)

    private fun nameLineHeight(): Float = lineHeight(NAME_SIZE, LbHudFonts.Weight.MEDIUM)

    private fun cardHeight(): Float = mainHeight() + BAR_HEIGHT

    private fun cardWidth(target: LivingEntity): Float {
        val armour = armourWidth(armourSlots(target))
        return PADDING + AVATAR_SIZE + GAP + midWidth(target) +
            (if (armour > 0f) GAP + armour else 0f) + PADDING
    }

    override fun measure(): HudSize {
        val target = resolveTarget(HudManager.isEditing) ?: return HudSize(sampleWidth(), cardHeight())
        return HudSize(cardWidth(target), cardHeight())
    }

    override fun paint(canvas: LbHudCanvas) {
        val target = resolveTarget(canvas.inEditor)
        if (target == null) {
            if (canvas.inEditor) {
                paintSample(canvas)
            }
            return
        }

        val name = target.name.string
        val health = target.health.coerceAtLeast(0f)
        val absorption = absorption(target)
        val armourValue = target.armorValue
        val slots = armourSlots(target)
        val midWidth = midWidth(target)
        val cardWidth = cardWidth(target)
        val cardHeight = cardHeight()
        val mainHeight = mainHeight()

        canvas.fill(0f, 0f, cardWidth, cardHeight, surfaceArgb(), 5f)

        drawAvatar(canvas, target)

        drawText(canvas, name, PADDING + AVATAR_SIZE + GAP, NAME_TOP, NAME_SIZE, textColor, LbHudFonts.Weight.MEDIUM)

        var statX = PADDING + AVATAR_SIZE + GAP
        val statY = NAME_TOP + nameLineHeight() + STAT_TOP_OFFSET
        statX += drawStat(canvas, statX, statY, floor(health).toInt().toString(), TARGET_HEALTH) + GAP
        if (absorption > 0f) {
            statX += drawStat(canvas, statX, statY, floor(absorption).toInt().toString(), TARGET_ABSORPTION) + GAP
        }
        drawStat(canvas, statX, statY, armourValue.toString(), TARGET_ARMOR)

        if (slots.isNotEmpty()) {
            var armourX = PADDING + AVATAR_SIZE + GAP + midWidth + GAP
            for ((index, stack) in slots.withIndex()) {
                drawArmourStatus(canvas, "target_hud:armour:$index", stack, armourX, AVATAR_TOP)
                armourX += armourSlotWidth()
            }
        }

        val maxHealth = max(1f, target.maxHealth + absorption)
        val targetRatio = ((health + absorption) / maxHealth).coerceIn(0f, 1f)
        animHealthRatio = LbHudPalette.expLerp(animHealthRatio, targetRatio, 10f, canvas.deltaSeconds)
        if (animHealthRatio > 0.002f) {
            canvas.fillCssBox(
                0f,
                mainHeight,
                cardWidth * animHealthRatio,
                BAR_HEIGHT,
                0f,
                0f,
                if (animHealthRatio > 0.98f) 5f else 0f,
                5f,
                healthBarArgb(),
            )
        }
    }

    /**
     * Editor sample card, drawn whenever there is no real target, for example in the HUD browser preview while the
     * main menu is open. It keeps the fixed sample size so OneConfig can fit it inside the preview box.
     */
    private fun paintSample(canvas: LbHudCanvas) {
        val cardWidth = sampleWidth()
        val cardHeight = cardHeight()
        val mainHeight = mainHeight()

        canvas.fill(0f, 0f, cardWidth, cardHeight, surfaceArgb(), 5f)

        val fallback = LbHudTextures.get(STEVE_FACE)
        if (fallback != null) {
            canvas.roundedTexture(
                fallback,
                PADDING, AVATAR_TOP, AVATAR_SIZE, AVATAR_SIZE, 5f,
                0f, 0f, fallback.width.toFloat(), fallback.height.toFloat(),
                -1,
                FilterQuality.None,
            )
        }

        drawText(canvas, LbText.hudString("liquidbounce.hud.target_hud.sample.name", "Player"), PADDING + AVATAR_SIZE + GAP, NAME_TOP, NAME_SIZE, textColor, LbHudFonts.Weight.MEDIUM)

        var statX = PADDING + AVATAR_SIZE + GAP
        val statY = NAME_TOP + nameLineHeight() + STAT_TOP_OFFSET
        statX += drawStat(canvas, statX, statY, SAMPLE_HEALTH, TARGET_HEALTH) + GAP
        drawStat(canvas, statX, statY, SAMPLE_ARMOUR, TARGET_ARMOR)

        canvas.fillCssBox(
            0f,
            mainHeight,
            cardWidth * SAMPLE_HEALTH_RATIO,
            BAR_HEIGHT,
            0f,
            0f,
            0f,
            5f,
            healthBarArgb(),
        )
    }

    private fun drawAvatar(canvas: LbHudCanvas, target: LivingEntity) {
        val hurtFactor = (target.hurtTime / 10f).coerceIn(0f, 1f)
        val dim = (255 * (1f - 0.45f * hurtFactor)).toInt().coerceIn(0, 255)
        val tint = ARGB.color(255, 255, dim, dim)

        val layers = runCatching { McPlayerFaces.layers(target.uuid) }.getOrNull()
        val face = layers?.flattened
        if (face != null && face.width > 0 && face.height > 0) {
            canvas.roundedTexture(
                face,
                PADDING, AVATAR_TOP, AVATAR_SIZE, AVATAR_SIZE, 5f,
                0f, 0f, face.width.toFloat(), face.height.toFloat(),
                tint,
                FilterQuality.None,
            )
            return
        }

        val fallback = LbHudTextures.get(STEVE_FACE) ?: return
        canvas.roundedTexture(
            fallback,
            PADDING, AVATAR_TOP, AVATAR_SIZE, AVATAR_SIZE, 5f,
            0f, 0f, fallback.width.toFloat(), fallback.height.toFloat(),
            tint,
            FilterQuality.None,
        )
    }

    private fun drawStat(canvas: LbHudCanvas, x: Float, y: Float, value: String, icon: String): Float {
        val valueWidth = max(STAT_VALUE_MIN_WIDTH, textWidth(value, STAT_SIZE))
        drawText(canvas, value, x, y, STAT_SIZE, dimTextArgb())
        canvas.icon(icon, x + valueWidth + STAT_ICON_GAP, y + 1.5f, STAT_ICON_SIZE, STAT_ICON_SIZE, textColor)
        return valueWidth + STAT_ICON_GAP + STAT_ICON_SIZE
    }

    private fun drawArmourStatus(canvas: LbHudCanvas, slot: String, stack: ItemStack, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y + 3f)
        val itemScale = ARMOUR_SLOT_SIZE / 16f
        canvas.scale(itemScale, itemScale)
        canvas.item(stack, slot, 0f, 0f, 16f, 16f)
        canvas.restore()

        val activePoints = if (stack.maxDamage > 0) {
            ceil(10.0 - stack.damageValue.toDouble() / stack.maxDamage.toDouble() * 10.0).toInt().coerceIn(0, 10)
        } else {
            10
        }

        val dotX = x + ARMOUR_SLOT_SIZE + 5f
        for (index in 0 until 10) {
            val pointIndex = 10 - index
            val colour = if (pointIndex <= activePoints) {
                accentArgb()
            } else {
                surfaceArgb(0.44f)
            }
            canvas.fill(dotX, y - 3f + index * 4f, 5f, 3f, colour, 1f)
        }
    }

    private val TARGET_HEALTH = "textures/ui/hud/targethud/icon-health.png"
    private val TARGET_ABSORPTION = "textures/ui/hud/targethud/icon-absorption.png"
    private val TARGET_ARMOR = "textures/ui/hud/targethud/icon-armor.png"
    private val STEVE_FACE = "textures/ui/hud/targethud/steve.png"

    private const val NAME_SIZE: Float = 15f
    private const val STAT_SIZE: Float = 14f
    private const val TARGET_CACHE_MS: Long = 1200L

    private const val AVATAR_TOP: Float = 10f
    private const val NAME_TOP: Float = 14f
    private const val STAT_TOP_OFFSET: Float = 4f
    private const val STAT_VALUE_MIN_WIDTH: Float = 18f
    private const val STAT_ICON_SIZE: Float = 14f
    private const val STAT_ICON_GAP: Float = 3f
    private const val ARMOUR_SLOT_SIZE: Float = 30f
    private const val ARMOUR_SLOT_GAP: Float = 20f
    private const val ARMOUR_DOT_OFFSET: Float = 5f
    private const val ARMOUR_DOT_SPAN: Float = 5f

    private const val SAMPLE_HEALTH: String = "20"
    private const val SAMPLE_ARMOUR: String = "10"
    private const val SAMPLE_HEALTH_RATIO: Float = 1f

    /** Editor sample card width, scaled with the HUD font so the preview stays inside OneConfig's browser box. */
    private fun sampleWidth(): Float = SAMPLE_BASE_WIDTH + (textScale - 1f) * SAMPLE_BASE_WIDTH * 0.25f

    private const val SAMPLE_BASE_WIDTH: Float = 150f

}
