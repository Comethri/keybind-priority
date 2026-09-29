# Keybind Priority

Big modpacks put ten mods on the same key. NeoForge triggers all of them at once, so pressing **V** opens the
voice chat menu *and* starts vein mining. Keybind Priority lets you decide who wins.

## How to use

1. In game, hold **Ctrl + Alt** and press the key you want to sort out.
2. A menu lists every binding on that key, with its category, modifier and context.
3. Use ▲ / ▼ to set the order, or ⤒ to bring a binding straight to the top. The top one has the highest
   priority.
4. Switch the mode to **"top binding only"**.
5. Or turn single bindings **off** for this key without unbinding them.

"Active" means the binding's context: a menu-only binding never blocks an in-game one. If the top binding
isn't available right now, the next one down takes over.

Settings are stored in `config/keybind_priority.json`.

## Limits

- Only bindings that go through Minecraft's normal keybind system can be controlled. A mod that reads the
  keyboard directly can't be stopped.
- The menu opens for keyboard keys, not mouse buttons (yet).
- Ctrl + Alt + key only opens the menu in game, never inside a screen, because Ctrl + Alt is AltGr on many
  keyboard layouts.

## Building

NeoForge 1.21.1, Java 21: `./gradlew build` → `build/libs/`.

Client-only. MIT licensed.

Code assisted by AI.
