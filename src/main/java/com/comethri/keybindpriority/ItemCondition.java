package com.comethri.keybindpriority;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** A binding can require a held item, or take priority while that item is held. */
public final class ItemCondition {
    public enum Mode { REQUIRE, PREFER }
    public enum Hand { MAIN, OFF, EITHER }

    public String selector = "";
    public Mode mode = Mode.REQUIRE;
    public Hand hand = Hand.MAIN;
    private transient String compiledSelector;
    private transient ItemFilter compiledFilter;
    private transient String iconSelector;
    private transient ItemStack icon = ItemStack.EMPTY;

    public ItemCondition() {
    }

    public ItemCondition(String selector, Mode mode, Hand hand) {
        this.selector = selector.trim();
        this.mode = mode;
        this.hand = hand;
    }

    void sanitize() {
        if (selector == null) selector = "";
        selector = selector.trim();
        if (mode == null) mode = Mode.REQUIRE;
        if (hand == null) hand = Hand.MAIN;
    }

    public static boolean validSelector(String selector) {
        return ItemFilter.parse(selector.trim()).error() == null;
    }

    public boolean matches() {
        var player = Minecraft.getInstance().player;
        return player != null && matches(player.getMainHandItem(), player.getOffhandItem());
    }

    public boolean matches(ItemStack main, ItemStack off) {
        return (hand != Hand.OFF && matchesStack(main)) || (hand != Hand.MAIN && matchesStack(off));
    }

    /** An item this condition accepts, for the menu; empty if none does. */
    public ItemStack icon() {
        if (!selector.equals(iconSelector)) {
            iconSelector = selector;
            icon = filter().example();
        }
        return icon;
    }

    private boolean matchesStack(ItemStack stack) {
        return filter().matches(stack);
    }

    private ItemFilter filter() {
        if (compiledFilter == null || !selector.equals(compiledSelector)) {
            compiledSelector = selector;
            compiledFilter = ItemFilter.parse(selector);
        }
        return compiledFilter;
    }
}
