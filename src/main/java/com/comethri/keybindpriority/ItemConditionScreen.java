package com.comethri.keybindpriority;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/** Edits a draft; Escape and Cancel leave the saved condition untouched. */
public final class ItemConditionScreen extends Screen {
    private static final String LANG = "screen." + KeybindPriority.MOD_ID + ".item.";
    private static final int PANEL_HEIGHT = 230;
    private static final int PAD = 10;
    private final PriorityScreen parent;
    private final String binding;
    private final boolean existing;
    private ItemCondition.Mode mode;
    private ItemCondition.Hand hand;
    private String value;
    private EditBox selector;
    private Button save;
    private Button held;
    private Button suggestion;
    private Button nextSuggestion;
    private List<String> catalog;
    private List<String> suggestions = List.of();
    private int suggestionIndex;
    private ItemFilter filter;
    private ItemStack example = ItemStack.EMPTY;
    private final ItemCondition preview = new ItemCondition();
    private int top;
    private int left;
    private int editorWidth;

    public ItemConditionScreen(PriorityScreen parent, String binding, ItemCondition condition) {
        super(Component.translatable(LANG + "title"));
        this.parent = parent;
        this.binding = binding;
        existing = condition != null;
        mode = condition == null ? ItemCondition.Mode.REQUIRE : condition.mode;
        hand = condition == null ? ItemCondition.Hand.MAIN : condition.hand;
        value = condition == null ? "" : condition.selector;
    }

    @Override
    protected void init() {
        editorWidth = Math.min(300, width - 2 * PAD - 16);
        left = (width - editorWidth) / 2;
        top = Math.max(4, (height - PANEL_HEIGHT) / 2);
        catalog = Compat.get().itemSelectors();
        int half = (editorWidth - 6) / 2;

        addRenderableWidget(Button.builder(Component.literal("?"), button -> minecraft.setScreen(new GuideScreen(this, GuideScreen.ITEM_PAGE)))
                .bounds(left + editorWidth - 16, top + 6, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.keybind_priority.guide.open"))).build());

        selector = new EditBox(font, left + 24, top + 48, editorWidth - 24, 20, Component.translatable(LANG + "selector"));
        selector.setMaxLength(1024);
        selector.setValue(value);
        selector.setCursorPosition(0);
        selector.setHint(Component.literal("#minecraft:pickaxes, @mekanism"));
        selector.setResponder(text -> {
            value = text;
            refreshFilter();
        });
        addRenderableWidget(selector);

        suggestion = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (suggestions.isEmpty()) return;
            int start = value.lastIndexOf(',') + 1;
            boolean excluded = value.substring(start).trim().startsWith("!");
            selector.setValue(value.substring(0, start) + (start > 0 ? " " : "")
                    + (excluded ? "!" : "") + suggestions.get(suggestionIndex));
            setFocused(selector);
        }).bounds(left, top + 72, editorWidth - 24, 20).build());
        nextSuggestion = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            suggestionIndex = (suggestionIndex + 1) % suggestions.size();
            updateSuggestion();
        }).bounds(left + editorWidth - 20, top + 72, 20, 20)
                .tooltip(Tooltip.create(Component.translatable(LANG + "next_suggestion"))).build());

        addRenderableWidget(Button.builder(modeLabel(), button -> {
            mode = mode == ItemCondition.Mode.REQUIRE ? ItemCondition.Mode.PREFER : ItemCondition.Mode.REQUIRE;
            button.setMessage(modeLabel());
        }).bounds(left, top + 98, half, 20).build());
        addRenderableWidget(Button.builder(handLabel(), button -> {
            hand = ItemCondition.Hand.values()[(hand.ordinal() + 1) % ItemCondition.Hand.values().length];
            button.setMessage(handLabel());
            held.active = !heldStack().isEmpty();
        }).bounds(left + half + 6, top + 98, editorWidth - half - 6, 20).build());

        held = addRenderableWidget(Button.builder(Component.translatable(LANG + "held"), button -> {
            ItemStack stack = heldStack();
            if (!stack.isEmpty()) selector.setValue(Compat.get().itemId(stack));
        }).bounds(left, top + 174, half, 20).build());
        held.active = !heldStack().isEmpty();
        Button clear = addRenderableWidget(Button.builder(Component.translatable(LANG + "clear"), button -> {
            parent.setItemCondition(binding, null);
            onClose();
        }).bounds(left + half + 6, top + 174, editorWidth - half - 6, 20).build());
        clear.active = existing;

        save = addRenderableWidget(Button.builder(Component.translatable(LANG + "save"), button -> {
            parent.setItemCondition(binding, new ItemCondition(value, mode, hand));
            onClose();
        }).bounds(left, top + 200, half, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(left + half + 6, top + 200, editorWidth - half - 6, 20).build());

        refreshFilter();
        setInitialFocus(selector);
    }

    private void refreshFilter() {
        filter = ItemFilter.parse(value);
        boolean valid = filter.error() == null && !value.isBlank();
        if (save != null) save.active = valid;
        example = valid ? filter.example() : ItemStack.EMPTY;
        suggestions = ItemFilter.suggestions(value, catalog);
        suggestionIndex = 0;
        updateSuggestion();
    }

    private void updateSuggestion() {
        if (suggestion == null || nextSuggestion == null) return;
        boolean available = !suggestions.isEmpty();
        suggestion.active = available;
        nextSuggestion.active = suggestions.size() > 1;
        suggestion.setMessage(available ? Component.literal(font.plainSubstrByWidth(suggestions.get(suggestionIndex), editorWidth - 40))
                : Component.translatable(LANG + "suggestions"));
        suggestion.setTooltip(available ? Tooltip.create(Component.literal(suggestions.get(suggestionIndex))) : null);
    }

    private ItemStack heldStack() {
        if (minecraft.player == null) return ItemStack.EMPTY;
        if (hand == ItemCondition.Hand.OFF) return minecraft.player.getOffhandItem();
        ItemStack main = minecraft.player.getMainHandItem();
        return hand == ItemCondition.Hand.EITHER && main.isEmpty() ? minecraft.player.getOffhandItem() : main;
    }

    private Component modeLabel() {
        return Component.translatable(LANG + "mode.short." + mode.name().toLowerCase(Locale.ROOT));
    }

    private Component handLabel() {
        return Component.translatable(LANG + "hand.short." + hand.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        Ui.panel(graphics, left - PAD, top, left + editorWidth + PAD, top + PANEL_HEIGHT);
        // Icon slot next to the filter field.
        graphics.fill(left, top + 48, left + 20, top + 68, 0xFF000000);
        graphics.renderOutline(left, top + 48, 20, 20, 0xFFA0A0A0);
        // Divider above the action buttons.
        graphics.fill(left, top + 168, left + editorWidth, top + 169, 0xFF3C3C48);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, top + 9, Ui.WHITE);
        String name = font.plainSubstrByWidth(Component.translatable(binding).getString(), editorWidth - 48);
        graphics.drawCenteredString(font, name, width / 2, top + 21, Ui.AQUA);
        Compat.get().drawText(graphics, font, Component.translatable(LANG + "selector").getString(), left, top + 37, Ui.MUTED);
        if (!example.isEmpty()) graphics.renderItem(example, left + 2, top + 50);

        // What the chosen mode does, plus a warning when it can't have any effect.
        Component explain = Component.translatable(LANG + "mode.explain." + mode.name().toLowerCase(Locale.ROOT));
        graphics.drawWordWrap(font, explain, left, top + 124, editorWidth, Ui.MUTED);
        if (mode == ItemCondition.Mode.PREFER && !parent.exclusive()) {
            int y = top + 124 + font.split(explain, editorWidth).size() * 9;
            graphics.drawWordWrap(font, Component.translatable(LANG + "mode.needs_exclusive"), left, y, editorWidth, Ui.ORANGE);
        }

        // Live check against what is in hand right now.
        int statusY = top + 152;
        if (value.isBlank()) {
            Compat.get().drawText(graphics, font, font.plainSubstrByWidth(Component.translatable(LANG + "empty_filter").getString(), editorWidth),
                    left, statusY + 4, Ui.DIM);
        } else if (filter.error() != null) {
            Compat.get().drawText(graphics, font, font.plainSubstrByWidth(Component.translatable(LANG + "invalid_filter", filter.error()).getString(), editorWidth),
                    left, statusY + 4, Ui.RED);
        } else {
            preview.selector = value;
            preview.hand = hand;
            boolean matches = preview.matches();
            ItemStack inHand = heldStack();
            int textLeft = left;
            if (!inHand.isEmpty()) {
                graphics.renderItem(inHand, left, statusY);
                textLeft += 20;
            }
            Component status = Component.translatable(LANG + (matches ? "match" : "no_match"));
            Compat.get().drawText(graphics, font, font.plainSubstrByWidth(status.getString(), editorWidth - (textLeft - left)),
                    textLeft, statusY + 4, matches ? Ui.GREEN : Ui.MUTED);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
