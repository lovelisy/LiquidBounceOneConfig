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
package net.ccbluex.liquidbounce.injection.mixins.oneconfig;

import androidx.compose.runtime.Composer;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function3;
import net.ccbluex.liquidbounce.integration.oneconfig.hud.LbHudPanels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the style panel cards a LiquidBounce element cannot honour.
 *
 * Every card of OneConfig's HUD style panel is built through this one composable, and each one is skipped before any
 * composition starts, so an element that paints its own card no longer shows a background switch that would change
 * nothing, and the watermark no longer shows the typography it does not draw.
 *
 * The selector is spelled out because the class also carries the synthetic default argument bridge.
 */
@Pseudo
@Mixin(targets = "org.polyfrost.oneconfig.internal.ui.hud.screens.sections.CardsKt", remap = false)
public final class MixinHudCardSection {

    @Inject(
        method = "CardSection(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/Boolean;"
            + "Lkotlin/jvm/functions/Function1;ZLkotlin/jvm/functions/Function0;ZLjava/lang/Boolean;"
            + "Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;II)V",
        at = @At("HEAD"),
        require = 0,
        cancellable = true,
        remap = false
    )
    private static void liquidbounce$dropUnusedCard(String title, String query, String[] labels, Boolean checked,
                                                    Function1<? super Boolean, Unit> onCheckedChange, boolean mixed,
                                                    Function0<Unit> onReset, boolean canReset,
                                                    Boolean collapsedByDefault, Function3<?, ?, ?, ?> content,
                                                    Composer composer, int changed, int changedDefaults,
                                                    CallbackInfo ci) {
        if (LbHudPanels.hides(title)) {
            ci.cancel();
        }
    }

}
