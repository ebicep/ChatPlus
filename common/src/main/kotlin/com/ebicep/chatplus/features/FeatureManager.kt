package com.ebicep.chatplus.features

import com.ebicep.chatplus.features.chattabs.ChatTabs
import com.ebicep.chatplus.features.chatwindows.ChatWindowsManager
import com.ebicep.chatplus.features.internal.Debug
import com.ebicep.chatplus.features.internal.OnScreenDisplay
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
        FilterMessages
        ChatTabs
        TranslateMessage
        AlignMessage
        ScreenshotChat
        SpeechToText
        PeakChat
        PlayerHeadChatDisplay
        SelectChat
        ScrollBar
        BookmarkMessages
        Animations
        ChatWindowsManager
        HideChat
        Debug
        OnScreenDisplay
        TimestampMessages
        ChatPadding
        WrappedMessageLineIndent
//        AlternatingColorBackground
    }

}