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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.integration.oneconfig.LbText
import net.ccbluex.liquidbounce.utils.client.logger
import org.polyfrost.oneconfig.api.config.v1.MetaKeys
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import java.lang.reflect.Method
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Keeps the settings of LiquidBounce HUD elements inside LiquidBounce's own configuration folder.
 *
 * OneConfig would normally persist a HUD's option tree into `OneConfig/Config/<profile>`. LiquidBounce keeps its
 * configuration together in `LiquidBounce/configs`, so every HUD element stores its tree in
 * `LiquidBounce/configs/hud/<id>.json` by installing OneConfig's `custom_save` hook on the tree.
 *
 * OneConfig still renders the settings panel and the HUD editor; it only delegates reading and writing of the option
 * values to this store. Placement, scale and visibility stay in OneConfig's `hud-registry.json`, because that file is
 * what drives the HUD editor.
 */
internal object LbHudStore {

    /** Metadata OneConfig reads to delegate saving and loading of a tree to the given [Runnable]. */
    private const val CUSTOM_SAVE = "custom_save"

    private const val SAVE_DELAY_MS = 400L

    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "LiquidBounce HUD Config").apply { isDaemon = true }
        }

    private val pending = ConcurrentHashMap<String, Pair<Tree, File>>()

    private val scheduled = ConcurrentHashMap<String, ScheduledFuture<*>>()

    val folder: File by lazy {
        File(ConfigSystem.userConfigsFolder, "hud").apply { mkdirs() }
    }

    /**
     * Makes [owner] persist its tree through LiquidBounce instead of OneConfig and restores the stored values.
     *
     * Called from `Hud.addToSerialized`, which OneConfig invokes right after the option tree was collected from the
     * annotated fields and right before the tree is registered with the config backend.
     */
    fun attach(owner: LbHud, tree: Tree) {
        val file = fileFor(owner)

        load(tree, file)

        tree.addMetadata(CUSTOM_SAVE, Runnable { save(tree, file) })

        replaceTitle(
            tree,
            LbText.hudTitle(owner.id, tree.getTitle()?.toString() ?: owner.id),
        )

        tree.onAllProps { _, property -> localize(owner, property) }
        tree.onAllProps { _, property -> bindSave(owner, property, tree, file) }
    }

    /**
     * Wires the collected properties into OneConfig's translation system. Titles become translatable components and
     * dropdown options get translatable labels, so the settings panel of the element follows the in-game language and
     * falls back to the annotated English text. The language files of the client are standard Minecraft language
     * resources, which the game loads for every namespace, so the keys resolve without further plumbing.
     *
     * The key is built from the property id, not from the title: a title such as `Show Tags` would turn into a key
     * holding a space, which no language file can ever match.
     *
     * OneConfig marks the title of a node as final once it has been read, so the package private title replace has to
     * be used through reflection.
     */
    private fun localize(owner: LbHud, property: Property<*>) {
        val id = property.getID()
        val title = property.getTitle()?.toString() ?: return
        val prefix = "${LbText.HUD_KEY_PREFIX}.${owner.id}.$id"

        val options = property.getMetadata(MetaKeys.OPTIONS.name) as? List<*>
        if (!options.isNullOrEmpty()) {
            val labels = options.map { option ->
                LbText.hudComponent("$prefix.${LbText.lowerFirst(option.toString())}", option.toString())
            }
            property.addMetadata(MetaKeys.LABELS.name, labels)
        }

        replaceTitle(property, LbText.hudComponent("$prefix.name", title))
    }

    private fun replaceTitle(node: Node, title: Any) {
        val method = replacement()
        if (method == null) {
            logger.warn("Unable to translate the title of '${node.getID()}': no replaceTitle method")
            return
        }

        runCatching {
            method.invoke(node, title)
        }.onFailure {
            logger.warn("Unable to translate the title of '${node.getID()}'", it)
        }
    }

    /**
     * `Node.replaceTitle` is package private: OneConfig locks the title of a node after it has been read once, and
     * only this method may overwrite it. The lookup is deferred so a changed OneConfig build cannot break class
     * initialisation, it only skips the translation.
     */
    private fun replacement(): Method? =
        replacementCache ?: run {
            runCatching {
                Node::class.java.getDeclaredMethod("replaceTitle", Any::class.java).apply { isAccessible = true }
            }.getOrNull().also { replacementCache = it }
        }

    private var replacementCache: Method? = null

    /**
     * Wires [property] to a deferred write, so a change made in the HUD settings panel reaches the disk without
     * waiting for OneConfig to flush its profiles.
     *
     * The predicate is called while the value is being written and may veto the change by returning `true`; this
     * listener always allows it, records that something changed and asks the element to repaint.
     */
    @Suppress("UNCHECKED_CAST")
    private fun bindSave(owner: LbHud, property: Property<*>, tree: Tree, file: File) {
        (property as Property<Any?>).addCallback {
            requestSave(tree, file)
            owner.invalidate()
            false
        }
    }

    private fun fileFor(owner: LbHud): File {
        val name = owner.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(folder, "$name.json")
    }

    private fun load(tree: Tree, file: File) {
        if (!file.isFile) {
            return
        }

        val root = file.runCatching {
            LbHudJson.gson.fromJson(readText(), JsonObject::class.java)
        }.onFailure {
            logger.error("Unable to read the HUD settings from ${file.name}", it)
        }.getOrNull() ?: return

        runCatching {
            apply(tree, root)
        }.onFailure {
            logger.error("Unable to apply the HUD settings from ${file.name}", it)
        }
    }

    /**
     * Copies the stored values onto [tree], property by property.
     *
     * Going through the properties keeps the tree identity, its callbacks and its metadata untouched; only the values
     * are replaced.
     */
    private fun apply(tree: Tree, json: JsonObject) {
        tree.onAllProps { id, property ->
            val element = json.get(id) ?: return@onAllProps
            if (element.isJsonNull) {
                return@onAllProps
            }

            val value = runCatching { LbHudJson.read(element, property.type) }.getOrNull()
                ?: return@onAllProps
            runCatching { setValue(property, value) }
                .onFailure { logger.warn("Unable to restore the HUD option '$id'", it) }
        }

        tree.onAll { id, node ->
            if (node is Tree) {
                val child = json.getAsJsonObject(id) ?: return@onAll
                runCatching { apply(node, child) }
                    .onFailure { logger.warn("Unable to restore the HUD group '$id'", it) }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setValue(property: Property<*>, value: Any) {
        (property as Property<Any?>).setAsSilently(value)
    }

    private fun save(tree: Tree, file: File) {
        val json = JsonObject()
        collect(tree, json)

        val text = LbHudJson.gson.toJson(json)
        file.parentFile?.mkdirs()
        file.runCatching {
            writeText(text)
        }.onFailure {
            logger.error("Unable to write the HUD settings to ${file.name}", it)
        }
    }

    private fun collect(tree: Tree, json: JsonObject) {
        tree.onAllProps { id, property ->
            if (property.type == Void::class.java) {
                return@onAllProps
            }

            val value = runCatching { property.get() }.getOrNull() ?: return@onAllProps
            runCatching { json.add(id, LbHudJson.write(value)) }
                .onFailure { logger.warn("Unable to store the HUD option '$id'", it) }
        }

        tree.onAll { id, node ->
            if (node is Tree) {
                val child = JsonObject()
                collect(node, child)
                json.add(id, child)
            }
        }
    }

    /**
     * Schedules [tree] to be written soon.
     *
     * Dragging a slider fires one change per frame, so writes are coalesced into a single trailing write instead of
     * touching the disk on every step.
     */
    private fun requestSave(tree: Tree, file: File) {
        val key = file.name
        pending[key] = tree to file
        scheduled.remove(key)?.cancel(false)
        scheduled[key] = scheduler.schedule({ flush(key) }, SAVE_DELAY_MS, TimeUnit.MILLISECONDS)
    }

    /** Writes every pending tree immediately, so nothing is lost when the client shuts down. */
    fun flushAll() {
        for (key in pending.keys.toList()) {
            scheduled.remove(key)?.cancel(false)
            flush(key)
        }
    }

    private fun flush(key: String) {
        val (tree, file) = pending.remove(key) ?: return
        save(tree, file)
    }

}
