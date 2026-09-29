package com.comethri.keybindpriority;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

import java.util.List;

/** A menu row, independent of how the running Minecraft version lays out list entries. */
public interface RowContent {
    void render(GuiGraphics graphics, int left, int top, int width, int mouseX, int mouseY, float partialTick);

    List<? extends GuiEventListener> children();

    List<? extends NarratableEntry> narratables();
}
