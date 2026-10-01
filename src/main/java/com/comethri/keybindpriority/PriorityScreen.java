package com.comethri.keybindpriority;

import com.comethri.keybindpriority.mixin.KeyMappingAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lists every binding on one key; order = priority, plus a mode toggle and per-binding on/off. */
public final class PriorityScreen extends Screen {
    private static final String LANG = "screen." + KeybindPriority.MOD_ID + ".";
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_TOP = 62;
    private static final int BUTTONS_WIDTH = 137;

    private final InputConstants.Key key;
    private final List<KeyMapping> bindings = new ArrayList<>();
    private final List<String> disabled = new ArrayList<>();
    private final Map<String, ItemCondition> items = new HashMap<>();
    private boolean exclusive;
    private boolean guideChecked;

    public PriorityScreen(InputConstants.Key key) {
        super(Component.translatable(LANG + "title", key.getDisplayName()));
        this.key = key;

        PriorityConfig.KeyRule rule = PriorityConfig.rule(key.getName());
        List<String> order = rule == null ? List.of() : rule.order;
        if (rule != null) {
            exclusive = rule.exclusive;
            disabled.addAll(rule.disabled);
            items.putAll(rule.items);
        }

        List<KeyMapping> onKey = new ArrayList<>();
        for (KeyMapping mapping : KeyMappingAccessor.keybindPriority$all().values()) {
            if (mapping.getKey().equals(key)) onKey.add(mapping);
        }
        // Known order first, bindings we have not ranked yet below it in the vanilla controls order.
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < order.size(); i++) rank.putIfAbsent(order.get(i), i);
        onKey.sort((a, b) -> {
            int ra = rank.getOrDefault(a.getName(), Integer.MAX_VALUE);
            int rb = rank.getOrDefault(b.getName(), Integer.MAX_VALUE);
            return ra != rb ? Integer.compare(ra, rb) : a.compareTo(b);
        });
        bindings.addAll(onKey);
        disabled.retainAll(onKey.stream().map(KeyMapping::getName).toList());
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(modeLabel(), b -> {
            exclusive = !exclusive;
            b.setMessage(modeLabel());
            apply();
        }).bounds(width / 2 - 110, 36, 220, 20)
                .tooltip(Tooltip.create(Component.translatable(LANG + "mode.tooltip"))).build());

        addRenderableWidget(Button.builder(Component.literal("?"), b -> openGuide(0))
                .bounds(width - 26, 6, 20, 20)
                .tooltip(Tooltip.create(Component.translatable(LANG + "guide.open"))).build());

        addRenderableWidget(new BindingList(minecraft, width, height - LIST_TOP - 32, LIST_TOP));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        // Opened from tick, not init: init also runs on resize and while the screen is being set.
        if (!guideChecked) {
            guideChecked = true;
            if (!PriorityConfig.guideSeen()) openGuide(0);
        }
    }

    void openGuide(int page) {
        minecraft.setScreen(new GuideScreen(this, page));
    }

    boolean exclusive() {
        return exclusive;
    }

    private Component modeLabel() {
        return Component.translatable(LANG + (exclusive ? "mode.exclusive" : "mode.all"));
    }

    /** Writes the current state into the config right away, so it takes effect before the file is saved. */
    private void apply() {
        if (bindings.isEmpty()) {
            PriorityConfig.removeRule(key.getName());
            return;
        }
        PriorityConfig.KeyRule rule = PriorityConfig.ruleOrCreate(key.getName());
        rule.exclusive = exclusive;
        rule.order = new ArrayList<>(bindings.stream().map(KeyMapping::getName).toList());
        rule.disabled.clear();
        rule.disabled.addAll(disabled);
        rule.items.clear();
        rule.items.putAll(items);
    }

    void setItemCondition(String binding, ItemCondition condition) {
        if (condition == null) items.remove(binding);
        else items.put(binding, condition);
        apply();
        PriorityConfig.save();
    }

    private void move(int index, int target) {
        if (index == target || target < 0 || target >= bindings.size()) return;
        bindings.add(target, bindings.remove(index));
        apply();
    }

    private void toggle(int index) {
        String name = bindings.get(index).getName();
        if (!disabled.remove(name)) disabled.add(name);
        apply();
    }

    /** Preview item conditions while the editor is open; binding contexts apply once back in game. */
    private int topEnabled() {
        int fallback = -1;
        for (int i = 0; i < bindings.size(); i++) {
            String name = bindings.get(i).getName();
            if (disabled.contains(name)) continue;
            ItemCondition condition = items.get(name);
            boolean matches = condition != null && condition.matches();
            if (condition != null && condition.mode == ItemCondition.Mode.REQUIRE && !matches) continue;
            if (matches && condition.mode == ItemCondition.Mode.PREFER) return i;
            if (fallback < 0) fallback = i;
        }
        return fallback;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // "Keybind Priority [ V ]", centered as one piece.
        String heading = Component.translatable(LANG + "heading").getString();
        String keyName = key.getDisplayName().getString();
        int headingWidth = font.width(heading) + 6 + Ui.keyWidth(font, keyName);
        int x = (width - headingWidth) / 2;
        Compat.get().drawText(graphics, font, heading, x, 10, Ui.WHITE);
        Ui.key(graphics, font, keyName, x + font.width(heading) + 6, 6);

        graphics.drawCenteredString(font, Component.translatable(LANG + "hint", bindings.size()), width / 2, 24, Ui.MUTED);
        if (bindings.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable(LANG + "empty"), width / 2, LIST_TOP + 20, Ui.MUTED);
        }
    }

    @Override
    public void removed() {
        apply();
        PriorityConfig.save();
    }

    private static String contextName(IKeyConflictContext context) {
        if (context == KeyConflictContext.IN_GAME) return null;
        if (context instanceof KeyConflictContext known) return known.name().toLowerCase(Locale.ROOT);
        return "custom";
    }

    // Raw type: the entry class comes from the compat layer of the running Minecraft version.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private final class BindingList extends ContainerObjectSelectionList {
        BindingList(Minecraft minecraft, int width, int height, int top) {
            super(minecraft, width, height, top, ROW_HEIGHT);
            // One row per position; each row shows whatever binding currently sits there,
            // so reordering never rebuilds the list and the scroll position stays put.
            for (int i = 0; i < bindings.size(); i++) addEntry(Compat.get().newRow(new Row(i)));
        }

        @Override
        public int getRowWidth() {
            return Math.min(460, width - 24);
        }
    }

    private final class Row implements RowContent {
        private final int index;
        private final Button top;
        private final Button up;
        private final Button down;
        private final Button onOff;
        private final Button item;

        Row(int index) {
            this.index = index;
            top = Button.builder(Component.literal("⤒"), b -> move(index, 0)).size(18, 20)
                    .tooltip(Tooltip.create(Component.translatable(LANG + "to_top"))).build();
            up = Button.builder(Component.literal("▲"), b -> move(index, index - 1)).size(18, 20).build();
            down = Button.builder(Component.literal("▼"), b -> move(index, index + 1)).size(18, 20).build();
            onOff = Button.builder(Component.empty(), b -> toggle(index)).size(36, 20).build();
            item = Button.builder(Component.translatable(LANG + "item"), b -> {
                String name = bindings.get(index).getName();
                minecraft.setScreen(new ItemConditionScreen(PriorityScreen.this, name, items.get(name)));
            }).size(40, 20).build();
        }

        @Override
        public void render(GuiGraphics graphics, int left, int rowTop, int rowWidth,
                           int mouseX, int mouseY, float partialTick) {
            KeyMapping mapping = bindings.get(index);
            boolean off = disabled.contains(mapping.getName());
            ItemCondition condition = items.get(mapping.getName());
            boolean itemMatches = condition != null && condition.matches();
            boolean itemInactive = condition != null && condition.mode == ItemCondition.Mode.REQUIRE && !itemMatches;
            int winner = exclusive ? topEnabled() : -1;
            boolean suppressed = off || itemInactive || (exclusive && index != winner);

            String state;
            int stateColor;
            int accent;
            if (off) {
                state = "state.off";
                stateColor = accent = Ui.RED;
            } else if (itemInactive) {
                state = "state.needs_item";
                stateColor = Ui.ORANGE;
                accent = 0;
            } else if (!exclusive) {
                state = "state.active";
                stateColor = accent = Ui.GREEN;
            } else if (index == winner) {
                boolean preferred = itemMatches && condition.mode == ItemCondition.Mode.PREFER;
                state = preferred ? "state.preferred" : "state.active";
                stateColor = accent = preferred ? Ui.AQUA : Ui.GOLD;
            } else {
                state = "state.backup";
                stateColor = Ui.DIM;
                accent = 0;
            }

            int cardLeft = left - 3;
            int cardRight = left + rowWidth + 1;
            boolean hovered = mouseX >= cardLeft && mouseX < cardRight && mouseY >= rowTop - 1 && mouseY < rowTop + 21;
            Ui.card(graphics, cardLeft, rowTop - 1, cardRight, rowTop + 21, accent, hovered);

            Compat.get().drawText(graphics, font, (index + 1) + ".", left + 2, rowTop + 6,
                    index == winner ? Ui.GOLD : Ui.DIM);
            int textLeft = left + 18;
            int buttonsLeft = left + rowWidth - BUTTONS_WIDTH;

            String stateText = Component.translatable(LANG + state).getString();
            int chipLeft = Ui.chip(graphics, font, stateText, buttonsLeft - 5, rowTop + 1, stateColor);

            int nameLeft = textLeft;
            if (index == winner) {
                Compat.get().drawText(graphics, font, "★", textLeft, rowTop + 2, Ui.GOLD);
                nameLeft += font.width("★ ");
            }
            String name = Component.translatable(mapping.getName()).getString();
            Compat.get().drawText(graphics, font, font.plainSubstrByWidth(name, chipLeft - nameLeft - 4), nameLeft, rowTop + 2,
                    suppressed ? Ui.MUTED : Ui.WHITE);

            MutableComponent info = Compat.get().categoryLabel(mapping).copy()
                    .append(" · ").append(mapping.getTranslatedKeyMessage());
            String context = contextName(mapping.getKeyConflictContext());
            if (context != null) info.append(" · ").append(Component.translatable(LANG + "context." + context));
            Compat.get().drawText(graphics, font, font.plainSubstrByWidth(info.getString(), buttonsLeft - textLeft - 5),
                    textLeft, rowTop + 12, Ui.DIM);

            top.active = index > 0;
            up.active = index > 0;
            down.active = index < bindings.size() - 1;
            onOff.setMessage(Component.translatable(LANG + (off ? "off" : "on"))
                    .withStyle(off ? ChatFormatting.RED : ChatFormatting.GREEN));
            onOff.setTooltip(Tooltip.create(Component.translatable(LANG + (off ? "off.tooltip" : "on.tooltip"))));

            ItemStack icon = condition == null ? ItemStack.EMPTY : condition.icon();
            item.setMessage(!icon.isEmpty() ? Component.empty() : Component.translatable(LANG + "item")
                    .withStyle(condition == null ? ChatFormatting.GRAY : ChatFormatting.AQUA));
            item.setTooltip(Tooltip.create(condition == null ? Component.translatable(LANG + "item.none")
                    : Component.translatable(LANG + "item.summary",
                    Component.translatable(LANG + "item.mode." + condition.mode.name().toLowerCase(Locale.ROOT)),
                    condition.selector,
                    Component.translatable(LANG + "item.hand." + condition.hand.name().toLowerCase(Locale.ROOT)))));

            top.setPosition(buttonsLeft, rowTop);
            up.setPosition(buttonsLeft + 19, rowTop);
            down.setPosition(buttonsLeft + 38, rowTop);
            onOff.setPosition(buttonsLeft + 58, rowTop);
            item.setPosition(buttonsLeft + 97, rowTop);
            top.render(graphics, mouseX, mouseY, partialTick);
            up.render(graphics, mouseX, mouseY, partialTick);
            down.render(graphics, mouseX, mouseY, partialTick);
            onOff.render(graphics, mouseX, mouseY, partialTick);
            item.render(graphics, mouseX, mouseY, partialTick);
            if (!icon.isEmpty()) graphics.renderItem(icon, buttonsLeft + 109, rowTop + 2);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(top, up, down, onOff, item);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(top, up, down, onOff, item);
        }
    }
}
