package com.comethri.keybindpriority;

import com.comethri.keybindpriority.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;

import java.util.ArrayList;
import java.util.List;

/** Decides which bindings are allowed to react to a key, based on {@link PriorityConfig}. */
public final class KeyFilter {
    private KeyFilter() {
    }

    /** Drops every suppressed binding from what NeoForge found for a key press. */
    public static List<KeyMapping> filter(List<KeyMapping> found) {
        if (found.isEmpty()) return found;

        KeyMapping sameKey = null;
        for (KeyMapping mapping : found) {
            if (mapping != null) {
                sameKey = mapping;
                break;
            }
        }
        if (sameKey == null) return found;

        PriorityConfig.KeyRule rule = PriorityConfig.rule(sameKey.getKey().getName());
        if (rule == null) return found;
        KeyMapping winner = rule.exclusive ? winner(rule, sameKey) : null;

        List<KeyMapping> out = null;
        for (int i = 0; i < found.size(); i++) {
            KeyMapping mapping = found.get(i);
            boolean suppressed = mapping != null && (rule.disabled.contains(mapping.getName())
                    || (rule.exclusive && winner != null && winner != mapping));
            if (suppressed) {
                if (out == null) out = new ArrayList<>(found.subList(0, i));
            } else if (out != null) {
                out.add(mapping);
            }
        }
        return out == null ? found : out;
    }

    /** True if this binding must not react to its key right now. */
    public static boolean isSuppressed(KeyMapping mapping) {
        PriorityConfig.KeyRule rule = PriorityConfig.rule(mapping.getKey().getName());
        if (rule == null) return false;
        if (rule.disabled.contains(mapping.getName())) return true;
        if (!rule.exclusive) return false;
        KeyMapping winner = winner(rule, mapping);
        return winner != null && winner != mapping;
    }

    /**
     * The highest-ranked enabled binding on the same key whose context and modifier are active right now.
     * Ranking by "active right now" means a GUI-only binding never blocks an in-game one and vice versa.
     */
    private static KeyMapping winner(PriorityConfig.KeyRule rule, KeyMapping sameKey) {
        var all = KeyMappingAccessor.keybindPriority$all();
        for (String name : rule.order) {
            if (rule.disabled.contains(name)) continue;
            KeyMapping candidate = all.get(name);
            if (candidate == null || !candidate.getKey().equals(sameKey.getKey())) continue;
            IKeyConflictContext context = candidate.getKeyConflictContext();
            if (context.isActive() && candidate.getKeyModifier().isActive(context)) return candidate;
        }
        return null;
    }
}
