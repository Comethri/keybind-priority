package com.comethri.keybindpriority;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Minecraft 1.21 - 1.21.8: categories are translation keys, keys arrive as int keysym/scancode. */
public final class Compat {
    private Compat() {
    }

    public static Component categoryLabel(KeyMapping mapping) {
        return Component.translatable(mapping.getCategory());
    }

    public static KeyMapping newTestMapping(String name, InputConstants.Key key) {
        return new KeyMapping(name, KeyConflictContext.UNIVERSAL, key, "key.categories.misc");
    }

    public static boolean matchesKeysym(KeyMapping mapping, int keysym) {
        return mapping.matches(keysym, 0);
    }

    /** List row that hands its layout to {@link #renderRow}. */
    public abstract static class Row<E extends Row<E>> extends ContainerObjectSelectionList.Entry<E> {
        protected abstract void renderRow(GuiGraphics graphics, int left, int top, int width,
                                          int mouseX, int mouseY, float partialTick);

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            renderRow(graphics, left, top, width, mouseX, mouseY, partialTick);
        }
    }
}
