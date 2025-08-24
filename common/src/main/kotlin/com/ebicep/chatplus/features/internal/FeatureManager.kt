package com.ebicep.chatplus.features.internal

import com.ebicep.chatplus.features.*
import com.ebicep.chatplus.features.chattabs.ChatTabs
import com.ebicep.chatplus.features.chatwindows.ChatWindowsManager
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
        PeekChat
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
        SaveInputBoxMessage
        DeleteMessages
        SendNote
        InputBoxAutoAdjustChatWindow
        MessageImagePreview
        InputBoxCharacterCount
        InputBoxNormalizeInput
//        AlternatingColorBackground
    }

}