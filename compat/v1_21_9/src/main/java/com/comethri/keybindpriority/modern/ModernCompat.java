package com.comethri.keybindpriority.modern;

import com.comethri.keybindpriority.Compat;
import com.comethri.keybindpriority.RowContent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

import java.util.List;

/** Minecraft 1.21.9+ (built against 1.21.11): categories are records, keys arrive as KeyEvent. */
public class ModernCompat implements Compat {
    @Override
    public Component categoryLabel(KeyMapping mapping) {
        return mapping.getCategory().label();
    }

    @Override
    public KeyMapping newTestMapping(String name, InputConstants.Key key) {
        return new KeyMapping(name, KeyConflictContext.UNIVERSAL, key, KeyMapping.Category.MISC);
    }

    @Override
    public boolean matchesKeysym(KeyMapping mapping, int keysym) {
        return mapping.matches(new KeyEvent(keysym, 0, 0));
    }

    @Override
    public void drawText(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color);
    }

    @Override
    public ContainerObjectSelectionList.Entry<?> newRow(RowContent content) {
        return new Row(content);
    }

    private static final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final RowContent content;

        Row(RowContent content) {
            this.content = content;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            content.render(graphics, getContentX(), getContentY(), getContentWidth(), mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return content.children();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return content.narratables();
        }
    }
}
