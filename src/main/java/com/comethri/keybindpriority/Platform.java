package com.comethri.keybindpriority;

import java.lang.reflect.Method;

/**
 * Detects the running Minecraft version by its features. Must not load Minecraft classes where the mixin
 * plugin uses it, so the input check only looks for the class file.
 */
public final class Platform {
    /** 1.21.9 reworked keyboard input around KeyEvent. */
    public static final boolean MODERN_INPUT = classExists("net/minecraft/client/input/KeyEvent.class");
    public static final boolean RENAMED_IDENTIFIERS = classExists("net/minecraft/resources/Identifier.class");

    private Platform() {
    }

    private static boolean classExists(String path) {
        for (ClassLoader loader : new ClassLoader[]{Platform.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()}) {
            if (loader != null && loader.getResource(path) != null) return true;
        }
        return false;
    }

    /** 1.21.6 changed GuiGraphics.drawString to return void. Only call once Minecraft is loaded. */
    static boolean textReturnsVoid() {
        try {
            Class<?> graphics = Class.forName("net.minecraft.client.gui.GuiGraphics");
            Class<?> font = Class.forName("net.minecraft.client.gui.Font");
            Method draw = graphics.getMethod("drawString", font, String.class, int.class, int.class, int.class);
            return draw.getReturnType() == void.class;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Keybind Priority: can't inspect GuiGraphics", e);
        }
    }
}
