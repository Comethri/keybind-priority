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

**One jar for every Minecraft 1.21 version**, from 1.21 to 1.21.11 (NeoForge 21.0.143 and newer, including
the beta-only releases 1.21.2, 1.21.6, 1.21.7 and 1.21.9). Client only, servers don't need it.

The jar carries three small compatibility parts, each built against the Minecraft version it is for, and
picks the right one when the game starts.

Every push starts that exact jar on the first and last NeoForge build of every 1.21.x (23 versions) and runs
an in-game self test: bindings are ranked, keys are pressed and held, and the menu is opened. See the
Actions tab.

## Limits

- It only controls bindings that use Minecraft's normal keybind system. A mod that reads the keyboard
  directly can't be stopped.
- The menu opens for keyboard keys, not mouse buttons (yet).
- Ctrl + Alt + key only opens the menu in the world, never inside a screen, because Ctrl + Alt is AltGr on
  many keyboard layouts and you still want to type `@` in chat.

## Building

Java 21: `./gradlew build`, the jar ends up in `build/libs/`.

- `src/` is built against 1.21.1 and must only use Minecraft methods that exist unchanged in every 1.21.x.
  `python tools/check_linkage.py <classes> <neoforge-merged.jar>` checks that.
- `compat/v1_21_6` (built against 1.21.8) and `compat/v1_21_9` (built against 1.21.11) hold what differs.
- `./gradlew :testrun:runSelftest -Ptest_neo_version=<version>` starts the finished jar on any NeoForge
  version and runs the self test.

## License

MIT.

Code assisted by AI.
