package com.comethri.keybindpriority;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Shared colors and small drawing helpers. Only uses GuiGraphics methods that are identical in every 1.21.x. */
final class Ui {
    // Full ARGB: since 1.21.6 text with alpha 0 is invisible.
    static final int WHITE = 0xFFFFFFFF;
    static final int TEXT = 0xFFE0E0E0;
    static final int MUTED = 0xFFA0A0A0;
    static final int DIM = 0xFF7A7A7A;
    static final int GOLD = 0xFFFFC94D;
    static final int GREEN = 0xFF6BD66B;
    static final int RED = 0xFFFF6B6B;
    static final int ORANGE = 0xFFFFA54D;
    static final int AQUA = 0xFF5ED8E6;

    private static final int PANEL = 0xE0101016;
    private static final int PANEL_EDGE = 0xFF3C3C48;

    private Ui() {
    }

    static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    static void panel(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.fill(x1, y1, x2, y2, PANEL);
        graphics.renderOutline(x1, y1, x2 - x1, y2 - y1, PANEL_EDGE);
    }

    static int chipWidth(Font font, String text) {
        return font.width(text) + 8;
    }

    /** A small colored label; {@code right} is its right edge. Returns the left edge. */
    static int chip(GuiGraphics graphics, Font font, String text, int right, int y, int color) {
        int left = right - chipWidth(font, text);
        graphics.fill(left, y, right, y + 11, withAlpha(color, 0x30));
        graphics.renderOutline(left, y, right - left, 11, withAlpha(color, 0x90));
        Compat.get().drawText(graphics, font, text, left + 4, y + 2, color);
        return left;
    }

    static int keyWidth(Font font, String key) {
        return font.width(key) + 10;
    }

    /** A key cap like [ V ]. */
    static void key(GuiGraphics graphics, Font font, String key, int x, int y) {
        int w = keyWidth(font, key);
        graphics.fill(x, y, x + w, y + 14, 0xFF202028);
        graphics.fill(x, y + 12, x + w, y + 14, 0xFF101014);
        graphics.renderOutline(x, y, w, 14, withAlpha(GOLD, 0xC0));
        Compat.get().drawText(graphics, font, key, x + 5, y + 3, GOLD);
    }

    /** Card behind a binding row, with a colored bar on the left when {@code accent != 0}. */
    static void card(GuiGraphics graphics, int x1, int y1, int x2, int y2, int accent, boolean hovered) {
        graphics.fill(x1, y1, x2, y2, 0x48000000);
        if (hovered) graphics.fill(x1, y1, x2, y2, 0x14FFFFFF);
        if (accent != 0) {
            graphics.fill(x1, y1, x2, y2, withAlpha(accent, 0x14));
            graphics.fill(x1, y1, x1 + 2, y2, accent);
        }
    }
}
