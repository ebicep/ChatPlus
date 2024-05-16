package com.ebicep.chatplus.config.neoforge

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.TimestampMode
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.config.serializers.KeyWithModifier
import com.ebicep.chatplus.features.AlignMessage
import com.ebicep.chatplus.features.FilterHighlight
import com.ebicep.chatplus.features.FilterHighlight.DEFAULT_COLOR
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.features.speechtotext.SpeechToText
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.translator.LanguageManager
import com.ebicep.chatplus.translator.RegexMatch
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
            .setSavingRunnable {
                Config.save()
                ChatManager.selectedTab.rescaleChat()
            }
            .transparentBackground()
        val entryBuilder: ConfigEntryBuilder = builder.entryBuilder()
        addGeneralOptions(builder, entryBuilder)
        addChatTabsOption(builder, entryBuilder)
        addFilterHighlightOption(builder, entryBuilder)
        addHoverHighlightOption(builder, entryBuilder)
        addBookmarkOption(builder, entryBuilder)
        addFindMessageOption(builder, entryBuilder)
        addChatScreenShotOption(builder, entryBuilder)
        addPlayerHeadChatDisplayOption(builder, entryBuilder)
        addKeyBindOptions(builder, entryBuilder)
        addTranslatorOptions(builder, entryBuilder)
        addSpeechToTextOptions(builder, entryBuilder)
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
        general.addEntry(
            entryBuilder.percentSlider(
                "chatPlus.chatSettings.chatTextSize",
                Config.values.scale,
                { Config.values.scale = it })
        )
        general.addEntry(
            entryBuilder.percentSlider(
                "chatPlus.chatSettings.textOpacity",
                Config.values.textOpacity,
                { Config.values.textOpacity = it })
        )
        general.addEntry(
            entryBuilder.percentSlider(
                "chatPlus.chatSettings.backgroundOpacity",
                Config.values.backgroundOpacity,
                { Config.values.backgroundOpacity = it }
            )
        )
        general.addEntry(
            entryBuilder.percentSlider(
                "chatPlus.chatSettings.unfocusedHeight",
                Config.values.unfocusedHeight,
                { Config.values.unfocusedHeight = it }
            )
        )
        general.addEntry(
            entryBuilder.percentSlider(
                "chatPlus.chatSettings.lineSpacing",
                Config.values.lineSpacing,
                { Config.values.lineSpacing = it }
            )
        )
        general.addEntry(
            entryBuilder.startEnumSelector(
                Component.translatable("chatPlus.chatSettings.chatTimestampMode"),
                TimestampMode::class.java,
                Config.values.chatTimestampMode
            )
                .setEnumNameProvider { (it as TimestampMode).translatable }
                .setDefaultValue(Config.values.chatTimestampMode)
                .setTooltip(Component.translatable("chatPlus.chatSettings.chatTimestampMode.tooltip"))
                .setSaveConsumer { Config.values.chatTimestampMode = it }
                .build())
        general.addEntry(entryBuilder.startEnumSelector(
            Component.translatable("chatPlus.chatSettings.messageAlignment"),
            AlignMessage.Alignment::class.java,
            Config.values.messageAlignment
        )
            .setEnumNameProvider { (it as AlignMessage.Alignment).translatable }
            .setDefaultValue(Config.values.messageAlignment)
            .setTooltip(Component.translatable("chatPlus.chatSettings.messageAlignment.tooltip"))
            .setSaveConsumer { Config.values.messageAlignment = it }
            .build())
    }

    private fun addChatTabsOption(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val chatTabs = builder.getOrCreateCategory(Component.translatable("chatPlus.chatTabs.title"))
        chatTabs.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.chatTabs.toggle",
                Config.values.chatTabsEnabled
            ) { Config.values.chatTabsEnabled = it })
        chatTabs.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.chatTabs.scrollCycleTabEnabled.toggle",
                Config.values.scrollCycleTabEnabled
            ) { Config.values.scrollCycleTabEnabled = it })
        chatTabs.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.chatTabs.arrowCycleTabEnabled.toggle",
                Config.values.arrowCycleTabEnabled
            ) { Config.values.arrowCycleTabEnabled = it })
        chatTabs.addEntry(
            getCustomListOption(
                "chatPlus.chatTabs.title",
                Config.values.chatTabs,
                {
                    Config.values.chatTabs = it
                    Config.resetSortedChatTabs()
                },
                Config.values.chatTabs.size > 0,
                { ChatTab("", "") },
                { value ->
                    listOf(
                        entryBuilder.stringField("chatPlus.chatTabs.name", value.name) { value.name = it },
                        entryBuilder.stringField("chatPlus.chatTabs.pattern", value.pattern) { value.pattern = it },
                        entryBuilder.booleanToggle(
                            "chatPlus.messageFilter.formatted.toggle",
                            value.formatted
                        ) { value.formatted = it },
                        entryBuilder.stringField("chatPlus.chatTabs.autoPrefix", value.autoPrefix) { value.autoPrefix = it },
                        entryBuilder.startIntField(
                            Component.translatable("chatPlus.chatTabs.priority"),
                            value.priority
                        )
                            .setTooltip(Component.translatable("chatPlus.chatTabs.priority.tooltip"))
                            .setDefaultValue(0)
                            .setSaveConsumer { value.priority = it }
                            .build(),
                        entryBuilder.booleanToggle(
                            "chatPlus.chatTabs.alwaysAdd",
                            value.alwaysAdd
                        ) { value.alwaysAdd = it },
                        entryBuilder.booleanToggle(
                            "chatPlus.chatTabs.skipOthers",
                            value.skipOthers
                        ) { value.skipOthers = it },
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
                        entryBuilder.booleanToggle(
                            "chatPlus.messageFilter.formatted.toggle",
                            value.formatted
                        ) { value.formatted = it },
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

    private fun addHoverHighlightOption(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val hoverHighlight = builder.getOrCreateCategory(Component.translatable("chatPlus.hoverHighlight.title"))
        hoverHighlight.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.hoverHighlight.toggle",
                Config.values.hoverHighlightEnabled
            ) { Config.values.hoverHighlightEnabled = it })
        hoverHighlight.addEntry(
            entryBuilder.startAlphaColorField(
                Component.translatable("chatPlus.hoverHighlight.color"),
                Color.ofTransparent(Config.values.hoverHighlightColor)
            )
                .setTooltip(Component.translatable("chatPlus.hoverHighlight.color.tooltip"))
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

    private fun addBookmarkOption(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val bookmark = builder.getOrCreateCategory(Component.translatable("chatPlus.bookmark.title"))
        bookmark.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.bookmark.toggle",
                Config.values.bookmarkEnabled
            ) { Config.values.bookmarkEnabled = it })
        bookmark.addEntry(
            entryBuilder.startAlphaColorField(
                Component.translatable("chatPlus.bookmark.color"),
                Color.ofTransparent(Config.values.bookmarkColor)
            )
                .setTooltip(Component.translatable("chatPlus.bookmark.color.tooltip"))
                .setAlphaMode(true)
                .setDefaultValue2 {
                    Color.ofTransparent(Config.values.bookmarkColor)
                }
                .setSaveConsumer2 {
                    Config.values.bookmarkColor = it.color
                }
                .build()
        )
        bookmark.addEntry(
            entryBuilder.startModifierKeyCodeField(
                Component.translatable("chatPlus.bookmark.key"),
                ModifierKeyCode.of(
                    Config.values.bookmarkKey.key,
                    Modifier.of(Config.values.bookmarkKey.modifier)
                )
            )
                .setTooltip(Component.translatable("chatPlus.bookmark.key.tooltip"))
                .setDefaultValue(
                    ModifierKeyCode.of(
                        Config.values.bookmarkKey.key,
                        Modifier.of(Config.values.bookmarkKey.modifier)
                    )
                )
                .setKeySaveConsumer { Config.values.bookmarkKey.key = it }
                .setModifierSaveConsumer { Config.values.bookmarkKey.modifier = it.modifier.value }
                .build()
        )
        bookmark.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.bookmark.textBarElement.toggle",
                Config.values.bookmarkTextBarElementEnabled
            ) { Config.values.bookmarkTextBarElementEnabled = it })
        bookmark.addEntry(
            entryBuilder.keyCodeOptionWithModifier(
                "chatPlus.bookmark.show.key",
                Config.values.bookmarkTextBarElementKey
            )
        )
        bookmark.addEntry(
            getCustomListOption(
                "chatPlus.bookmark.auto.title",
                Config.values.autoBookMarkPatterns,
                { Config.values.autoBookMarkPatterns = it },
                true,
                { MessageFilter("") },
                { value ->
                    listOf(
                        entryBuilder.stringField("chatPlus.bookmark.auto.pattern", value.pattern) { value.pattern = it },
                        entryBuilder.booleanToggle(
                            "chatPlus.messageFilter.formatted.toggle",
                            value.formatted
                        ) { value.formatted = it },
                    )
                }

            )
        )
    }

    private fun addFindMessageOption(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val findMessage = builder.getOrCreateCategory(Component.translatable("chatPlus.findMessage.title"))
        findMessage.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.findMessage.toggle",
                Config.values.findMessageEnabled
            ) { Config.values.findMessageEnabled = it })
        findMessage.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.findMessage.highlightInputBox.toggle",
                Config.values.findMessageHighlightInputBox
            ) { Config.values.findMessageHighlightInputBox = it })
        findMessage.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.findMessage.textBarElement.toggle",
                Config.values.findMessageTextBarElementEnabled
            ) { Config.values.findMessageTextBarElementEnabled = it })
        findMessage.addEntry(
            entryBuilder.keyCodeOptionWithModifier(
                "chatPlus.findMessage.key",
                Config.values.findMessageKey
            )
        )
    }

    private fun addChatScreenShotOption(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val screenshot = builder.getOrCreateCategory(Component.translatable("chatPlus.screenshotChat.title"))
        screenshot.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.screenshotChat.toggle",
                Config.values.screenshotChatEnabled
            ) { Config.values.screenshotChatEnabled = it })
        screenshot.addEntry(
            entryBuilder.startModifierKeyCodeField(
                Component.translatable("chatPlus.screenshotChat.line.key"),
                ModifierKeyCode.of(
                    Config.values.screenshotChatLine.key,
                    Modifier.of(Config.values.screenshotChatLine.modifier)
                )
            )
                .setDefaultValue(
                    ModifierKeyCode.of(
                        Config.values.screenshotChatLine.key,
                        Modifier.of(Config.values.screenshotChatLine.modifier)
                    )
                )
                .setKeySaveConsumer { Config.values.screenshotChatLine.key = it }
                .setModifierSaveConsumer { Config.values.screenshotChatLine.modifier = it.modifier.value }
                .build()
        )
        screenshot.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.screenshotChatAutoUpload.toggle",
                Config.values.screenshotChatAutoUpload
            ) { Config.values.screenshotChatAutoUpload = it })
    }

    private fun addPlayerHeadChatDisplayOption(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val headDisplay = builder.getOrCreateCategory(Component.translatable("chatPlus.playerHeadChatDisplay.title"))
        headDisplay.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.playerHeadChatDisplayEnabled.toggle",
                Config.values.playerHeadChatDisplayEnabled
            ) { Config.values.playerHeadChatDisplayEnabled = it })
        headDisplay.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.playerHeadChatDisplayShowOnWrapped.toggle",
                Config.values.playerHeadChatDisplayShowOnWrapped
            ) { Config.values.playerHeadChatDisplayShowOnWrapped = it })
        headDisplay.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.playerHeadChatDisplayOffsetNonHeadMessages.toggle",
                Config.values.playerHeadChatDisplayOffsetNonHeadMessages
            ) { Config.values.playerHeadChatDisplayOffsetNonHeadMessages = it })
        headDisplay.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.playerHeadChatDisplayOffsetNonHeadMessagesShowOnWrapped.toggle",
                Config.values.playerHeadChatDisplayOffsetNonHeadMessagesShowOnWrapped
            ) { Config.values.playerHeadChatDisplayOffsetNonHeadMessagesShowOnWrapped = it })

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
            entryBuilder.keyCodeOptionWithModifier(
                "key.copyMessage",
                Config.values.keyCopyMessageWithModifier
            )
        )
        keyBinds.addEntry(entryBuilder.booleanToggle(
            "key.copyMessage.noFormatting.toggle",
            Config.values.copyNoFormatting
        ) { Config.values.copyNoFormatting = it })
        keyBinds.addEntry(
            entryBuilder.keyCodeOption("key.peekChat", Config.values.keyPeekChat) { Config.values.keyPeekChat = it }
        )
    }

    private fun addTranslatorOptions(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val translator = builder.getOrCreateCategory(Component.translatable("chatPlus.translator.title"))
        translator.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.translator.translatorToggle",
                Config.values.translatorEnabled
            ) { Config.values.translatorEnabled = it })
        val languageNamesSpeak: MutableList<String> = mutableListOf()
        val languageNames = LanguageManager.languages.map {
            val name = it.name
            if (name != "Auto Detect") {
                languageNamesSpeak.add(name)
            }
            name
        }
        translator.addEntry(
            entryBuilder.startDropdownMenu(
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
        translator.addEntry(
            entryBuilder.startDropdownMenu(
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
        translator.addEntry(
            entryBuilder.startDropdownMenu(
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
        translator.addEntry(
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
        translator.addEntry(
            entryBuilder.booleanToggle(
                "chatPlus.translator.translateClick.toggle",
                Config.values.translateClickEnabled
            ) { Config.values.translateClickEnabled = it })
    }

    private fun addSpeechToTextOptions(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val speechToText = builder.getOrCreateCategory(Component.translatable("chatPlus.speechToText"))
        speechToText.addEntry(entryBuilder.booleanToggle(
            "chatPlus.speechToText.toggle",
            Config.values.speechToTextEnabled
        ) { Config.values.speechToTextEnabled = it })
        speechToText.addEntry(entryBuilder.startIntField(
            Component.translatable("chatPlus.speechToText.speechToTextSampleRate"),
            Config.values.speechToTextSampleRate
        )
            .setTooltip(Component.translatable("chatPlus.speechToText.speechToTextSampleRate.tooltip"))
            .setDefaultValue(Config.values.speechToTextSampleRate)
            .setSaveConsumer { Config.values.speechToTextSampleRate = it }
            .build()
        )
        val microphoneNames = SpeechToText.getAllMicrophoneNames()
        microphoneNames.add(0, "Default")
        speechToText.addEntry(entryBuilder.startDropdownMenu(
            Component.translatable("chatPlus.speechToText.microphone"),
            DropdownMenuBuilder.TopCellElementBuilder.of(Config.values.speechToTextMicrophone) { str -> str },
            DropdownMenuBuilder.CellCreatorBuilder.of()
        )
            .setTooltip(Component.translatable("chatPlus.speechToText.microphone.tooltip"))
            .setDefaultValue(Config.values.speechToTextMicrophone)
            .setSelections(microphoneNames)
            .setErrorSupplier { str: String ->
                if (microphoneNames.contains(str)) {
                    Optional.empty()
                } else {
                    Optional.of(Component.translatable("chatPlus.speechToText.microphone.invalid"))
                }
            }
            .setSaveConsumer { str: String ->
                Config.values.speechToTextMicrophone = str
            }
            .build()
        )
        val models = SpeechToText.getAllPossibleModels()
        models.add(0, "")
        speechToText.addEntry(entryBuilder.startDropdownMenu(
            Component.translatable("chatPlus.speechToText.selectedAudioModel"),
            DropdownMenuBuilder.TopCellElementBuilder.of(Config.values.speechToTextSelectedAudioModel) { str -> str },
            DropdownMenuBuilder.CellCreatorBuilder.of()
        )
            .setTooltip(Component.translatable("chatPlus.speechToText.selectedAudioModel.tooltip"))
            .setDefaultValue(Config.values.speechToTextSelectedAudioModel)
            .setSelections(models)
            .setErrorSupplier { str: String ->
                if (models.contains(str)) {
                    Optional.empty()
                } else {
                    Optional.of(Component.translatable("chatPlus.speechToText.selectedAudioModel.invalid"))
                }
            }
            .setSaveConsumer { str: String ->
                Config.values.speechToTextSelectedAudioModel = str
            }
            .build()
        )
        speechToText.addEntry(
            entryBuilder.keyCodeOption(
                "key.speechToText.ptt",
                Config.values.speechToTextMicrophoneKey
            ) { Config.values.speechToTextMicrophoneKey = it }
        )
        speechToText.addEntry(
            entryBuilder.keyCodeOption(
                "key.speechToText.quickSend",
                Config.values.speechToTextQuickSendKey
            ) { Config.values.speechToTextQuickSendKey = it }
        )
        speechToText.addEntry(entryBuilder.booleanToggle(
            "chatPlus.speechToText.speechToTextTranslateEnabled.toggle",
            Config.values.speechToTextTranslateEnabled
        ) { Config.values.speechToTextTranslateEnabled = it })
        val languageNamesSpeak: MutableList<String> = mutableListOf()
        LanguageManager.languages.map {
            val name = it.name
            if (name != "Auto Detect") {
                languageNamesSpeak.add(name)
            }
            name
        }
        speechToText.addEntry(
            entryBuilder.startDropdownMenu(
                Component.translatable("chatPlus.speechToText.speechToTextTranslateLang"),
                DropdownMenuBuilder.TopCellElementBuilder.of(Config.values.speechToTextTranslateLang) { str -> str },
                DropdownMenuBuilder.CellCreatorBuilder.of()
            )
                .setTooltip(Component.translatable("chatPlus.speechToText.speechToTextTranslateLang.tooltip"))
                .setDefaultValue(Config.values.speechToTextTranslateLang)
                .setSelections(languageNamesSpeak)
                .setErrorSupplier { str: String ->
                    if (languageNamesSpeak.contains(str)) {
                        Optional.empty()
                    } else {
                        Optional.of(Component.translatable("chatPlus.translator.translateInvalid"))
                    }
                }
                .setSaveConsumer { str: String ->
                    Config.values.speechToTextTranslateLang = str
                    SpeechToText.updateTranslateLanguage()
                    queueUpdateConfig = true
                }
                .build()
        )
    }


    private fun ConfigEntryBuilder.stringField(translatable: String, variable: String, saveConsumer: Consumer<String>): StringListEntry {
        return startStrField(Component.translatable(translatable), variable)
            .setDefaultValue(variable)
            .setTooltip(Component.translatable("$translatable.tooltip"))
            .setSaveConsumer {
                saveConsumer.accept(it)
                queueUpdateConfig = true
            }
            .build()
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

    private fun ConfigEntryBuilder.percentSlider(
        translatable: String,
        variable: Float,
        saveConsumer: Consumer<Float>,
        updateDimensions: Boolean = true
    ): IntegerSliderEntry {
        return percentSlider(translatable, variable, 0, 1, saveConsumer, updateDimensions)
    }

    private fun ConfigEntryBuilder.percentSlider(
        translatable: String,
        variable: Float,
        min: Int,
        max: Int,
        saveConsumer: Consumer<Float>,
        updateDimensions: Boolean = true
    ):
            IntegerSliderEntry {
        val intValue = (variable * 100).toInt()
        return startIntSlider(Component.translatable(translatable), intValue, min * 100, max * 100)
            .setDefaultValue(intValue)
            .setTooltip(Component.translatable("$translatable.tooltip"))
            .setTextGetter { Component.literal("$it%") }
            .setSaveConsumer {
                saveConsumer.accept(it / 100f)
                if (updateDimensions) {
                    ChatRenderer.updateCachedDimension()
                }
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

    private fun ConfigEntryBuilder.keyCodeOptionWithModifier(
        translatable: String,
        variable: KeyWithModifier
    ): KeyCodeEntry {
        return startModifierKeyCodeField(
            Component.translatable(translatable),
            ModifierKeyCode.of(
                variable.key,
                Modifier.of(variable.modifier)
            )
        )
            .setTooltip(Component.translatable("$translatable.tooltip"))
            .setDefaultValue(
                ModifierKeyCode.of(
                    variable.key,
                    Modifier.of(variable.modifier)
                )
            )
            .setKeySaveConsumer {
                variable.key = it
            }
            .setModifierSaveConsumer {
                variable.modifier = it.modifier.value
            }
            .build()
    }
}
