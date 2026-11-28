package net.ccbluex.liquidbounce.integration.oneconfig.hud

import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.Weight

/**
 * Snapshot of the OneConfig style states a HUD paints with.
 *
 * The values are read during composition, where Compose observes the underlying MutableStates. Passing the snapshot
 * into the draw pass makes every painted pixel depend on those states, so editing a colour or the background switch in
 * the style panel repaints the element immediately instead of waiting for an unrelated animation frame.
 */
data class LbHudStyle(
    val textColor: Int,
    val showBackground: Boolean,
    val bgColor: Int,
    val bgRadius: Float,
    val textScale: Float,
    val font: Font,
    val textBold: Boolean,
    val textItalic: Boolean,
    val textWeight: Weight,
)
