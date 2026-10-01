package com.comethri.keybindpriority;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Comma-separated alternatives, followed by exclusions that always take precedence. */
final class ItemFilter {
    private record Term(String selector, boolean excluded, Pattern glob) {
        boolean matches(ItemStack stack, String id) {
            if (selector.startsWith("@")) return id.startsWith(selector.substring(1) + ":");
            if (glob != null) return glob.matcher(id).matches();
            return Compat.get().matchesItemSelector(stack, selector);
        }
    }

    private final List<Term> terms;
    private final String error;

    private ItemFilter(List<Term> terms, String error) {
        this.terms = terms;
        this.error = error;
    }

    static ItemFilter parse(String expression) {
        List<Term> terms = new ArrayList<>();
        for (String raw : expression.split(",", -1)) {
            String token = raw.trim();
            boolean excluded = token.startsWith("!");
            String selector = (excluded ? token.substring(1).trim() : token).toLowerCase(Locale.ROOT);
            Pattern glob = null;
            boolean valid;
            if (selector.startsWith("@")) {
                String mod = selector.substring(1);
                valid = mod.matches("[a-z0-9_.-]+") && Catalog.SELECTORS.contains("@" + mod);
            } else if (selector.contains("*") && !selector.startsWith("#")) {
                String normalized = selector.contains(":") || selector.equals("*") ? selector : "minecraft:" + selector;
                valid = normalized.matches("[a-z0-9_.*-]+:[a-z0-9_./*-]+|\\*");
                if (valid) {
                    String regex = java.util.Arrays.stream(normalized.split("\\*", -1))
                            .map(Pattern::quote).collect(Collectors.joining(".*"));
                    glob = Pattern.compile(regex);
                }
            } else {
                valid = !selector.isEmpty() && Compat.get().validItemSelector(selector);
            }
            if (!valid) return new ItemFilter(List.of(), token.isEmpty() ? "<empty>" : token);
            terms.add(new Term(selector, excluded, glob));
        }
        return new ItemFilter(List.copyOf(terms), null);
    }

    String error() {
        return error;
    }

    boolean matches(ItemStack stack) {
        if (error != null || stack.isEmpty()) return false;
        String id = Compat.get().itemId(stack);
        boolean hasPositive = false;
        boolean positiveMatch = false;
        for (Term term : terms) {
            boolean match = term.matches(stack, id);
            if (term.excluded && match) return false;
            if (!term.excluded) {
                hasPositive = true;
                positiveMatch |= match;
            }
        }
        return !hasPositive || positiveMatch;
    }

    /** An item this filter accepts, to show as its icon; empty if none does. */
    ItemStack example() {
        if (error != null) return ItemStack.EMPTY;
        for (Term term : terms) {
            ItemStack exact = term.excluded ? null : AllItems.BY_ID.get(term.selector);
            if (exact != null && matches(exact)) return exact;
        }
        for (ItemStack stack : AllItems.STACKS) {
            if (matches(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    static List<String> suggestions(String expression, List<String> catalog) {
        String token = expression.substring(expression.lastIndexOf(',') + 1).trim();
        String prefix = (token.startsWith("!") ? token.substring(1).trim() : token).toLowerCase(Locale.ROOT);
        if (prefix.isEmpty()) return List.of();
        return catalog.stream().filter(entry -> !entry.equals(prefix)
                && (entry.startsWith(prefix) || (!prefix.contains(":") && entry.startsWith("minecraft:" + prefix))))
                .limit(32).toList();
    }

    private static final class Catalog {
        static final List<String> SELECTORS = Compat.get().itemSelectors();
    }

    private static final class AllItems {
        static final List<ItemStack> STACKS = BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new).filter(stack -> !stack.isEmpty()).toList();
        static final Map<String, ItemStack> BY_ID = STACKS.stream()
                .collect(Collectors.toMap(stack -> Compat.get().itemId(stack), stack -> stack, (a, b) -> a));
    }
}
