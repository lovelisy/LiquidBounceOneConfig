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
package net.ccbluex.liquidbounce.integration.oneconfig

import androidx.compose.runtime.mutableStateOf
import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.config.types.BindValue
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ClientLanguageChangedEvent
import net.ccbluex.liquidbounce.features.global.GlobalManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.input.InputBind
import org.polyfrost.oneconfig.api.config.v1.MetaKeys
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Turns the LiquidBounce settings tree into the flat tree OneConfig renders.
 *
 * OneConfig draws only properties that are direct children of a tree, so every LiquidBounce group and mode is
 * flattened into single properties. The metadata carries the category tab and the section heading, while
 * display conditions keep the nesting semantics: settings of a disabled group or of an inactive mode are hidden.
 */
internal object LbConfigTree {

    private const val SEPARATOR = " / "

    private val revisionState = mutableStateOf(0)

    val revision: Int
        get() = revisionState.value

    @Volatile
    private var cached: Tree? = null

    @Volatile
    private var cachedRevision: Int = -1

    @Volatile
    private var languageHookInstalled = false

    private fun ensureLanguageHook() {
        if (languageHookInstalled) {
            return
        }
        languageHookInstalled = true

        val listener: EventListener = object : EventListener {}
        listener.on(ClientLanguageChangedEvent::class.java) {
            revisionState.value++
        }
    }

    fun invalidate() {
        revisionState.value++
    }

    fun build(): Tree {
        ensureLanguageHook()

        val current = revision
        cached?.let { if (cachedRevision == current) return it }

        val tree = createTree()
        cached = tree
        cachedRevision = current
        return tree
    }

    private fun createTree(): Tree {
        usedIds.clear()
        val root = Tree("liquidbounce", "LiquidBounce", null, null)

        val known = ModuleCategories.entries.toList()

        for (moduleCategory in known) {
            ModuleManager
                .filter { it.category === moduleCategory }
                .sortedBy { it.name }
                .forEach { module(it, root) }
        }

        ModuleManager
            .filter { module -> known.none { it === module.category } }
            .sortedBy { it.name }
            .forEach { module(it, root) }

        val global = LbText.categoryName("Global")
        appendGroup(root, GlobalManager, global, emptyList(), "", emptySet())

        return root
    }

    private val usedIds = HashSet<String>()

    private fun id(prefix: String, hint: String): String {
        val base = buildString(prefix.length + hint.length + 1) {
            append(prefix)
            append('_')
            for (character in hint) {
                append(
                    if (character.isLetterOrDigit() || character == '_' || character == '-' ||
                        character == '.' || character == '/'
                    ) {
                        character
                    } else {
                        '_'
                    },
                )
            }
        }

        var candidate = base
        var index = 2
        while (!usedIds.add(candidate)) {
            candidate = "${base}_${index++}"
        }
        return candidate
    }

    private fun submenu(key: String, title: String, category: String): Tree {
        val tree = Tree(id("module", key), title, null, null)
        tree.addMetadata(MetaKeys.CATEGORY.name, category)
        tree.addMetadata("submenu", true)
        return tree
    }

    private fun module(module: ClientModule, root: Tree) {
        val category = LbText.categoryName(module.category.tag)
        val tree = submenu(module.name, LbText.moduleName(module.name), category)
        tree.description = LbText.moduleDescription(module.descriptionKey)

        val toggle = booleanProperty(module.enabledValue)
        toggle.addMetadata(MetaKeys.CATEGORY.name, category)
        tree.put(toggle)
        tree.addMetadata("enableToggle", toggle)

        appendGroup(tree, module, category, emptyList(), "", setOf(module.enabledValue))

        root.put(tree)
    }

    private fun appendGroup(
        tree: Tree,
        group: ValueGroup,
        category: String,
        conditions: List<Supplier<Property.Display>>,
        path: String,
        skip: Set<Value<*>>,
    ) {
        for (value in group.containedValues) {
            if (value in skip) {
                continue
            }

            when {
                value is ModeValueGroup<*> -> appendMode(tree, value, category, conditions, path)
                value is ToggleableValueGroup -> appendToggleable(tree, value, category, conditions, path)
                value is ValueGroup -> appendGroup(
                    tree,
                    value,
                    category,
                    conditions,
                    join(path, LbText.valueName(value)),
                    emptySet(),
                )
                else -> {
                    val property = leafProperty(value) ?: continue
                    place(tree, property, category, path, conditions, value)
                }
            }
        }
    }

    private fun appendToggleable(
        tree: Tree,
        group: ToggleableValueGroup,
        category: String,
        conditions: List<Supplier<Property.Display>>,
        path: String,
    ) {
        val heading = join(path, LbText.valueName(group))
        val toggle = booleanProperty(group.enabledValue, LbText.valueName(group))
        place(tree, toggle, category, heading, conditions, group)

        val enabled = Supplier {
            if (group.enabledValue.get()) Property.Display.SHOWN else Property.Display.HIDDEN
        }

        val added = ArrayList<Property<*>>()
        appendInto(tree, group, category, conditions + enabled, heading, setOf(group.enabledValue), added)
        added.forEach { toggle.addDependent(it) }
    }

    private fun appendMode(
        tree: Tree,
        group: ModeValueGroup<*>,
        category: String,
        conditions: List<Supplier<Property.Display>>,
        path: String,
        collect: MutableList<Property<*>>? = null,
    ) {
        val modes = group.modes.toList()
        val heading = join(path, LbText.valueName(group))

        val property = Properties.functional(
            Supplier { group.activeMode.tag },
            Consumer { tag: String ->
                runCatching { group.setByString(tag) }
                Unit
            },
            id("mode", group.name),
            LbText.valueName(group),
            LbText.description(group) ?: "",
            String::class.java,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.DropdownVisualizer::class.java)
        property.addMetadata(MetaKeys.OPTIONS.name, modes.map { it.tag })
        property.addMetadata(MetaKeys.LABELS.name, modes.map { LbText.tagName(it.tag) })
        property.addMetadata("optionValues", modes.map { it.tag })
        place(tree, property, category, heading, conditions, group)
        collect?.add(property)

        for (mode in modes) {
            val selected = Supplier {
                if (group.activeMode === mode) Property.Display.SHOWN else Property.Display.HIDDEN
            }

            val added = ArrayList<Property<*>>()
            appendInto(
                tree,
                mode,
                category,
                conditions + selected,
                heading,
                emptySet(),
                added,
            )
            added.forEach { property.addDependent(it) }
            collect?.addAll(added)
        }
    }

    private fun appendInto(
        tree: Tree,
        group: ValueGroup,
        category: String,
        conditions: List<Supplier<Property.Display>>,
        path: String,
        skip: Set<Value<*>>,
        collect: MutableList<Property<*>>,
    ) {
        for (value in group.containedValues) {
            if (value in skip) {
                continue
            }

            when {
                value is ModeValueGroup<*> -> appendMode(tree, value, category, conditions, path, collect)

                value is ToggleableValueGroup -> {
                    val heading = join(path, LbText.valueName(value))
                    val toggle = booleanProperty(value.enabledValue, LbText.valueName(value))
                    place(tree, toggle, category, heading, conditions, value)
                    collect += toggle

                    val enabled = Supplier {
                        if (value.enabledValue.get()) Property.Display.SHOWN else Property.Display.HIDDEN
                    }

                    val added = ArrayList<Property<*>>()
                    appendInto(
                        tree,
                        value,
                        category,
                        conditions + enabled,
                        heading,
                        setOf(value.enabledValue),
                        added,
                    )
                    added.forEach { toggle.addDependent(it) }
                    collect += added
                }

                value is ValueGroup -> appendInto(
                    tree,
                    value,
                    category,
                    conditions,
                    join(path, LbText.valueName(value)),
                    emptySet(),
                    collect,
                )
                else -> {
                    val property = leafProperty(value) ?: continue
                    place(tree, property, category, path, conditions, value)
                    collect += property
                }
            }
        }
    }

    private fun place(
        tree: Tree,
        property: Property<*>,
        category: String,
        subcategory: String,
        conditions: List<Supplier<Property.Display>>,
        value: Value<*>?,
    ) {
        property.addMetadata(MetaKeys.CATEGORY.name, category)
        if (subcategory.isNotBlank()) {
            property.addMetadata(MetaKeys.SUBCATEGORY.name, subcategory)
        }

        if (conditions.isEmpty() && value == null) {
            tree.put(property)
            return
        }

        val all = ArrayList<Supplier<Property.Display>>(conditions.size + 1)
        all.addAll(conditions)
        if (value != null) {
            all.add(Supplier {
                if (value.visibleCondition.asBoolean) Property.Display.SHOWN else Property.Display.HIDDEN
            })
        }
        property.addDisplayCondition(all)
        tree.put(property)
    }

    private fun join(path: String, name: String): String = if (path.isEmpty()) name else path + SEPARATOR + name

    private fun booleanProperty(value: Value<Boolean>, title: String = LbText.valueName(value)): Property<Boolean> {
        val property = Properties.functional(
            Supplier { value.get() },
            Consumer { flag: Boolean -> value.set(flag) },
            id("toggle", value.name),
            title,
            LbText.description(value) ?: "",
            Boolean::class.javaObjectType,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.SwitchVisualizer::class.java)
        return property
    }

    private fun leafProperty(value: Value<*>): Property<*>? {
        if (value.notAnOption) {
            return null
        }

        return when (value.valueType) {
            ValueType.BOOLEAN -> booleanProperty(value as Value<Boolean>)
            ValueType.FLOAT -> floatProperty(value)
            ValueType.INT -> intProperty(value)
            ValueType.FLOAT_RANGE, ValueType.INT_RANGE -> rangeProperty(value)
            ValueType.TEXT -> textProperty(value)
            ValueType.COLOR -> colorProperty(value)
            ValueType.KEY -> keyProperty(value)
            ValueType.BIND -> bindProperty(value)
            ValueType.CHOOSE -> choiceProperty(value)
            ValueType.MULTI_CHOOSE -> multiChoiceProperty(value)
            else -> stringProperty(value) ?: infoProperty(value)
        }
    }

    private fun floatProperty(value: Value<*>): Property<Float> {
        @Suppress("UNCHECKED_CAST")
        val target = value as Value<Float>
        val ranged = value as? RangedValue<*>
        val min = (ranged?.range?.start as? Number)?.toFloat() ?: 0f
        val max = (ranged?.range?.endInclusive as? Number)?.toFloat() ?: 1f

        val property = Properties.functional(
            Supplier { target.get() },
            Consumer { number: Float -> target.set(number) },
            id("float", value.name),
            LbText.valueName(value),
            LbText.description(value) ?: "",
            Float::class.javaObjectType,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.SliderVisualizer::class.java)
        property.addMetadata(MetaKeys.MIN.name, min)
        property.addMetadata(MetaKeys.MAX.name, max)
        if (max - min <= 10f) {
            property.addMetadata(MetaKeys.STEP.name, 0.01f)
        }
        ranged?.suffix?.takeIf { it.isNotBlank() }?.let {
            property.addMetadata(MetaKeys.UNIT.name, it)
        }
        return property
    }

    private fun intProperty(value: Value<*>): Property<Int> {
        @Suppress("UNCHECKED_CAST")
        val target = value as Value<Int>
        val ranged = value as? RangedValue<*>
        val min = (ranged?.range?.start as? Number)?.toInt() ?: 0
        val max = (ranged?.range?.endInclusive as? Number)?.toInt() ?: 100

        val property = Properties.functional(
            Supplier { target.get() },
            Consumer { number: Int -> target.set(number) },
            id("int", value.name),
            LbText.valueName(value),
            LbText.description(value) ?: "",
            Int::class.javaObjectType,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.SliderVisualizer::class.java)
        property.addMetadata(MetaKeys.MIN.name, min)
        property.addMetadata(MetaKeys.MAX.name, max)
        property.addMetadata(MetaKeys.STEP.name, 1)
        ranged?.suffix?.takeIf { it.isNotBlank() }?.let {
            property.addMetadata(MetaKeys.UNIT.name, it)
        }
        return property
    }

    @Suppress("UNCHECKED_CAST")
    private fun rangeProperty(value: Value<*>): Property<FloatArray> {
        val ranged = value as RangedValue<*>
        val min = (ranged.range.start as? Number)?.toFloat() ?: 0f
        val max = (ranged.range.endInclusive as? Number)?.toFloat() ?: 1f
        val integer = ranged.range.start is Int
        val target = value as Value<Any>

        val property = Properties.functional(
            Supplier {
                val current = target.get() as? ClosedRange<*>
                floatArrayOf(
                    (current?.start as? Number)?.toFloat() ?: min,
                    (current?.endInclusive as? Number)?.toFloat() ?: max,
                )
            },
            Consumer { numbers: FloatArray ->
                if (numbers.size >= 2) {
                    val written: Any = if (integer) {
                        numbers[0].toInt()..numbers[1].toInt()
                    } else {
                        numbers[0]..numbers[1]
                    }
                    target.set(written)
                }
            },
            id("range", value.name),
            LbText.valueName(value),
            LbText.description(value) ?: "",
            FloatArray::class.java,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.RangeSliderVisualizer::class.java)
        property.addMetadata(MetaKeys.MIN.name, min)
        property.addMetadata(MetaKeys.MAX.name, max)
        property.addMetadata(MetaKeys.STEP.name, if (integer) 1f else 0.01f)
        ranged.suffix.takeIf { it.isNotBlank() }?.let {
            property.addMetadata(MetaKeys.UNIT.name, it)
        }
        return property
    }

    private fun textProperty(value: Value<*>): Property<String> {
        @Suppress("UNCHECKED_CAST")
        val target = value as Value<String>
        val property = Properties.functional(
            Supplier { target.get() },
            Consumer { text: String -> target.set(text) },
            id("text", value.name),
            LbText.valueName(value),
            LbText.description(value) ?: "",
            String::class.java,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.TextVisualizer::class.java)
        return property
    }

    private fun colorProperty(value: Value<*>): Property<Int> {
        @Suppress("UNCHECKED_CAST")
        val target = value as Value<Color4b>
        val property = Properties.functional(
            Supplier { target.get().argb },
            Consumer { argb: Int -> target.set(Color4b(argb)) },
            id("color", value.name),
            LbText.valueName(value),
            LbText.description(value) ?: "",
            Int::class.javaObjectType,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.ColorVisualizer::class.java)
        return property
    }

    private fun choiceProperty(value: Value<*>): Property<String> {
        val choice = value as ChoiceListValue<*>
        val choices = choice.choices.toList()

        val property = Properties.functional(
            Supplier { choice.get().tag },
            Consumer { tag: String ->
                runCatching { choice.setByString(tag) }
                Unit
            },
            id("choice", value.name),
            LbText.valueName(value),
            LbText.description(value) ?: "",
            String::class.java,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.DropdownVisualizer::class.java)
        property.addMetadata(MetaKeys.OPTIONS.name, choices.map { it.tag })
        property.addMetadata(MetaKeys.LABELS.name, choices.map { LbText.tagName(it.tag) })
        property.addMetadata("optionValues", choices.map { it.tag })
        return property
    }

    @Suppress("UNCHECKED_CAST")
    private fun multiChoiceProperty(value: Value<*>): Property<BooleanArray> {
        val multi = value as MultiChoiceListValue<Tagged>
        val choices = multi.choices.toList()

        val property = Properties.functional(
            Supplier {
                val active = multi.get()
                BooleanArray(choices.size) { choices[it] in active }
            },
            Consumer { flags: BooleanArray ->
                for (index in choices.indices) {
                    val choice = choices[index]
                    val desired = flags.getOrElse(index) { false }
                    if ((choice in multi.get()) != desired) {
                        multi.toggle(choice)
                    }
                }
            },
            id("multi", value.name),
            LbText.valueName(value),
            LbText.description(value) ?: "",
            BooleanArray::class.java,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.MultiSelectDropdownVisualizer::class.java)
        property.addMetadata(MetaKeys.OPTIONS.name, choices.map { it.tag }.toTypedArray())
        property.addMetadata(MetaKeys.LABELS.name, choices.map { LbText.tagName(it.tag) })
        property.addMetadata("checkable", true)
        return property
    }

    private fun keyProperty(value: Value<*>): Property<OneConfigKeybind> {
        @Suppress("UNCHECKED_CAST")
        val target = value as Value<InputConstants.Key>

        val property = Properties.functional(
            Supplier {
                val key = target.get()
                if (key == InputConstants.UNKNOWN) {
                    OneConfigKeybind(null, null, 0, 0L) { true }
                } else {
                    keybindOf(key)
                }
            },
            Consumer { keybind: OneConfigKeybind ->
                val mouse = keybind.mouseBtns?.firstOrNull()
                val keyboard = keybind.keyCodes?.firstOrNull()
                target.set(
                    when {
                        mouse != null -> InputConstants.Type.MOUSE.getOrCreate(mouse)
                        keyboard != null -> InputConstants.Type.KEYBOARD.getOrCreate(keyboard)
                        else -> InputConstants.UNKNOWN
                    },
                )
            },
            id("key", value.name),
            LbText.valueName(value),
            LbText.description(value) ?: "",
            OneConfigKeybind::class.java,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.KeybindVisualizer::class.java)
        property.addMetadata("singleKey", true)
        return property
    }

    private fun bindProperty(value: Value<*>): Property<OneConfigKeybind> {
        val target = value as BindValue

        val property = Properties.functional(
            Supplier { keybindOf(target.get().boundKey) },
            Consumer { keybind: OneConfigKeybind ->
                val mouse = keybind.mouseBtns?.firstOrNull()
                val keyboard = keybind.keyCodes?.firstOrNull()
                val key = when {
                    mouse != null -> InputConstants.Type.MOUSE.getOrCreate(mouse)
                    keyboard != null -> InputConstants.Type.KEYBOARD.getOrCreate(keyboard)
                    else -> InputConstants.UNKNOWN
                }
                val modifiers = InputBind.Modifier.entries
                    .filterTo(HashSet<InputBind.Modifier>()) { it.isActive(keybind.mods.toInt()) }
                target.set(InputBind(key, target.get().action, modifiers))
            },
            id("bind", value.name),
            LbText.valueName(value),
            LbText.description(value) ?: "",
            OneConfigKeybind::class.java,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.KeybindVisualizer::class.java)
        return property
    }

    private fun stringProperty(value: Value<*>): Property<String>? {
        if (value.valueType.deserializer == null) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        val target = value as Value<Any>

        val property = Properties.functional(
            Supplier { describeValue(target.get()) },
            Consumer { text: String ->
                runCatching { target.setByString(text) }
                Unit
            },
            id("string", value.name),
            LbText.valueName(value),
            LbText.description(value) ?: "",
            String::class.java,
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.TextVisualizer::class.java)
        return property
    }

    private fun infoProperty(value: Value<*>): Property<Void> {
        val property = Properties.dummy(
            id("info", value.name),
            LbText.valueName(value),
            describeValue(value.get()),
        )
        property.addMetadata(MetaKeys.VISUALIZER.name, Visualizer.InfoVisualizer::class.java)
        return property
    }

    private fun describeValue(value: Any?): String = when (value) {
        null -> "—"
        is IntRange -> "${value.first}..${value.last}"
        is ClosedRange<*> -> "${value.start}..${value.endInclusive}"
        is Array<*> -> value.joinToString(", ")
        is Iterable<*> -> value.joinToString(", ")
        is InputConstants.Key -> value.name
        is Tagged -> value.tag
        else -> value.toString()
    }

    private fun keybindOf(key: InputConstants.Key): OneConfigKeybind {
        val keyCodes = if (key.type == InputConstants.Type.KEYBOARD) intArrayOf(key.value) else null
        val mouseBtns = if (key.type == InputConstants.Type.MOUSE) intArrayOf(key.value) else null
        return OneConfigKeybind(keyCodes, mouseBtns, 0, 0L) { true }
    }

}
