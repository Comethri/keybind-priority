package com.comethri.keybindpriority;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Per-key rules, stored in {@code config/keybind_priority.json}. Keys are
 * {@code InputConstants.Key#getName()} (e.g. {@code key.keyboard.v}), bindings are
 * {@code KeyMapping#getName()} (e.g. {@code key.ftbultimine}), so both survive restarts and mod updates.
 */
public final class PriorityConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final class KeyRule {
        /** true: only the highest-ranked binding whose context is active reacts. */
        public boolean exclusive;
        /** Binding names, highest priority first. */
        public List<String> order = new ArrayList<>();
        /** Bindings that never react to this key. */
        public Set<String> disabled = new LinkedHashSet<>();

        void sanitize() {
            if (order == null) order = new ArrayList<>();
            if (disabled == null) disabled = new LinkedHashSet<>();
        }
    }

    private static final class Data {
        int version = 1;
        Map<String, KeyRule> keys = new TreeMap<>();
    }

    private static Data data = new Data();

    private PriorityConfig() {
    }

    public static KeyRule rule(String keyName) {
        return data.keys.get(keyName);
    }

    public static KeyRule ruleOrCreate(String keyName) {
        return data.keys.computeIfAbsent(keyName, k -> new KeyRule());
    }

    public static void removeRule(String keyName) {
        data.keys.remove(keyName);
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(KeybindPriority.MOD_ID + ".json");
    }

    public static void load() {
        Path file = file();
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded == null) return;
            if (loaded.keys == null) loaded.keys = new TreeMap<>();
            loaded.keys = new TreeMap<>(loaded.keys);
            loaded.keys.values().removeIf(r -> r == null);
            loaded.keys.values().forEach(KeyRule::sanitize);
            data = loaded;
        } catch (IOException | JsonParseException e) {
            KeybindPriority.LOG.error("Could not read {}, starting with no rules", file, e);
        }
    }

    public static void save() {
        Path file = file();
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            KeybindPriority.LOG.error("Could not write {}", file, e);
        }
    }
}
