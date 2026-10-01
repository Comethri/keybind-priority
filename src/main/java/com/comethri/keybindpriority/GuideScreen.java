package com.comethri.keybindpriority;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A few pages explaining the menu. Opens by itself the first time, afterwards from the "?" buttons. */
public final class GuideScreen extends Screen {
    private static final String LANG = "screen." + KeybindPriority.MOD_ID + ".guide.";
    static final int PAGES = 5;
    /** The page about held-item conditions, opened from the item editor. */
    static final int ITEM_PAGE = 3;
    private static final int PANEL_HEIGHT = 206;

    private final Screen parent;
    private int page;
    private int left;
    private int top;
    private int panelWidth;
    private Button back;
    private Button next;
    private Button skip;

    public GuideScreen(Screen parent, int page) {
        super(Component.translatable(LANG + "title"));
        this.parent = parent;
        this.page = Math.max(0, Math.min(PAGES - 1, page));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(320, width - 24);
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - PANEL_HEIGHT) / 2);
        int buttonY = top + PANEL_HEIGHT - 28;
        skip = addRenderableWidget(Button.builder(Component.translatable(LANG + "skip"), b -> onClose())
                .bounds(left + 8, buttonY, 60, 20).build());
        back = addRenderableWidget(Button.builder(Component.translatable(LANG + "back"), b -> turn(-1))
                .bounds(left + panelWidth - 148, buttonY, 66, 20).build());
        next = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (page == PAGES - 1) onClose();
            else turn(1);
        }).bounds(left + panelWidth - 78, buttonY, 70, 20).build());
        updateButtons();
    }

    void turn(int delta) {
        page = Math.max(0, Math.min(PAGES - 1, page + delta));
        updateButtons();
    }

    int page() {
        return page;
    }

    private void updateButtons() {
        if (next == null) return;
        back.visible = page > 0;
        skip.visible = page < PAGES - 1;
        next.setMessage(Component.translatable(LANG + (page == PAGES - 1 ? "done" : "next")));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        Ui.panel(graphics, left, top, left + panelWidth, top + PANEL_HEIGHT);
        drawIllustration(graphics, left + 16, top + 30, panelWidth - 32);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Compat.get().drawText(graphics, font, Component.translatable(LANG + "step", page + 1, PAGES).getString(),
                left + 10, top + 10, Ui.DIM);
        graphics.drawCenteredString(font, Component.translatable(LANG + page + ".title"), width / 2, top + 10, Ui.GOLD);
        graphics.drawWordWrap(font, Component.translatable(LANG + page + ".text"), left + 14, top + 98,
                panelWidth - 28, Ui.TEXT);

        // Page dots.
        int dotsLeft = width / 2 - (PAGES * 8 - 4) / 2;
        int dotsY = top + PANEL_HEIGHT - 40;
        for (int i = 0; i < PAGES; i++) {
            int x = dotsLeft + i * 8;
            graphics.fill(x, dotsY, x + 4, dotsY + 4, i == page ? Ui.GOLD : 0xFF4A4A55);
        }
    }

    /** Little mock-ups of the menu, drawn with the same pieces the real menu uses. */
    private void drawIllustration(GuiGraphics graphics, int x, int y, int w) {
        String vein = Component.translatable(LANG + "example.vein").getString();
        String voice = Component.translatable(LANG + "example.voice").getString();
        String zoom = Component.translatable(LANG + "example.zoom").getString();
        switch (page) {
            case 0 -> {
                Ui.key(graphics, font, "V", x, y + 21);
                int rows = x + 34;
                int line = Ui.withAlpha(Ui.GOLD, 0x90);
                int fork = x + 25;
                graphics.fill(x + Ui.keyWidth(font, "V"), y + 28, fork, y + 29, line);
                graphics.fill(fork, y + 8, fork + 1, y + 49, line);
                for (int i = 0; i < 3; i++) graphics.fill(fork, y + 8 + i * 20, rows - 1, y + 9 + i * 20, line);
                mockRow(graphics, rows, y, w - 34, 1, vein, null, 0, 0, false);
                mockRow(graphics, rows, y + 20, w - 34, 2, voice, null, 0, 0, false);
                mockRow(graphics, rows, y + 40, w - 34, 3, zoom, null, 0, 0, false);
            }
            case 1 -> {
                mockRow(graphics, x, y, w, 1, vein, "state.active", Ui.GOLD, Ui.GOLD, true);
                mockRow(graphics, x, y + 20, w, 2, voice, "state.backup", Ui.DIM, 0, false);
                mockRow(graphics, x, y + 40, w, 3, zoom, "state.backup", Ui.DIM, 0, false);
            }
            case 2 -> {
                mockRow(graphics, x, y + 4, w, 1, vein, "state.active", Ui.GOLD, Ui.GOLD, true);
                mockRow(graphics, x, y + 24, w, 2, voice, "state.off", Ui.RED, Ui.RED, false);
                int bx = x + w - 112;
                mockButton(graphics, bx, y + 46, 18, "⤒", Ui.TEXT);
                mockButton(graphics, bx + 20, y + 46, 18, "▲", Ui.TEXT);
                mockButton(graphics, bx + 40, y + 46, 18, "▼", Ui.TEXT);
                mockButton(graphics, bx + 62, y + 46, 22, Component.translatable("screen.keybind_priority.on").getString(), Ui.GREEN);
                mockButton(graphics, bx + 86, y + 46, 26, Component.translatable("screen.keybind_priority.off").getString(), Ui.RED);
            }
            case 3 -> {
                mockRow(graphics, x, y + 14, w, 1, vein, "state.preferred", Ui.AQUA, Ui.AQUA, true);
                mockRow(graphics, x, y + 34, w, 2, voice, "state.backup", Ui.DIM, 0, false);
                int ix = x + w - 24;
                mockButton(graphics, ix, y + 14, 22, "", Ui.TEXT);
                graphics.renderItem(new ItemStack(Items.DIAMOND_PICKAXE), ix + 3, y + 15);
            }
            default -> {
                String ctrl = Component.translatable(LANG + "key.ctrl").getString();
                int kx = x + (w - (Ui.keyWidth(font, ctrl) + Ui.keyWidth(font, "Alt") + Ui.keyWidth(font, "V") + 28)) / 2;
                Ui.key(graphics, font, ctrl, kx, y + 14);
                kx += Ui.keyWidth(font, ctrl) + 4;
                Compat.get().drawText(graphics, font, "+", kx, y + 17, Ui.MUTED);
                kx += 10;
                Ui.key(graphics, font, "Alt", kx, y + 14);
                kx += Ui.keyWidth(font, "Alt") + 4;
                Compat.get().drawText(graphics, font, "+", kx, y + 17, Ui.MUTED);
                kx += 10;
                Ui.key(graphics, font, "V", kx, y + 14);
                graphics.drawCenteredString(font, Component.translatable(LANG + "4.caption"), x + w / 2, y + 40, Ui.MUTED);
            }
        }
    }

    private void mockRow(GuiGraphics graphics, int x, int y, int w, int rank, String name,
                         String state, int stateColor, int accent, boolean winner) {
        Ui.card(graphics, x, y, x + w, y + 17, accent, false);
        Compat.get().drawText(graphics, font, rank + ".", x + 5, y + 5, winner ? Ui.GOLD : Ui.DIM);
        int textLeft = x + 18;
        if (winner) {
            Compat.get().drawText(graphics, font, "★", textLeft, y + 5, Ui.GOLD);
            textLeft += font.width("★ ");
        }
        Compat.get().drawText(graphics, font, name, textLeft, y + 5, state == null || winner ? Ui.WHITE : Ui.MUTED);
        if (state != null) {
            int right = x + w - (page == ITEM_PAGE ? 28 : 4);
            Ui.chip(graphics, font, Component.translatable("screen.keybind_priority." + state).getString(), right, y + 3, stateColor);
        }
    }

    private void mockButton(GuiGraphics graphics, int x, int y, int w, String label, int color) {
        graphics.fill(x, y, x + w, y + 18, 0xFF5A5A5A);
        Ui.outline(graphics, x, y, w, 18, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF8A8A8A);
        if (!label.isEmpty()) graphics.drawCenteredString(font, label, x + w / 2, y + 5, color);
    }

    @Override
    public void onClose() {
        if (!PriorityConfig.guideSeen()) {
            PriorityConfig.setGuideSeen(true);
            PriorityConfig.save();
        }
        minecraft.setScreen(parent);
    }
}
