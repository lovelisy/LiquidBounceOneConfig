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

package net.ccbluex.liquidbounce.event

import com.google.gson.Gson
import net.ccbluex.liquidbounce.config.gson.interopGson

/**
 * Marker interface for events that carry a serializer for external consumers (add-ons / config).
 *
 * The WebSocket transport itself has been removed together with the WebUI; this marker is kept
 * as a lightweight, dependency-free contract for the event classes that still implement it.
 */
interface WebSocketEvent {
    val serializer: Gson get() = interopGson

    val serializeAsync: Boolean get() = true
}
