package com.comethri.keybindpriority;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Minecraft 1.21.9+: categories are records, keys arrive as KeyEvent. */
public final class Compat {
    private Compat() {
    }

    public static Component categoryLabel(KeyMapping mapping) {
        return mapping.getCategory().label();
    }

    public static KeyMapping newTestMapping(String name, InputConstants.Key key) {
        return new KeyMapping(name, KeyConflictContext.UNIVERSAL, key, KeyMapping.Category.MISC);
    }

    public static boolean matchesKeysym(KeyMapping mapping, int keysym) {
        return mapping.matches(new KeyEvent(keysym, 0, 0));
    }

    /** List row that hands its layout to {@link #renderRow}. */
    public abstract static class Row<E extends Row<E>> extends ContainerObjectSelectionList.Entry<E> {
        protected abstract void renderRow(GuiGraphics graphics, int left, int top, int width,
                                          int mouseX, int mouseY, float partialTick);

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            renderRow(graphics, getContentX(), getContentY(), getContentWidth(), mouseX, mouseY, partialTick);
        }
    }
}
