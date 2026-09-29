# Keybind Priority

Ever played a big modpack and pressed **V**, expecting to vein mine, and got the voice chat menu instead? Or
both at once?

Big packs run out of keys fast. In All the Mods 10, **C** is bound thirteen times out of the box. **H** eleven
times. **R** ten. NeoForge doesn't pick one when you press a key like that. It triggers all of them, and you
get whatever happens.

Your options so far: open the controls screen, scroll through 400 bindings, rebind half of them to keys you'll
never remember, and do it again after the next pack update.

**Keybind Priority lets you rank them instead.**

![Nine bindings on B in All the Mods 10, with Sophisticated Backpacks ranked first](docs/screenshot.png)

## How it works

Stand in the world, hold **Ctrl + Alt** and press the key that's causing trouble.

You get a list of everything bound to that key, with the mod's category, any modifier and where it applies.
Put the one you actually want at the top and switch the mode to **"top binding only"**. That's it. V vein
mines again, and voice chat keeps its binding but stops answering to V.

- **⤒ ▲ ▼** set the order. Top wins.
- **On / Off** silences one binding on this key without unbinding it.
- **Mode** switches between "all bindings" (the normal NeoForge behaviour) and "top binding only".

It's smart about context. A binding that only works in menus never blocks one that works in the world, and
the other way round. If the top binding can't do anything right now, the next one down takes over. So you
can put the key you use in menus and the key you use in game on the same button and both keep working.

Nothing is rebound, nothing is deleted. Remove the mod and every key behaves exactly like before. Your
rankings live in `config/keybind_priority.json`.

## Versions

NeoForge, client only. Servers don't need it. Pick the jar for your Minecraft version:

| Minecraft | NeoForge | Jar |
|---|---|---|
| 1.21, 1.21.1 | 21.0.143+, 21.1.x | `keybind-priority-neoforge-1.21-1.21.1-…` |
| 1.21.2, 1.21.3 | 21.2 beta, 21.3.56+ | `keybind-priority-neoforge-1.21.2-1.21.3-…` |
| 1.21.4 | 21.4.121+ | `keybind-priority-neoforge-1.21.4-…` |
| 1.21.5 | 21.5.74+ | `keybind-priority-neoforge-1.21.5-…` |
| 1.21.6, 1.21.7, 1.21.8 | 21.6 beta, 21.7 beta, 21.8.9+ | `keybind-priority-neoforge-1.21.6-1.21.8-…` |
| 1.21.9, 1.21.10 | 21.9 beta, 21.10.64+ | `keybind-priority-neoforge-1.21.9-1.21.10-…` |
| 1.21.11 | 21.11.42+ | `keybind-priority-neoforge-1.21.11-…` |

Every push starts the game on the first and last NeoForge build (beta or stable) of each of these and runs an in-game self
test: bindings are ranked, keys are pressed and held, and the menu is opened. See the Actions tab.

## Limits

- It only controls bindings that use Minecraft's normal keybind system. A mod that reads the keyboard
  directly can't be stopped.
- The menu opens for keyboard keys, not mouse buttons (yet).
- Ctrl + Alt + key only opens the menu in the world, never inside a screen, because Ctrl + Alt is AltGr on
  many keyboard layouts and you still want to type `@` in chat.

## Building

Java 21: `./gradlew build` builds every version, the jars end up in `versions/<minecraft>/build/libs/`.
Shared code lives in `src/`, the differences between Minecraft versions in `compat/legacy` (1.21–1.21.8) and
`compat/modern` (1.21.9+).

## License

MIT.

Code assisted by AI.
