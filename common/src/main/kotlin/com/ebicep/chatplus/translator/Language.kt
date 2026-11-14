package com.ebicep.chatplus.translator

data class Language(
    val name: String,
    val nameUnicode: String?,
    val googleCode: String,
    val baiduCode: String? = null,
)
