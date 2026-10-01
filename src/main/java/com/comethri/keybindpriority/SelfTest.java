package com.comethri.keybindpriority;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game check for CI, only active with {@code -Dkeybindpriority.selftest=true}. Once the game has loaded it
 * puts two bindings on an unused key, checks gameplay resolution and unfiltered menu input, opens the menu for a few
 * frames and quits. The log line {@code KEYBIND_PRIORITY_SELFTEST: PASS} or {@code FAIL} is the result.
 */
public final class SelfTest {
    public static final String PROPERTY = "keybindpriority.selftest";
    private static final int TEST_KEY = GLFW.GLFW_KEY_F25;
    private static final int MENU_TICKS = 160;
    /** CI takes a screenshot of the display once this line shows up in the log. */
    private static final int MENU_READY_TICK = 40;

    private static final List<String> failures = new ArrayList<>();
    private static int stage;
    private static int ticks;

    private SelfTest() {
    }

    public static void register() {
        KeybindPriority.LOG.info("Self test enabled");
        NeoForge.EVENT_BUS.addListener(SelfTest::onTick);
    }

    private static InputConstants.Key key() {
        return InputConstants.Type.KEYSYM.getOrCreate(TEST_KEY);
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (stage == 0) {
                // Loading is done once the overlay is gone and the first screen (title/onboarding) is up.
                if (minecraft.getOverlay() != null || minecraft.screen == null) return;
                runKeyTests();
                PriorityConfig.setGuideSeen(false);
                minecraft.setScreen(new PriorityScreen(key()));
                stage = 1;
                return;
            }
            if (stage == 1) ticks++;
            if (stage == 1 && ticks == 6) {
                check(minecraft.screen instanceof GuideScreen, "guide must open the first time the menu opens");
                grab(minecraft, "guide");
            } else if (stage == 1 && ticks == 8 && minecraft.screen instanceof GuideScreen guide) {
                guide.turn(GuideScreen.ITEM_PAGE);
                check(guide.page() == GuideScreen.ITEM_PAGE, "guide must turn pages");
            } else if (stage == 1 && ticks == 12) {
                grab(minecraft, "guide item page");
            } else if (stage == 1 && ticks == 14 && minecraft.screen != null) {
                minecraft.screen.onClose();
            } else if (stage == 1 && ticks == 30) {
                check(minecraft.screen instanceof PriorityScreen, "closing the guide must return to the menu");
                check(PriorityConfig.guideSeen(), "closing the guide must remember it");
            } else if (stage == 1 && ticks == MENU_READY_TICK) {
                KeybindPriority.LOG.info("KEYBIND_PRIORITY_SELFTEST: MENU_OPEN");
                Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(), message -> KeybindPriority.LOG.info("Self test screenshot: {}", message.getString()));
            } else if (stage == 1 && ticks == 80) {
                check(minecraft.screen instanceof PriorityScreen, "priority menu missing before editor test");
                PriorityScreen parent = (PriorityScreen) minecraft.screen;
                minecraft.setScreen(new ItemConditionScreen(parent, "key.keybind_priority.selftest.b",
                        new ItemCondition("@mine", ItemCondition.Mode.PREFER, ItemCondition.Hand.MAIN)));
            } else if (stage == 1 && ticks == 100) {
                check(minecraft.screen instanceof ItemConditionScreen, "item editor missing");
                Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(), message -> KeybindPriority.LOG.info("Self test editor screenshot: {}", message.getString()));
            } else if (stage == 1 && ticks == 110) {
                minecraft.screen.onClose();
                PriorityScreen parent = (PriorityScreen) minecraft.screen;
                minecraft.setScreen(new ItemConditionScreen(parent, "key.keybind_priority.selftest.b",
                        new ItemCondition("@minecraft, !minecraft:stick", ItemCondition.Mode.PREFER, ItemCondition.Hand.EITHER)));
            } else if (stage == 1 && ticks == 130) {
                Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(), message -> KeybindPriority.LOG.info("Self test valid filter screenshot: {}", message.getString()));
            } else if (stage == 1 && ticks == 140) {
                minecraft.screen.onClose();
            } else if (stage == 1 && ticks >= MENU_TICKS) {
                check(minecraft.screen instanceof PriorityScreen, "menu closed by itself");
                minecraft.setScreen(null);
                stage = 2;
                finish(minecraft);
            }
        } catch (Throwable t) {
            KeybindPriority.LOG.error("Self test crashed", t);
            failures.add("exception: " + t);
            stage = 2;
            finish(minecraft);
        }
    }

    private static void grab(Minecraft minecraft, String what) {
        Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(),
                message -> KeybindPriority.LOG.info("Self test {} screenshot: {}", what, message.getString()));
    }

    private static void runKeyTests() {
        InputConstants.Key key = key();
        PriorityConfig.removeRule(key.getName());
        KeyMapping a = Compat.get().newTestMapping("key.keybind_priority.selftest.a", key);
        KeyMapping b = Compat.get().newTestMapping("key.keybind_priority.selftest.b", key);

        KeyMapping.click(key);
        expectClicks("no rule", a, 1, b, 1);

        PriorityConfig.KeyRule rule = PriorityConfig.ruleOrCreate(key.getName());
        rule.exclusive = true;
        rule.order = new ArrayList<>(List.of(b.getName(), a.getName()));
        expectFiltered("top binding only", a, b, List.of(b));

        KeyMapping.set(key, true);
        check(a.isDown() && b.isDown(), "menus: held keys must remain unfiltered");
        check(KeyFilter.isSuppressed(a, true) && !KeyFilter.isSuppressed(b, true), "world: only the top binding is allowed");
        KeyMapping.set(key, false);
        check(!a.isDown() && !b.isDown(), "release: nothing should stay down");

        check(Compat.get().matchesKeysym(b, TEST_KEY) && Compat.get().matchesKeysym(a, TEST_KEY), "menus: both bindings must still match");
        check(!KeyFilter.inGame(), "title screen must not count as gameplay");
        check(KeyFilter.filter(List.of(a, b)).equals(List.of(a, b)), "menus: priority must not suppress bindings");

        rule.disabled.add(b.getName());
        expectFiltered("top one off, next takes over", a, b, List.of(a));
        KeyMapping.click(key);
        expectClicks("menus: disabled bindings must still receive clicks", a, 1, b, 1);

        rule.exclusive = false;
        rule.disabled.clear();
        KeyMapping.click(key);
        expectClicks("all bindings", a, 1, b, 1);

        runItemTests(rule, a, b);
        runFilterTests();

        // Leave a ranked rule behind so the menu screenshot shows a winner and an item condition with its icon.
        rule.exclusive = true;
        rule.items.put(b.getName(), new ItemCondition("minecraft:diamond_pickaxe", ItemCondition.Mode.REQUIRE, ItemCondition.Hand.MAIN));
        runConfigTests();
    }

    private static void runConfigTests() {
        String name = "key.keyboard.keybind_priority_config_selftest";
        PriorityConfig.KeyRule rule = PriorityConfig.ruleOrCreate(name);
        rule.exclusive = true;
        rule.order.add("test.binding");
        rule.items.put("test.binding", new ItemCondition("#minecraft:pickaxes", ItemCondition.Mode.PREFER, ItemCondition.Hand.EITHER));
        PriorityConfig.save();
        PriorityConfig.load();
        ItemCondition saved = PriorityConfig.rule(name).items.get("test.binding");
        check(saved != null && saved.selector.equals("#minecraft:pickaxes") && saved.mode == ItemCondition.Mode.PREFER
                && saved.hand == ItemCondition.Hand.EITHER, "item condition config round trip");
        check(PriorityConfig.rule(name).exclusive && PriorityConfig.rule(name).order.contains("test.binding"), "rankings survive config round trip");
        PriorityConfig.removeRule(name);
    }

    private static void runItemTests(PriorityConfig.KeyRule rule, KeyMapping a, KeyMapping b) {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack stick = new ItemStack(Items.STICK);
        ItemCondition condition = new ItemCondition("minecraft:diamond_pickaxe", ItemCondition.Mode.REQUIRE, ItemCondition.Hand.MAIN);
        check(condition.matches(pickaxe, stick), "main-hand matching item");
        check(!condition.matches(stick, pickaxe), "main-hand must not match off-hand item");
        condition.hand = ItemCondition.Hand.OFF;
        check(condition.matches(stick, pickaxe), "off-hand matching item");
        check(!condition.matches(pickaxe, stick), "off-hand must not match main-hand item");
        condition.hand = ItemCondition.Hand.EITHER;
        check(condition.matches(pickaxe, stick) && condition.matches(stick, pickaxe), "either-hand condition");
        check(!condition.matches(ItemStack.EMPTY, ItemStack.EMPTY), "empty hands must not match");
        check(ItemCondition.validSelector("#minecraft:pickaxes"), "item tag selector accepted");
        check(!ItemCondition.validSelector("minecraft:missing_selftest_item"), "unknown item rejected");
        check(!ItemCondition.validSelector("#") && !ItemCondition.validSelector("bad id!"), "invalid selector rejected");
        condition.selector = "invalid id!";
        check(!condition.matches(pickaxe, pickaxe), "invalid config selector must not match or crash");
        condition.selector = "minecraft:diamond_pickaxe";

        rule.items.put(b.getName(), condition);
        rule.exclusive = true;
        check(KeyFilter.winner(rule, a, item -> item.matches(stick, stick)) == a, "required item absent: next binding wins");
        check(KeyFilter.winner(rule, a, item -> item.matches(pickaxe, stick)) == b, "required item present: top binding wins");
        expectFiltered("required item without player", a, b, List.of(a));
        rule.exclusive = false;
        expectFiltered("required item in all mode", a, b, List.of(a));
        rule.items.put(a.getName(), condition);
        expectFiltered("no eligible bindings", a, b, List.of());
        KeyMapping.click(key());
        expectClicks("menus: item conditions must not affect clicks", a, 1, b, 1);
        rule.items.remove(a.getName());

        rule.exclusive = true;
        rule.order = new ArrayList<>(List.of(a.getName(), b.getName()));
        condition.mode = ItemCondition.Mode.PREFER;
        check(KeyFilter.winner(rule, a, item -> item.matches(pickaxe, stick)) == b, "matching preference beats normal order");
        check(KeyFilter.winner(rule, a, item -> item.matches(stick, stick)) == a, "unmatched preference restores normal order");
        rule.items.put(a.getName(), condition);
        check(KeyFilter.winner(rule, a, item -> item.matches(pickaxe, stick)) == a, "matching preferences use list order");
        rule.items.remove(a.getName());
        rule.disabled.add(b.getName());
        check(KeyFilter.winner(rule, a, item -> item.matches(pickaxe, stick)) == a, "preference cannot enable disabled binding");
        rule.disabled.clear();

        rule.items.clear();
        KeyMapping.set(key(), true);
        check(a.isDown(), "held binding active before condition changes");
        condition.mode = ItemCondition.Mode.REQUIRE;
        rule.items.put(a.getName(), condition);
        check(KeyFilter.isSuppressed(a, true), "world: held binding must stop when item condition becomes inactive");
        check(a.isDown(), "menus: item condition must not affect held keys");
        KeyMapping.set(key(), false);
        rule.items.clear();
    }

    private static void runFilterTests() {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack stick = new ItemStack(Items.STICK);
        check(ItemFilter.parse("@minecraft").matches(pickaxe), "mod namespace filter");
        check(ItemFilter.parse("@Minecraft").matches(pickaxe), "filters ignore capitalization");
        check(!ItemCondition.validSelector("@") && !ItemCondition.validSelector("@missing_selftest_mod"), "unknown mod rejected");
        check(ItemFilter.parse("minecraft:*_pickaxe").matches(pickaxe), "wildcard matches item");
        check(!ItemFilter.parse("minecraft:*_pickaxe").matches(stick), "wildcard rejects other items");
        check(ItemFilter.parse("*_pickaxe").matches(pickaxe), "unqualified wildcard uses minecraft namespace");
        check(ItemFilter.parse("minecraft:stick, minecraft:diamond_pickaxe").matches(pickaxe), "comma means OR");
        check(!ItemFilter.parse("@minecraft, !minecraft:diamond_pickaxe").matches(pickaxe), "exclusion beats inclusion");
        check(ItemFilter.parse("@minecraft, !minecraft:diamond_pickaxe").matches(stick), "nonexcluded item remains eligible");
        check(!ItemFilter.parse("!minecraft:diamond_pickaxe").matches(pickaxe), "exclusion-only filter excludes item");
        check(ItemFilter.parse("!minecraft:diamond_pickaxe").matches(stick), "exclusion-only filter allows other held items");
        check(!ItemFilter.parse("*").matches(ItemStack.EMPTY), "wildcard must not match empty hands");
        check(!ItemFilter.parse("minecraft:stick, bad id!").matches(stick), "invalid term fails whole filter closed");
        check(!ItemCondition.validSelector("minecraft:stick,") && !ItemCondition.validSelector("!"), "empty terms rejected");
        check(ItemFilter.suggestions("minecraft:stick, !@mine", List.of("@minecraft", "minecraft:stick"))
                .equals(List.of("@minecraft")), "suggestions complete last term and handle exclusions");
        check(ItemFilter.suggestions("diamond", List.of("minecraft:diamond_pickaxe"))
                .equals(List.of("minecraft:diamond_pickaxe")), "unqualified item suggestion");
    }

    private static void expectFiltered(String what, KeyMapping a, KeyMapping b, List<KeyMapping> expected) {
        check(KeyFilter.filter(List.of(a, b), true).equals(expected), what);
    }

    private static void expectClicks(String what, KeyMapping a, int wantA, KeyMapping b, int wantB) {
        int gotA = drain(a);
        int gotB = drain(b);
        check(gotA == wantA && gotB == wantB,
                what + ": expected a=" + wantA + " b=" + wantB + ", got a=" + gotA + " b=" + gotB);
    }

    private static int drain(KeyMapping mapping) {
        int clicks = 0;
        while (mapping.consumeClick()) clicks++;
        return clicks;
    }

    private static void check(boolean ok, String message) {
        if (!ok) failures.add(message);
    }

    private static void finish(Minecraft minecraft) {
        if (failures.isEmpty()) {
            KeybindPriority.LOG.info("KEYBIND_PRIORITY_SELFTEST: PASS");
            minecraft.stop();
        } else {
            KeybindPriority.LOG.error("KEYBIND_PRIORITY_SELFTEST: FAIL {}", failures);
            System.out.flush();
            Runtime.getRuntime().halt(1);
        }
    }
}
