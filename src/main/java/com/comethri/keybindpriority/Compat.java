package com.comethri.keybindpriority;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.network.chat.Component;

/**
 * Everything that differs between Minecraft 1.21.x versions. One jar carries three implementations, each
 * compiled against the version it is for, and {@link #get()} picks the one matching the running game:
 * <ul>
 *   <li>{@code legacy.LegacyCompat} (built against 1.21.1): 1.21 - 1.21.5</li>
 *   <li>{@code v1_21_6.Compat1216} (built against 1.21.8): 1.21.6 - 1.21.8, drawString returns void</li>
 *   <li>{@code modern.ModernCompat} (built against 1.21.11): 1.21.9+, KeyEvent input and Category records</li>
 * </ul>
 * Shared code must only call Minecraft methods whose exact signature exists in every version
 * ({@code tools/check_linkage.py} checks that).
 */
public interface Compat {
    Component categoryLabel(KeyMapping mapping);

    KeyMapping newTestMapping(String name, InputConstants.Key key);

    boolean matchesKeysym(KeyMapping mapping, int keysym);

    void drawText(GuiGraphics graphics, Font font, String text, int x, int y, int color);

    /** Wraps a row in the list entry type of this Minecraft version. */
    ContainerObjectSelectionList.Entry<?> newRow(RowContent content);

    static Compat get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        static final Compat INSTANCE = create();

        private Holder() {
        }

        private static Compat create() {
            String impl = Platform.MODERN_INPUT ? "modern.ModernCompat"
                    : Platform.textReturnsVoid() ? "v1_21_6.Compat1216"
                    : "legacy.LegacyCompat";
            KeybindPriority.LOG.info("Using compat layer {}", impl);
            try {
                return (Compat) Class.forName(Compat.class.getPackageName() + "." + impl)
                        .getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Keybind Priority: missing compat layer " + impl, e);
            }
        }
    }
}
