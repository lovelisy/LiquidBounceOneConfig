package net.ccbluex.liquidbounce.integration.oneconfig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer

/**
 * Renders keybind settings exactly like OneConfig's own keybind visualizer, but under a distinct class so the
 * config-driven keybind catalog does not collect these properties into a single "LiquidBounce" group. Our own
 * provider groups them by module instead.
 */
internal class LbKeybindVisualizer : Visualizer {
    private val delegate = Visualizer.KeybindVisualizer()

    @Composable
    override fun visualize(prop: Property<*>, composer: Composer, i: Int) {
        delegate.visualize(prop, composer, i)
    }
}
