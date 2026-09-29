package com.comethri.keybindpriority.mixin.modern;

import com.comethri.keybindpriority.PriorityScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ctrl + Alt + key in game opens the priority menu for that key instead of pressing it. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void keybindPriority$openMenu(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS || window != minecraft.getWindow().handle()) return;
        // Only in game: inside screens Ctrl + Alt is AltGr on many layouts (e.g. "@" in chat).
        if (minecraft.screen != null || !event.hasControlDown() || !event.hasAltDown()) return;
        if (event.key() >= GLFW.GLFW_KEY_LEFT_SHIFT && event.key() <= GLFW.GLFW_KEY_RIGHT_SUPER) return;
        InputConstants.Key pressed = InputConstants.getKey(event);
        if (pressed == InputConstants.UNKNOWN) return;
        minecraft.setScreen(new PriorityScreen(pressed));
        ci.cancel();
    }
}
