package com.comethri.keybindpriority;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game check for CI, only active with {@code -Dkeybindpriority.selftest=true}. Once the game has loaded it
 * puts two bindings on an unused key, checks every rule against real key presses, opens the menu for a few
 * frames and quits. The log line {@code KEYBIND_PRIORITY_SELFTEST: PASS} or {@code FAIL} is the result.
 */
public final class SelfTest {
    public static final String PROPERTY = "keybindpriority.selftest";
    private static final int TEST_KEY = GLFW.GLFW_KEY_F25;
    private static final int MENU_TICKS = 160;
    /** CI takes a screenshot of the display once this line shows up in the log. */
    private static final int MENU_READY_TICK = 20;

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
                minecraft.setScreen(new PriorityScreen(key()));
                stage = 1;
            } else if (stage == 1 && ++ticks == MENU_READY_TICK) {
                KeybindPriority.LOG.info("KEYBIND_PRIORITY_SELFTEST: MENU_OPEN");
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

    private static void runKeyTests() {
        InputConstants.Key key = key();
        KeyMapping a = Compat.get().newTestMapping("key.keybind_priority.selftest.a", key);
        KeyMapping b = Compat.get().newTestMapping("key.keybind_priority.selftest.b", key);

        KeyMapping.click(key);
        expectClicks("no rule", a, 1, b, 1);

        PriorityConfig.KeyRule rule = PriorityConfig.ruleOrCreate(key.getName());
        rule.exclusive = true;
        rule.order = new ArrayList<>(List.of(b.getName(), a.getName()));
        KeyMapping.click(key);
        expectClicks("top binding only", a, 0, b, 1);

        KeyMapping.set(key, true);
        check(!a.isDown() && b.isDown(), "held: only the top binding should be down");
        KeyMapping.set(key, false);
        check(!a.isDown() && !b.isDown(), "release: nothing should stay down");

        check(Compat.get().matchesKeysym(b, TEST_KEY) && !Compat.get().matchesKeysym(a, TEST_KEY), "matches: only the top binding should match");

        rule.disabled.add(b.getName());
        KeyMapping.click(key);
        expectClicks("top one off, next takes over", a, 1, b, 0);

        rule.exclusive = false;
        rule.disabled.clear();
        KeyMapping.click(key);
        expectClicks("all bindings", a, 1, b, 1);

        // Leave a ranked rule behind so the menu screenshot shows a winner and a suppressed binding.
        rule.exclusive = true;
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
