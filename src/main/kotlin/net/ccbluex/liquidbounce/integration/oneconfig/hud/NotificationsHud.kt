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

import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.oneconfig.LbText
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.Hud
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

/**
 * Native notification feed.
 *
 * Notifications slide in from the right, stack upwards and fade out after three seconds. Toggle notifications replace
 * each other instead of piling up, exactly like the native client does.
 */
object NotificationsHud : LbHudSurface("notifications", "Notifications", Hud.Category.INFO, LbHudPalette.surface(0.68f)) {

    @Switch(title = "Show Info")
    @JvmField
    var showInfo: Boolean = true

    @Switch(title = "Show Success")
    @JvmField
    var showSuccess: Boolean = true

    @Switch(title = "Show Error")
    @JvmField
    var showError: Boolean = true

    @Switch(title = "Show Enabled")
    @JvmField
    var showEnabled: Boolean = true

    @Switch(title = "Show Disabled")
    @JvmField
    var showDisabled: Boolean = true

    private class LiveNotification(
        var title: String,
        val message: String,
        var severity: NotificationEvent.Severity,
        var createdMs: Long,
        var yOffset: Float = 0f,
        var slideX: Float = 30f,
        var knobRatio: Float = if (severity == NotificationEvent.Severity.ENABLED) 0.62f else 0.38f,
    )

    private val activeList: CopyOnWriteArrayList<LiveNotification> = CopyOnWriteArrayList()

    @Suppress("unused")
    private val notificationHandler = handler<NotificationEvent> { event ->
        if (!isAllowed(event.severity)) {
            return@handler
        }

        val now = System.currentTimeMillis()
        if (event.severity == NotificationEvent.Severity.ENABLED || event.severity == NotificationEvent.Severity.DISABLED) {
            val existing = activeList.find {
                (it.severity == NotificationEvent.Severity.ENABLED || it.severity == NotificationEvent.Severity.DISABLED) &&
                    it.message == event.message
            }
            if (existing != null) {
                existing.title = event.title
                existing.severity = event.severity
                existing.createdMs = now
                activeList.remove(existing)
                activeList.add(0, existing)
                return@handler
            }
        }

        activeList.add(0, LiveNotification(event.title, event.message, event.severity, now))
        while (activeList.size > MAX_NOTIFICATIONS) {
            activeList.removeAt(activeList.lastIndex)
        }
    }

    private fun isAllowed(severity: NotificationEvent.Severity): Boolean = when (severity) {
        NotificationEvent.Severity.INFO -> showInfo
        NotificationEvent.Severity.SUCCESS -> showSuccess
        NotificationEvent.Severity.ERROR -> showError
        NotificationEvent.Severity.ENABLED -> showEnabled
        NotificationEvent.Severity.DISABLED -> showDisabled
    }

    override fun measure(): HudSize {
        val cardHeight = cardHeight()
        val visible = activeList.size
        if (visible == 0) {
            return HudSize(cardWidth(), cardHeight)
        }
        return HudSize(cardWidth(), visible * cardHeight + max(0, visible - 1) * CARD_GAP)
    }

    override fun paint(canvas: LbHudCanvas) {
        val now = System.currentTimeMillis()
        activeList.removeIf { now - it.createdMs > LIFETIME_MS }

        if (activeList.isEmpty()) {
            if (canvas.inEditor) {
                drawCard(
                    canvas,
                    0f,
                    0f,
                    LbText.hudString("liquidbounce.hud.notifications.name", "Notifications"),
                    LbText.hudString("liquidbounce.hud.notifications.sample.message", "Client notifications appear here"),
                    NotificationEvent.Severity.INFO,
                    0.5f,
                    1f,
                )
            }
            return
        }

        val cardHeight = cardHeight()
        activeList.forEachIndexed { index, notification ->
            val ageMs = now - notification.createdMs
            val targetY = index * (cardHeight + CARD_GAP)
            notification.yOffset = LbHudPalette.expLerp(notification.yOffset, targetY, 15f, canvas.deltaSeconds)

            val targetSlide = if (ageMs > FADE_START_MS) 35f else 0f
            notification.slideX = LbHudPalette.expLerp(notification.slideX, targetSlide, 16f, canvas.deltaSeconds)

            val targetKnob = if (notification.severity == NotificationEvent.Severity.ENABLED) 0.62f else 0.38f
            notification.knobRatio = LbHudPalette.expLerp(notification.knobRatio, targetKnob, 16f, canvas.deltaSeconds)

            val fadeAlpha = if (ageMs > FADE_START_MS) {
                (1f - (ageMs - FADE_START_MS) / FADE_DURATION_MS.toFloat()).coerceIn(0f, 1f)
            } else {
                1f
            }

            drawCard(
                canvas,
                notification.slideX,
                notification.yOffset,
                notification.title,
                notification.message,
                notification.severity,
                notification.knobRatio,
                fadeAlpha,
            )
        }
    }

    private fun drawCard(
        canvas: LbHudCanvas,
        x: Float,
        y: Float,
        title: String,
        message: String,
        severity: NotificationEvent.Severity,
        knobRatio: Float,
        alpha: Float,
    ) {
        val cardHeight = cardHeight()
        val titleLine = titleLineHeight()
        val messageLine = messageLineHeight()

        canvas.alpha(alpha) {
            canvas.fill(x, y, CARD_WIDTH, cardHeight, surfaceArgb(), 5f)

            val iconBoxX = x + ICON_MARGIN
            val iconBoxY = y + (cardHeight - ICON_BOX_SIZE) * 0.5f
            val badgeColour = when (severity) {
                NotificationEvent.Severity.SUCCESS,
                NotificationEvent.Severity.ENABLED -> LbHudPalette.SUCCESS_COLOR
                NotificationEvent.Severity.ERROR,
                NotificationEvent.Severity.DISABLED -> LbHudPalette.ERROR_COLOR
                NotificationEvent.Severity.INFO -> accentArgb()
            }
            canvas.fill(iconBoxX, iconBoxY, ICON_BOX_SIZE, ICON_BOX_SIZE, badgeColour, 4f)

            val iconId = when (severity) {
                NotificationEvent.Severity.SUCCESS -> NOTIFY_SUCCESS
                NotificationEvent.Severity.ERROR -> NOTIFY_ERROR
                NotificationEvent.Severity.INFO -> NOTIFY_INFO
                NotificationEvent.Severity.ENABLED,
                NotificationEvent.Severity.DISABLED -> NOTIFY_TOGGLE
            }
            canvas.icon(iconId, iconBoxX + 9f, iconBoxY + 9f, 22f, 22f, textColor)

            if (severity == NotificationEvent.Severity.ENABLED || severity == NotificationEvent.Severity.DISABLED) {
                val knobX = iconBoxX + ICON_BOX_SIZE * knobRatio - 5f
                val knobY = iconBoxY + ICON_BOX_SIZE * 0.5f - 5f
                canvas.fill(knobX, knobY, 10f, 10f, textColor, 5f)
            }

            val textX = iconBoxX + ICON_BOX_SIZE + TEXT_GAP
            val maxTextWidth = CARD_WIDTH - (textX - x) - ICON_MARGIN
            val clippedTitle = ellipsize(title, maxTextWidth, TITLE_SIZE, LbHudFonts.Weight.SEMIBOLD)
            val clippedMessage = ellipsize(message, maxTextWidth, MESSAGE_SIZE, LbHudFonts.Weight.REGULAR)
            val textTop = y + (cardHeight - (titleLine + TEXT_GAP + messageLine)) * 0.5f
            drawText(canvas, clippedTitle, textX, textTop, TITLE_SIZE, textColor, LbHudFonts.Weight.SEMIBOLD)
            drawText(
                canvas,
                clippedMessage,
                textX,
                textTop + titleLine + TEXT_GAP,
                MESSAGE_SIZE,
                dimTextArgb(),
                LbHudFonts.Weight.REGULAR,
            )
        }
    }

    private fun titleLineHeight(): Float = lineHeight(TITLE_SIZE, LbHudFonts.Weight.SEMIBOLD)

    private fun messageLineHeight(): Float = lineHeight(MESSAGE_SIZE, LbHudFonts.Weight.REGULAR)

    /** The card follows the HUD font, so the text never spills out of the surface when the size is raised. */
    private fun cardHeight(): Float {
        val iconExtent = ICON_BOX_SIZE + ICON_MARGIN * 2f
        val textExtent = titleLineHeight() + TEXT_GAP + messageLineHeight() + ICON_MARGIN * 2f
        return max(iconExtent, textExtent)
    }

    private fun cardWidth(): Float = CARD_WIDTH

    private val NOTIFY_SUCCESS = "textures/ui/hud/notification/icon-success.png"
    private val NOTIFY_ERROR = "textures/ui/hud/notification/icon-error.png"
    private val NOTIFY_INFO = "textures/ui/hud/notification/icon-info.png"
    private val NOTIFY_TOGGLE = "textures/ui/hud/notification/icon-toggle.png"

    private const val CARD_WIDTH: Float = 300f
    private const val CARD_GAP: Float = 10f
    private const val ICON_BOX_SIZE: Float = 40f
    private const val ICON_MARGIN: Float = 10f
    private const val TEXT_GAP: Float = 4f
    private const val TITLE_SIZE: Float = 14f
    private const val MESSAGE_SIZE: Float = 12f
    private const val MAX_NOTIFICATIONS: Int = 12
    private const val LIFETIME_MS: Long = 3200L
    private const val FADE_START_MS: Long = 3000L
    private const val FADE_DURATION_MS: Long = 200L

}
