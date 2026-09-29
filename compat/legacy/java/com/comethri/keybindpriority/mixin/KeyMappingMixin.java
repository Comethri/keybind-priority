package com.comethri.keybindpriority.mixin;

import com.comethri.keybindpriority.KeyFilter;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyMappingLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;

import java.util.List;

/**
 * NeoForge hands a key press to every binding on that key. These hooks drop the suppressed ones at each
 * place a binding learns about its key: clicks, held state, the resync after closing a screen, and the
 * {@code matches} checks screens use.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @WrapOperation(method = "click", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/settings/KeyMappingLookup;getAll(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Ljava/util/List;"))
    private static List<KeyMapping> keybindPriority$filterClick(KeyMappingLookup lookup, InputConstants.Key key,
                                                                Operation<List<KeyMapping>> original) {
        return KeyFilter.filter(original.call(lookup, key));
    }

    // NeoForge 21.1.x later added a "releasing" flag to this lookup; older 21.0/21.1 builds call getAll(key).
    // Exactly one of the two exists, the group makes sure one of them applies.
    @Group(name = "keybindPriority$set", min = 1)
    @WrapOperation(method = "set", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/settings/KeyMappingLookup;getAll(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)Ljava/util/List;"))
    private static List<KeyMapping> keybindPriority$filterSet(KeyMappingLookup lookup, InputConstants.Key key,
                                                              boolean releasing, Operation<List<KeyMapping>> original) {
        List<KeyMapping> found = original.call(lookup, key, releasing);
        // Releasing always reaches everyone, so nothing can get stuck held down.
        return releasing ? found : KeyFilter.filter(found);
    }

    @Group(name = "keybindPriority$set", min = 1)
    @WrapOperation(method = "set", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/settings/KeyMappingLookup;getAll(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Ljava/util/List;"))
    private static List<KeyMapping> keybindPriority$filterSetLegacy(KeyMappingLookup lookup, InputConstants.Key key,
                                                                    Operation<List<KeyMapping>> original,
                                                                    @Local(argsOnly = true) boolean down) {
        List<KeyMapping> found = original.call(lookup, key);
        return down ? KeyFilter.filter(found) : found;
    }

    @WrapOperation(method = "setAll", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V"))
    private static void keybindPriority$filterSetAll(KeyMapping mapping, boolean down, Operation<Void> original) {
        original.call(mapping, down && !KeyFilter.isSuppressed(mapping));
    }

    @ModifyReturnValue(method = "matches(II)Z", at = @At("RETURN"))
    private boolean keybindPriority$filterMatches(boolean matches) {
        return matches && !KeyFilter.isSuppressed((KeyMapping) (Object) this);
    }
}
