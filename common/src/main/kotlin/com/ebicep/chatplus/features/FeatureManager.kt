package com.ebicep.chatplus.features

import com.ebicep.chatplus.features.chattabs.ChatTabs
import com.ebicep.chatplus.features.speechtotext.SpeechToText
import com.ebicep.chatplus.features.textbarelements.TextBarElements

object FeatureManager {

    init {
        // TODO feature interface with register/unregister methods for small performance? Less event calls
        CopyMessage
        MovableChat
        HoverHighlight
        TextBarElements
        CompactMessages
        FilterHighlight
        ChatTabs
        TranslateText
        AlignText
        ScreenshotChat
        SpeechToText
        PeakChat
        PlayerHeadChatDisplay
//        AlternatingColorBackground
    }

}