package com.comethri.keybindpriority.modern;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.TreeSet;

/** Only loaded on Minecraft 1.21.11+, where item IDs use Identifier. */
public final class ModernItemSelectors {
    private ModernItemSelectors() {
    }

    public static boolean valid(String selector) {
        boolean tag = selector.startsWith("#");
        Identifier id = Identifier.tryParse(tag ? selector.substring(1) : selector);
        return id != null && !id.getPath().isEmpty() && (tag || BuiltInRegistries.ITEM.containsKey(id));
    }

    public static boolean matches(ItemStack stack, String selector) {
        boolean tag = selector.startsWith("#");
        Identifier id = Identifier.tryParse(tag ? selector.substring(1) : selector);
        if (id == null) return false;
        return tag ? stack.is(TagKey.create(Registries.ITEM, id)) : id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static List<String> selectors() {
        TreeSet<String> selectors = new TreeSet<>();
        BuiltInRegistries.ITEM.keySet().forEach(id -> {
            selectors.add(id.toString());
            selectors.add("@" + id.getNamespace());
        });
        BuiltInRegistries.ITEM.stream().flatMap(item -> item.builtInRegistryHolder().tags())
                .forEach(tag -> selectors.add("#" + tag.location()));
        return List.copyOf(selectors);
    }
}
