package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.ConfigScreen.getTabEditorScreen
import com.ebicep.chatplus.config.ConfigScreen.getWindowEditorScreen
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.hud.ChatManager.resetGlobalSortedTabs
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawString0
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import java.util.function.Consumer

class Editor(
    val previousScreen: Screen?,
    titleComponent: () -> String,
    val subTitleComponent: () -> String?,
    val pressClone: (Button) -> Unit,
    val pressEdit: (Button) -> Unit,
    val pressDelete: (Button) -> Unit,
    val canDelete: () -> Boolean,
) : Screen(Component.translatable(titleComponent())) {

    companion object {
        fun tabEditor(previousScreen: Screen?, chatTab: ChatTab): Editor {
            return Editor(
                previousScreen,
                { "chatPlus.gui.tabEditor" },
                { "( ${chatTab.name} ) " },
                { button ->
                    chatTab.chatWindow.tabSettings.cloneTab(chatTab)
                    Minecraft.getInstance().setScreen(previousScreen)
                },
                { button -> Minecraft.getInstance().setScreen(getTabEditorScreen(previousScreen, chatTab)) },
                { button ->
                    chatTab.chatWindow.tabSettings.removeTab(chatTab)
                    Minecraft.getInstance().setScreen(previousScreen)
                },
                { chatTab.chatWindow.tabSettings.tabs.size > 1 }
            )
        }

        fun windowEditor(previousScreen: Screen?, chatWindow: ChatWindow): Editor {
            return Editor(
                previousScreen,
                { "chatPlus.gui.windowEditor" },
                { "( ${chatWindow.tabSettings.tabs.joinToString(", ") { it.name }} )" },
                { button ->
                    val oldRenderer = chatWindow.renderer

                    val newWindow = chatWindow.clone()
                    newWindow.tabSettings.tabs = chatWindow.tabSettings.tabs.map { it.clone() }.toMutableList()

                    val newRenderer = newWindow.renderer
                    newRenderer.x = newRenderer.getUpdatedX(oldRenderer.x + 10)
                    newRenderer.y = newRenderer.getUpdatedY(oldRenderer.y - 10)
                    newRenderer.internalX = oldRenderer.x
                    newRenderer.internalY = oldRenderer.y
                    newRenderer.width = oldRenderer.width
                    newRenderer.height = oldRenderer.height
                    newRenderer.updateCachedDimension()

                    val windows = Config.values.chatWindows.toMutableList()
                    windows.add(newWindow)
                    Config.values.chatWindows = windows
                    resetGlobalSortedTabs()
                    Minecraft.getInstance().setScreen(previousScreen)
                },
                { button -> Minecraft.getInstance().setScreen(getWindowEditorScreen(previousScreen, chatWindow)) },
                { button ->
                    val windows = Config.values.chatWindows.toMutableList()
                    windows.remove(chatWindow)
                    Config.values.chatWindows = windows
                    Minecraft.getInstance().setScreen(previousScreen)
                },
                { Config.values.chatWindows.size > 1 }
            )
        }
    }

    lateinit var deleteButton: Button
    var deleteConfirm = 0

    override fun init() {
        val gridlayout = GridLayout()
        gridlayout.defaultCellSetting().padding(4, 30, 4, 0)
        val rowHelper = gridlayout.createRowHelper(1)

        rowHelper.addChild(
            Button.builder(
                Component.translatable("chatPlus.gui.clone"),
                Button.OnPress { pressClone(it) }
            ).width(200).build()
        )
        rowHelper.addChild(
            Button.builder(
                Component.translatable("chatPlus.gui.edit"),
                Button.OnPress { pressEdit(it) }
            ).width(200).build()
        )

        deleteButton = Button.builder(
            Component.translatable("chatPlus.gui.delete"),
            Button.OnPress {
                if (deleteConfirm > 0) {
                    pressDelete(it)
                } else {
                    it.message = Component.translatable("chatPlus.gui.deleteConfirm")
                    deleteConfirm = 40
                }
            }
        ).width(200).build()
        if (!canDelete()) {
            deleteButton.active = false
        }
        rowHelper.addChild(deleteButton)

        rowHelper.addChild(
            Button.builder(
                CommonComponents.GUI_DONE,
                Button.OnPress { this.minecraft!!.setScreen(previousScreen) }
            ).width(200).build(),
            1,
            gridlayout.newCellSettings().paddingTop(90)
        )

        gridlayout.arrangeElements()
        FrameLayout.alignInRectangle(gridlayout, 0, 20, this.width, this.height, 0.5f, 0.25f)
        gridlayout.visitWidgets(Consumer { guiEventListener: AbstractWidget -> this.addRenderableWidget(guiEventListener) })
    }

    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        super.render(guiGraphics, i, j, f)
        val poseStack = guiGraphics.pose()
        poseStack.createPose {
            drawText(poseStack, 2f, guiGraphics, this.title.visualOrderText)
        }
        subTitleComponent()?.let {
            poseStack.createPose {
                poseStack.translate0(y = 20f)
                drawText(poseStack, 1f, guiGraphics, Component.literal(it).visualOrderText)
            }
        }
    }

    private fun drawText(
        poseStack: PoseStack,
        scale: Float,
        guiGraphics: GuiGraphics,
        text: FormattedCharSequence
    ) {
        poseStack.scale(scale, scale, 1f)
        guiGraphics.drawString0(
            text,
            minecraft!!.window.guiScaledWidth * .5f / scale - minecraft!!.font.width(text) / 2f,
            10 / scale,
            0xFFFFFF,
        )
    }

    override fun tick() {
        super.tick()
        if (deleteConfirm > 0) {
            deleteConfirm--
            if (deleteConfirm == 0) {
                deleteButton.message = Component.translatable("chatPlus.gui.delete")
            }
        }
    }

}