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
    private static final int LIST_TOP = 56;

    private final InputConstants.Key key;
    private final List<KeyMapping> bindings = new ArrayList<>();
    private final List<String> disabled = new ArrayList<>();
    private boolean exclusive;
    private BindingList list;

    public PriorityScreen(InputConstants.Key key) {
        super(Component.translatable(LANG + "title", key.getDisplayName()));
        this.key = key;

        PriorityConfig.KeyRule rule = PriorityConfig.rule(key.getName());
        List<String> order = rule == null ? List.of() : rule.order;
        if (rule != null) {
            exclusive = rule.exclusive;
            disabled.addAll(rule.disabled);
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
        }).bounds(width / 2 - 120, 30, 240, 20).build());

        list = addRenderableWidget(new BindingList(minecraft, width, height - LIST_TOP - 32, LIST_TOP));
        list.rebuild();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, 20).build());
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
    }

    private void move(int index, int delta) {
        int target = index + delta;
        if (target < 0 || target >= bindings.size()) return;
        bindings.add(target, bindings.remove(index));
        apply();
        list.rebuild();
    }

    private void moveToTop(int index) {
        move(index, -index);
    }

    private void toggle(KeyMapping mapping) {
        if (!disabled.remove(mapping.getName())) disabled.add(mapping.getName());
        apply();
        list.rebuild();
    }

    /** Index of the binding that wins in exclusive mode (ignoring context), or -1. */
    private int topEnabled() {
        for (int i = 0; i < bindings.size(); i++) {
            if (!disabled.contains(bindings.get(i).getName())) return i;
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(LANG + "hint"), width / 2, 18, 0xA0A0A0);
        if (bindings.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable(LANG + "empty"), width / 2, LIST_TOP + 20, 0xA0A0A0);
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

    private final class BindingList extends ContainerObjectSelectionList<Row> {
        BindingList(Minecraft minecraft, int width, int height, int top) {
            super(minecraft, width, height, top, ROW_HEIGHT);
        }

        void rebuild() {
            double scroll = getScrollAmount();
            List<Row> rows = new ArrayList<>();
            for (int i = 0; i < bindings.size(); i++) rows.add(new Row(i));
            replaceEntries(rows);
            setScrollAmount(scroll);
        }

        @Override
        public int getRowWidth() {
            return Math.min(340, width - 24);
        }
    }

    private final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final int index;
        private final KeyMapping mapping;
        private final Button top;
        private final Button up;
        private final Button down;
        private final Button onOff;

        Row(int index) {
            this.index = index;
            this.mapping = bindings.get(index);
            boolean off = disabled.contains(mapping.getName());
            top = Button.builder(Component.literal("⤒"), b -> moveToTop(index)).size(18, 20)
                    .tooltip(Tooltip.create(Component.translatable(LANG + "to_top"))).build();
            up = Button.builder(Component.literal("▲"), b -> move(index, -1)).size(18, 20).build();
            down = Button.builder(Component.literal("▼"), b -> move(index, 1)).size(18, 20).build();
            onOff = Button.builder(Component.translatable(LANG + (off ? "off" : "on"))
                    .withStyle(off ? ChatFormatting.RED : ChatFormatting.GREEN), b -> toggle(mapping)).size(36, 20).build();
            top.active = index > 0;
            up.active = index > 0;
            down.active = index < bindings.size() - 1;
        }

        @Override
        public void render(GuiGraphics graphics, int idx, int top, int left, int rowWidth, int rowHeight,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            boolean off = disabled.contains(mapping.getName());
            int winner = exclusive ? topEnabled() : -1;
            boolean suppressed = off || (exclusive && index != winner);

            String rank = (index + 1) + ".";
            graphics.drawString(font, rank, left, top + 3, 0x808080);
            int textLeft = left + 16;

            MutableComponent name = Component.translatable(mapping.getName());
            if (index == winner) name = Component.literal("★ ").append(name);

            MutableComponent info = Component.translatable(mapping.getCategory()).copy()
                    .append(" · ").append(mapping.getTranslatedKeyMessage());
            String context = contextName(mapping.getKeyConflictContext());
            if (context != null) info.append(" · ").append(Component.translatable(LANG + "context." + context));
            if (suppressed) {
                info.append(" · ").append(Component.translatable(LANG + (off ? "state.off" : "state.suppressed")));
            }
            int buttonsLeft = left + rowWidth - 95;
            int maxText = buttonsLeft - textLeft - 4;
            graphics.drawString(font, font.plainSubstrByWidth(name.getString(), maxText), textLeft, top + 3,
                    suppressed ? 0x808080 : 0xFFFFFF);
            graphics.drawString(font, font.plainSubstrByWidth(info.getString(), maxText), textLeft, top + 13, 0x707070);

            this.top.setPosition(buttonsLeft, top);
            up.setPosition(buttonsLeft + 19, top);
            down.setPosition(buttonsLeft + 38, top);
            onOff.setPosition(buttonsLeft + 58, top);
            this.top.render(graphics, mouseX, mouseY, partialTick);
            up.render(graphics, mouseX, mouseY, partialTick);
            down.render(graphics, mouseX, mouseY, partialTick);
            onOff.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(top, up, down, onOff);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(top, up, down, onOff);
        }
    }
}
