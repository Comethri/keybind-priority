package com.comethri.keybindpriority.legacy;

import com.comethri.keybindpriority.Compat;
import com.comethri.keybindpriority.RowContent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

import java.util.List;

/** Minecraft 1.21 - 1.21.5 (built against 1.21.1): categories are translation keys, keys arrive as ints. */
public class LegacyCompat implements Compat {
    @Override
    public List<String> itemSelectors() {
        return LegacyItemSelectors.selectors();
    }

    @Override
    public boolean validItemSelector(String selector) {
        return LegacyItemSelectors.valid(selector);
    }

    @Override
    public boolean matchesItemSelector(ItemStack stack, String selector) {
        return LegacyItemSelectors.matches(stack, selector);
    }

    @Override
    public String itemId(ItemStack stack) {
        return LegacyItemSelectors.itemId(stack);
    }

    @Override
    public Component categoryLabel(KeyMapping mapping) {
        return Component.translatable(mapping.getCategory());
    }

    @Override
    public KeyMapping newTestMapping(String name, InputConstants.Key key) {
        return new KeyMapping(name, KeyConflictContext.UNIVERSAL, key, "key.categories.misc");
    }

    @Override
    public boolean matchesKeysym(KeyMapping mapping, int keysym) {
        return mapping.matches(keysym, 0);
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
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            content.render(graphics, left, top, width, mouseX, mouseY, partialTick);
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
