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

import net.ccbluex.liquidbounce.utils.client.logger
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Text
import org.polyfrost.oneconfig.api.hud.v1.Hud
import java.io.File
import java.net.URI
import java.util.concurrent.Executors

/** Transparent surface, used by the elements the native client draws without a plate. */
/**
 * Native custom image element.
 *
 * The picture is read from a web address or a local file and decoded into a bitmap OneConfig can draw. The download and
 * the decode both run off the render thread, so a slow host or a large file never stalls a frame: the element keeps
 * drawing the previous picture, or the LiquidBounce logo while the first load is still running.
 *
 * One element is registered, because OneConfig owns the placement of a HUD unit; the native client allows any number of
 * copies, which would each need their own saved path.
 */
object ImageHud : LbHudSurface("image", "Image", Hud.Category.INFO, 0) {

    @Dropdown(title = "Source", options = ["URL", "File"])
    @JvmField
    var source: String = "URL"

    @Text(title = "URL", placeholder = "https://example.com/image.png")
    @JvmField
    var url: String = DEFAULT_URL

    @Text(title = "File Path", placeholder = "/storage/emulated/0/Pictures/image.png")
    @JvmField
    var filePath: String = ""

    @Slider(title = "Scale", min = 0.1f, max = 10f, step = 0.1f)
    @JvmField
    var imageScale: Float = 1f

    @Slider(title = "Opacity", min = 0f, max = 100f, step = 1f)
    @JvmField
    var opacity: Float = 100f

    /** Bitmap currently on screen, or `null` while the first load runs. */
    @Volatile
    private var bitmap: PolyBitmap? = null

    /** Source key of [bitmap], so the cache can tell when the path changed. */
    @Volatile
    private var loadedKey: String = ""

    /** Source key of the load in flight, so the same source is never downloaded twice at once. */
    @Volatile
    private var loadingKey: String = ""

    private val loader = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "LiquidBounce-HudImage").apply { isDaemon = true }
    }

    /** Resolved source of the current settings, as `isFile|spec`. */
    private fun sourceKey(): String {
        val isFile = source == "File"
        val spec = if (isFile) filePath.trim() else url.trim()
        if (spec.isEmpty() || spec == DEFAULT_URL) {
            return BUNDLED_KEY
        }
        return "$isFile|$spec"
    }

    /** Starts a background load when the configured source changed since the last attempt. */
    private fun refresh() {
        val key = sourceKey()
        if (key == loadedKey || key == loadingKey) {
            return
        }

        loadingKey = key
        loader.execute {
            val bytes = runCatching {
                if (key == BUNDLED_KEY) {
                    null
                } else if (key.startsWith("true|")) {
                    File(key.removePrefix("true|")).readBytes()
                } else {
                    URI.create(key.removePrefix("false|")).toURL().openStream().use { it.readBytes() }
                }
            }.onFailure {
                logger.warn("Unable to read the HUD image '$key'", it)
            }.getOrNull()

            val decoded = bytes?.let { data ->
                runCatching { PolyBitmap.decode(data) }.getOrNull()
            }

            bitmap = decoded
            loadedKey = key
            loadingKey = ""
        }
    }

    private val naturalWidth: Float
        get() = bitmap?.width?.toFloat()?.takeIf { it > 0f } ?: LOGO_WIDTH

    private val naturalHeight: Float
        get() = bitmap?.height?.toFloat()?.takeIf { it > 0f } ?: LOGO_HEIGHT

    private val shownScale: Float
        get() = imageScale.coerceIn(0.1f, 10f)

    override fun measure(): HudSize {
        refresh()
        return HudSize(
            (naturalWidth * shownScale).coerceAtLeast(MIN_EXTENT),
            (naturalHeight * shownScale).coerceAtLeast(MIN_EXTENT),
        )
    }

    override fun paint(canvas: LbHudCanvas) {
        refresh()

        val alpha = (opacity.coerceIn(0f, 100f) / 100f)
        if (alpha <= 0f) {
            return
        }

        val width = naturalWidth * shownScale
        val height = naturalHeight * shownScale
        val tint = LbHudPalette.withAlphaFactor(WHITE, alpha)

        canvas.fill(0f, 0f, width, height, canvas.surfaceArgb(), canvas.surfaceRadius())

        val picture = bitmap
        if (picture != null) {
            canvas.roundedTexture(
                picture,
                0f,
                0f,
                width,
                height,
                0f,
                0f,
                0f,
                picture.width.toFloat(),
                picture.height.toFloat(),
                tint,
            )
            return
        }

        canvas.stretchedIcon(LOGO_BADGE, 0f, 0f, width, height, accentArgb(alpha))
        canvas.stretchedIcon(LOGO_FOREGROUND, 0f, 0f, width, height, tint)
    }

    private const val DEFAULT_URL: String = "https://liquidbounce.net/images/logos/logo.svg"
    private const val BUNDLED_KEY: String = "bundled"

    private const val LOGO_BADGE: String = "textures/ui/hud/watermark_badge.png"
    private const val LOGO_FOREGROUND: String = "textures/ui/hud/watermark_fg.png"

    private const val LOGO_WIDTH: Float = 165f
    private const val LOGO_HEIGHT: Float = 61.8917f

    private const val MIN_EXTENT: Float = 16f
    private const val WHITE: Int = -1
}
