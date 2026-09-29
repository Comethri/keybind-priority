package com.comethri.keybindpriority.v1_21_6;

import com.comethri.keybindpriority.legacy.LegacyCompat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Minecraft 1.21.6 - 1.21.8 (built against 1.21.8): same as 1.21.1 except drawString returns void. */
public class Compat1216 extends LegacyCompat {
    @Override
    public void drawText(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color);
    }
}
