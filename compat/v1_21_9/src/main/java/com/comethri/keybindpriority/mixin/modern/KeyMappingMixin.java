package com.comethri.keybindpriority.mixin.modern;

import com.comethri.keybindpriority.KeyFilter;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyMappingLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * NeoForge hands a key press to every binding on that key. These hooks drop the suppressed ones at each
 * place a binding learns about its key: clicks and held state (both go through forAllKeyMappings since
 * 1.21.9), the resync after closing a screen, and the {@code matches} checks screens use.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @WrapOperation(method = "forAllKeyMappings(Lcom/mojang/blaze3d/platform/InputConstants$Key;Ljava/util/function/Consumer;Z)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/settings/KeyMappingLookup;getAll(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)Ljava/util/List;"))
    private static List<KeyMapping> keybindPriority$filter(KeyMappingLookup lookup, InputConstants.Key key,
                                                           boolean releasing, Operation<List<KeyMapping>> original) {
        List<KeyMapping> found = original.call(lookup, key, releasing);
        // Releasing always reaches everyone, so nothing can get stuck held down.
        return releasing ? found : KeyFilter.filter(found);
    }

    @WrapOperation(method = "setAll", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V"))
    private static void keybindPriority$filterSetAll(KeyMapping mapping, boolean down, Operation<Void> original) {
        original.call(mapping, down && !KeyFilter.isSuppressed(mapping));
    }

    @ModifyReturnValue(method = "matches(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("RETURN"))
    private boolean keybindPriority$filterMatches(boolean matches) {
        return matches && !KeyFilter.isSuppressed((KeyMapping) (Object) this);
    }
}
