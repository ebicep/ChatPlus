package com.ebicep.chatplus.config.fabric

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.TimestampMode
import com.ebicep.chatplus.hud.ChatManager
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.function.Consumer

object ConfigScreenImpl {

    private fun ConfigEntryBuilder.booleanToggle(
        translatable: String,
        variable: Boolean,
        saveConsumer: Consumer<Boolean>
    ): BooleanListEntry {
        return startBooleanToggle(Component.translatable(translatable), variable)
            .setDefaultValue(variable)
            .setTooltip(Component.translatable("$translatable.tooltip"))
            .setSaveConsumer(saveConsumer)
            .build()
    }

    private fun ConfigEntryBuilder.percentSlider(translatable: String, variable: Float, saveConsumer: Consumer<Float>): IntegerSliderEntry {
        return percentSlider(translatable, variable, 0, 1, saveConsumer)
    }

    private fun ConfigEntryBuilder.percentSlider(translatable: String, variable: Float, min: Int, max: Int, saveConsumer: Consumer<Float>):
            IntegerSliderEntry {
        val intValue = (variable * 100).toInt()
        return startIntSlider(Component.translatable(translatable), intValue, min, max)
            .setDefaultValue(intValue)
            .setTooltip(Component.translatable("$translatable.tooltip"))
            .setSaveConsumer { saveConsumer.accept(it / 100f) }
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
            .setSaveConsumer(saveConsumer)
            .build()
    }

    @JvmStatic
    fun getConfigScreen(previousScreen: Screen? = null): Screen {
        val builder: ConfigBuilder = ConfigBuilder.create()
            .setParentScreen(previousScreen)
            .setTitle(Component.literal("chatplus.title"))
            .setSavingRunnable(Config::save)
            .transparentBackground()
        val entryBuilder: ConfigEntryBuilder = builder.entryBuilder()
        addGeneralOptions(builder, entryBuilder)
        addChatTabsOption(builder, entryBuilder)
        return builder.build()
    }

    private fun addGeneralOptions(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val general = builder.getOrCreateCategory(Component.translatable("chatplus.title"))
        general.addEntry(entryBuilder.booleanToggle("chatPlus.chatSettings.toggle", Config.values.enabled) { Config.values.enabled = it })
        general.addEntry(
            entryBuilder.intSlider(
                "chatplus.config.general.enabled",
                Config.values.maxMessages,
                1000,
                10_000_000
            ) { Config.values.maxMessages = it })
        general.addEntry(entryBuilder.percentSlider("chatPlus.chatSettings.maxMessages", Config.values.scale) { Config.values.scale = it })
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
            .setDefaultValue(Config.values.chatTimestampMode)
            .setTooltip(Component.translatable("chatPlus.chatSettings.chatTimestampMode.tooltip"))
            .setSaveConsumer { Config.values.chatTimestampMode = it }
            .build())
    }

    private fun addChatTabsOption(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val chatTabs = builder.getOrCreateCategory(Component.translatable("chatPlus.chatTabs.title"))
        val tabNameList = mutableListOf<String>()
        val tabPatternList = mutableListOf<String>()
        ChatManager.chatTabs.forEach { tab ->
            tabNameList.add(tab.name)
            tabPatternList.add(tab.pattern)
        }
//        chatTabs.addEntry(
//            entryBuilder.startStrList(Component.literal("A list of Strings"), tabNameList)
//                .setTooltip(Component.literal("Yes this is some beautiful tooltip\nOh and this is the second line!"))
//                .setDefaultValue(tabPatternList).build()
//        )

//ClothConfigDemo
    }

}