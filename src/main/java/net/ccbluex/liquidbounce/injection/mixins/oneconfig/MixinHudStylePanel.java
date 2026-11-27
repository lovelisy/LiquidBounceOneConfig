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
import net.ccbluex.liquidbounce.integration.oneconfig.hud.LbHudPanels;
import org.polyfrost.oneconfig.api.hud.v1.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Publishes the HUD element whose style panel is being built.
 *
 * OneConfig's style panel is hard coded Compose UI which the mod cannot reach, but it builds every card through one
 * composable, so the element has to be known while that card is built.
 */
@Pseudo
@Mixin(targets = "org.polyfrost.oneconfig.internal.ui.hud.screens.sections.StyleKt", remap = false)
public final class MixinHudStylePanel {

    @Inject(
        method = "Style(Lorg/polyfrost/oneconfig/api/hud/v1/Hud;Ljava/lang/String;"
            + "Landroidx/compose/runtime/Composer;I)V",
        at = @At("HEAD"),
        require = 0,
        remap = false
    )
    private static void liquidbounce$enterStylePanel(Hud hud, String query, Composer composer, int changed,
                                                     CallbackInfo ci) {
        LbHudPanels.enter(hud);
    }

    @Inject(
        method = "Style(Lorg/polyfrost/oneconfig/api/hud/v1/Hud;Ljava/lang/String;"
            + "Landroidx/compose/runtime/Composer;I)V",
        at = @At("RETURN"),
        require = 0,
        remap = false
    )
    private static void liquidbounce$leaveStylePanel(Hud hud, String query, Composer composer, int changed,
                                                     CallbackInfo ci) {
        LbHudPanels.exit();
    }

}
