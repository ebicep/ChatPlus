package com.ebicep.chatplus.config.forge

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.TimestampMode
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.features.FilterHighlight
import com.ebicep.chatplus.features.FilterHighlight.DEFAULT_COLOR
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.translator.LanguageManager
import com.ebicep.chatplus.translator.RegexMatch
import com.ebicep.chatplus.translator.languages
import com.mojang.blaze3d.platform.InputConstants
import me.shedaniel.clothconfig2.api.*
import me.shedaniel.clothconfig2.gui.entries.*
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder
import me.shedaniel.math.Color
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.*
import java.util.function.Consumer

object ConfigScreenImpl {

    @JvmStatic
    fun getConfigScreen(previousScreen: Screen? = null): Screen {
//        return ClothConfigDemo.getConfigBuilderWithDemo().build()
        val builder: ConfigBuilder = ConfigBuilder.create()
            .setParentScreen(previousScreen)
            .setTitle(Component.translatable("chatPlus.title"))
            .setSavingRunnable(Config::save)
            .transparentBackground()
        val entryBuilder: ConfigEntryBuilder = builder.entryBuilder()
        addGeneralOptions(builder, entryBuilder)
        addChatTabsOption(builder, entryBuilder)
        addFilterHighlightOption(builder, entryBuilder)
        addKeyBindOptions(builder, entryBuilder)
        addTranslatorRegexOptions(builder, entryBuilder)
        return builder.build()
    }

    private fun addGeneralOptions(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val general = builder.getOrCreateCategory(Component.translatable("chatPlus.title"))
        general.addEntry(entryBuilder.booleanToggle("chatPlus.chatSettings.toggle", Config.values.enabled) { Config.values.enabled = it })
        general.addEntry(
            entryBuilder.intSlider(
                "chatPlus.chatSettings.maxMessages",
                Config.values.maxMessages,
                1000,
                10_000_000
            ) { Config.values.maxMessages = it })
        general.addEntry(
            entryBuilder.intSlider(
                "chatPlus.chatSettings.maxCommandSuggestions",
                Config.values.maxCommandSuggestions,
                10,
                30
            ) { Config.values.maxCommandSuggestions = it })
        general.addEntry(entryBuilder.percentSlider("chatPlus.chatSettings.chatTextSize", Config.values.scale) { Config.values.scale = it })
        general.addEntry(
            entryBuilder.percentSlider(
                "chatPlus.chatSettings.textOpacity",
                Config.values.textOpacity
            ) { Config.values.textOpacity = it })
        general.addEntry(
            entryBuilder.percentSlider(
                "chatPlus.chatSettings.backgroundOpacity",
                Config.values.backgroundOpacity
            ) { Config.values.backgroundOpacity = it })
        general.addEntry(
            entryBuilder.percentSlider(
                "chatPlus.chatSettings.lineSpacing",
                Config.values.lineSpacing
            ) { Config.values.lineSpacing = it })
        general.addEntry(entryBuilder.startEnumSelector(
            Component.translatable("chatPlus.chatSettings.chatTimestampMode"),
            TimestampMode::class.java,
            Config.values.chatTimestampMode
        )
            .setEnumNameProvider { (it as TimestampMode).translatable }
            .setDefaultValue(Config.values.chatTimestampMode)
            .setTooltip(Component.translatable("chatPlus.chatSettings.chatTimestampMode.tooltip"))
            .setSaveConsumer { Config.values.chatTimestampMode = it }
            .build())
        general.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.chatSettings.hoverHighlight.toggle",
                Config.values.hoverHighlightEnabled
            ) { Config.values.hoverHighlightEnabled = it })
        general.addEntry(
            entryBuilder.startAlphaColorField(
                Component.translatable("chatPlus.chatSettings.hoverHighlight.color"),
                Color.ofTransparent(Config.values.hoverHighlightColor)
            )
                .setTooltip(Component.translatable("chatPlus.chatSettings.hoverHighlight.color.tooltip"))
                .setAlphaMode(true)
                .setDefaultValue2 {
                    Color.ofTransparent(Config.values.hoverHighlightColor)
                }
                .setSaveConsumer2 {
                    Config.values.hoverHighlightColor = it.color
                }
                .build()
        )
    }

    private fun addChatTabsOption(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val chatTabs = builder.getOrCreateCategory(Component.translatable("chatPlus.chatTabs.title"))
        chatTabs.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.chatTabs.toggle",
                Config.values.chatTabsEnabled
            ) { Config.values.chatTabsEnabled = it })
        chatTabs.addEntry(
            getCustomListOption(
                "chatPlus.chatTabs.title",
                Config.values.chatTabs,
                { Config.values.chatTabs = it },
                Config.values.chatTabs.size in 2..9,
                { ChatTab("", "") },
                { value ->
                    listOf(
                        entryBuilder.startStrField(Component.translatable("chatPlus.chatTabs.name"), value.name)
                            .setTooltip(Component.translatable("chatPlus.chatTabs.name.tooltip"))
                            .setDefaultValue("")
                            .setSaveConsumer { value.name = it }
                            .build(),
                        entryBuilder.startStrField(Component.translatable("chatPlus.chatTabs.pattern"), value.pattern)
                            .setTooltip(Component.translatable("chatPlus.chatTabs.pattern.tooltip"))
                            .setDefaultValue("")
                            .setSaveConsumer { value.pattern = it }
                            .build(),
                    )
                }

            )
        )
    }

    private fun addFilterHighlightOption(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val filterHighlight = builder.getOrCreateCategory(Component.translatable("chatPlus.filterHighlight.title"))
        filterHighlight.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.filterHighlight.toggle",
                Config.values.filterHighlightEnabled
            ) { Config.values.filterHighlightEnabled = it })
        filterHighlight.addEntry(
            getCustomListOption(
                "chatPlus.filterHighlight.title",
                Config.values.filterHighlights,
                { Config.values.filterHighlights = it },
                true,
                { FilterHighlight.Filter("", DEFAULT_COLOR) },
                { value ->
                    listOf(
                        entryBuilder.startStrField(Component.translatable("chatPlus.filterHighlight.pattern"), value.pattern)
                            .setTooltip(Component.translatable("chatPlus.filterHighlight.pattern.tooltip"))
                            .setDefaultValue("")
                            .setSaveConsumer { value.pattern = it }
                            .build(),
                        entryBuilder.startAlphaColorField(Component.translatable("chatPlus.filterHighlight.color"), value.color)
                            .setTooltip(Component.translatable("chatPlus.filterHighlight.color.tooltip"))
                            .setDefaultValue(DEFAULT_COLOR)
                            .setSaveConsumer { value.color = it }
                            .build(),
                    )
                }

            )
        )
    }

    private fun addKeyBindOptions(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val keyBinds = builder.getOrCreateCategory(Component.translatable("chatPlus.chatKeyBinds"))
        keyBinds.addEntry(
            entryBuilder.keyCodeOption("key.noScroll", Config.values.keyNoScroll) { Config.values.keyNoScroll = it }
        )
        keyBinds.addEntry(
            entryBuilder.keyCodeOption("key.fineScroll", Config.values.keyFineScroll) { Config.values.keyFineScroll = it }
        )
        keyBinds.addEntry(
            entryBuilder.keyCodeOption("key.largeScroll", Config.values.keyLargeScroll) { Config.values.keyLargeScroll = it }
        )
        keyBinds.addEntry(
            entryBuilder.keyCodeOption("key.moveChat", Config.values.keyMoveChat) { Config.values.keyMoveChat = it }
        )
        keyBinds.addEntry(
            entryBuilder.startModifierKeyCodeField(
                Component.translatable("key.copyMessage"),
                ModifierKeyCode.of(
                    Config.values.keyCopyMessageWithModifier.key,
                    Modifier.of(Config.values.keyCopyMessageWithModifier.modifier)
                )
            )
                .setDefaultValue(
                    ModifierKeyCode.of(
                        Config.values.keyCopyMessageWithModifier.key,
                        Modifier.of(Config.values.keyCopyMessageWithModifier.modifier)
                    )
                )
                .setKeySaveConsumer {
                    Config.values.keyCopyMessageWithModifier.key = it
                }
                .setModifierSaveConsumer {
                    Config.values.keyCopyMessageWithModifier.modifier = it.modifier.value
                }
                .build()
        )
        keyBinds.addEntry(entryBuilder.booleanToggle(
            "key.copyMessage.noFormatting.toggle",
            Config.values.copyNoFormatting
        ) { Config.values.copyNoFormatting = it })
    }

    private fun addTranslatorRegexOptions(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val chatTabs = builder.getOrCreateCategory(Component.translatable("chatPlus.translator.title"))
        chatTabs.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.translator.translatorToggle",
                Config.values.translatorEnabled
            ) { Config.values.translatorEnabled = it })
        val languageNamesSpeak: MutableList<String> = mutableListOf()
        val languageNames = languages.map {
            val name = it.name
            if (name != "Auto Detect") {
                languageNamesSpeak.add(name)
            }
            name
        }
        chatTabs.addEntry(entryBuilder.startDropdownMenu(
            Component.translatable("chatPlus.translator.translateTo"),
            DropdownMenuBuilder.TopCellElementBuilder.of(Config.values.translateTo) { str -> str },
            DropdownMenuBuilder.CellCreatorBuilder.of()
        )
            .setTooltip(Component.translatable("chatPlus.translator.translateTo.tooltip"))
            .setDefaultValue(Config.values.translateTo)
            .setSelections(languageNames)
            .setErrorSupplier { str: String ->
                if (languageNames.contains(str)) {
                    Optional.empty()
                } else {
                    Optional.of(Component.translatable("chatPlus.translator.translateInvalid"))
                }
            }
            .setSaveConsumer { str: String ->
                Config.values.translateTo = str
                LanguageManager.updateTranslateLanguages()
                queueUpdateConfig = true
            }
            .build()
        )
        chatTabs.addEntry(entryBuilder.startDropdownMenu(
            Component.translatable("chatPlus.translator.translateSelf"),
            DropdownMenuBuilder.TopCellElementBuilder.of(Config.values.translateSelf) { str -> str },
            DropdownMenuBuilder.CellCreatorBuilder.of()
        )
            .setTooltip(Component.translatable("chatPlus.translator.translateSelf.tooltip"))
            .setDefaultValue(Config.values.translateSelf)
            .setSelections(languageNames)
            .setErrorSupplier { str: String ->
                if (languageNames.contains(str)) {
                    Optional.empty()
                } else {
                    Optional.of(Component.translatable("chatPlus.translator.translateInvalid"))
                }
            }
            .setSaveConsumer { str: String ->
                Config.values.translateSelf = str
                LanguageManager.updateTranslateLanguages()
                queueUpdateConfig = true
            }
            .build()
        )
        chatTabs.addEntry(entryBuilder.startDropdownMenu(
            Component.translatable("chatPlus.translator.translateSpeak"),
            DropdownMenuBuilder.TopCellElementBuilder.of(Config.values.translateSpeak) { str -> str },
            DropdownMenuBuilder.CellCreatorBuilder.of()
        )
            .setTooltip(Component.translatable("chatPlus.translator.translateSpeak.tooltip"))
            .setDefaultValue(Config.values.translateSpeak)
            .setSelections(languageNamesSpeak)
            .setErrorSupplier { str: String ->
                if (languageNamesSpeak.contains(str)) {
                    Optional.empty()
                } else {
                    Optional.of(Component.translatable("chatPlus.translator.translateInvalid"))
                }
            }
            .setSaveConsumer { str: String ->
                Config.values.translateSpeak = str
                LanguageManager.updateTranslateLanguages()
                queueUpdateConfig = true
            }
            .build()
        )

        chatTabs.addEntry(
            getCustomListOption(
                "chatPlus.translator.regexes",
                Config.values.translatorRegexes,
                { Config.values.translatorRegexes = it },
                true,
                { RegexMatch("", 0) },
                { value ->
                    listOf(
                        entryBuilder.startStrField(Component.translatable("chatPlus.translator.match"), value.match)
                            .setTooltip(Component.translatable("chatPlus.translator.match.tooltip"))
                            .setDefaultValue("")
                            .setSaveConsumer { value.match = it }
                            .build(),
                        entryBuilder.startIntField(
                            Component.translatable("chatPlus.translator.senderNameGroupIndex"),
                            value.senderNameGroupIndex
                        )
                            .setTooltip(Component.translatable("chatPlus.translator.senderNameGroupIndex.tooltip"))
                            .setDefaultValue(0)
                            .setSaveConsumer { value.senderNameGroupIndex = it }
                            .build(),
                    )
                }

            )
        )
    }


    private fun ConfigEntryBuilder.booleanToggle(
        translatable: String,
        variable: Boolean,
        saveConsumer: Consumer<Boolean>
    ): BooleanListEntry {
        return startBooleanToggle(Component.translatable(translatable), variable)
            .setDefaultValue(variable)
            .setTooltip(Component.translatable("$translatable.tooltip"))
            .setSaveConsumer {
                saveConsumer.accept(it)
                queueUpdateConfig = true
            }
            .build()
    }

    private fun ConfigEntryBuilder.percentSlider(translatable: String, variable: Float, saveConsumer: Consumer<Float>): IntegerSliderEntry {
        return percentSlider(translatable, variable, 0, 1, saveConsumer)
    }

    private fun ConfigEntryBuilder.percentSlider(translatable: String, variable: Float, min: Int, max: Int, saveConsumer: Consumer<Float>):
            IntegerSliderEntry {
        val intValue = (variable * 100).toInt()
        return startIntSlider(Component.translatable(translatable), intValue, min * 100, max * 100)
            .setDefaultValue(intValue)
            .setTooltip(Component.translatable("$translatable.tooltip"))
            .setTextGetter { Component.literal("$it%") }
            .setSaveConsumer {
                saveConsumer.accept(it / 100f)
                queueUpdateConfig = true
            }
            .build()
    }

    private fun ConfigEntryBuilder.intSlider(
        translatable: String,
        variable: Int,
        min: Int,
        max: Int,
        saveConsumer: Consumer<Int>
    ): IntegerSliderEntry {
        return startIntSlider(Component.translatable(translatable), variable, min, max)
            .setDefaultValue(variable)
            .setTooltip(Component.translatable("$translatable.tooltip"))
            .setSaveConsumer {
                saveConsumer.accept(it)
                queueUpdateConfig = true
            }
            .build()
    }

    private fun <T> getCustomListOption(
        translatable: String,
        list: MutableList<T>,
        saveConsumer: Consumer<MutableList<T>>,
        canDelete: Boolean,
        create: () -> T,
        render: (T) -> List<AbstractConfigListEntry<*>>
    ): NestedListListEntry<T, MultiElementListEntry<T>> {
        return NestedListListEntry(
            Component.translatable(translatable),
            list,
            true,
            { Optional.empty() },
            saveConsumer,
            { mutableListOf() },
            Component.literal("Reset"),
            canDelete,
            false,
            { value, entry ->
                val v = value ?: create()
                MultiElementListEntry(Component.empty(), v, render(v), true)
            }
        )
    }

    private fun ConfigEntryBuilder.keyCodeOption(
        translatable: String,
        variable: InputConstants.Key,
        saveConsumer: Consumer<InputConstants.Key>
    ): KeyCodeEntry {
        return startKeyCodeField(Component.translatable(translatable), variable)
            .setDefaultValue(variable)
            //.setTooltip(Component.translatable("$translatable.tooltip"))
            .setKeySaveConsumer {
                saveConsumer.accept(it)
                queueUpdateConfig = true
            }
            .build()
    }
}
