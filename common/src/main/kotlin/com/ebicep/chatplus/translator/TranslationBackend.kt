package com.ebicep.chatplus.translator

import com.ebicep.chatplus.config.EnumTranslatableName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component

@Serializable
enum class TranslationBackend(private val translationKey: String) : EnumTranslatableName {
    GOOGLE_UNOFFICIAL("chatPlus.translator.backend.googleUnofficial"),
    GOOGLE_CLOUD("chatPlus.translator.backend.googleCloud"),
    DEEPL("chatPlus.translator.backend.deepl"),
    AZURE("chatPlus.translator.backend.azure"),
    LIBRETRANSLATE("chatPlus.translator.backend.libretranslate"),
    LINGVA("chatPlus.translator.backend.lingva"),
    MYMEMORY("chatPlus.translator.backend.mymemory"),
    BAIDU("chatPlus.translator.backend.baidu"),
    ;

    val translatable: Component = Component.translatable(translationKey)

    override fun getTranslatableName(): Component = translatable
}