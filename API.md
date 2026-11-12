# McExec API Reference

**Minecraft Java 26.3 / Fabric / Java 25 / Groovy 5**

McExec runs Groovy with direct access to Minecraft, Java and the local machine. There is no sandbox. Scripts execute on the Minecraft client thread after compilation, and registered callbacks remain active until the script is stopped, re-run under the same filename or the world changes.

## Script globals

| Global | Type | Purpose |
| --- | --- | --- |
| `api` | `dev.mcexec.client.ScriptAPI` | Managed game features, callbacks, reflection and cleanup |
| `draw` | `dev.mcexec.client.DrawAPI` | Persistent named 2D drawings |
| `mc` | `net.minecraft.client.Minecraft` | Minecraft singleton, including `player`, `level`, `options` and rendering state |
| `player` | `LocalPlayer` or `null` | Player at the moment the script begins |
| `world` | `ClientLevel` or `null` | World at the moment the script begins |
| `session` | `ScriptSession` | Current execution session |
| `log(value)` | closure | Append to the McExec Console |

Inside callbacks, always read `mc.player` and `mc.level` again because the player or world can change.

### Hello world

```groovy
log('Hello from McExec')
api.local('This appears in the Minecraft chat HUD')
```

## Console, chat and commands

| Method | Return | Notes |
| --- | --- | --- |
| `api.log(Object message)` | `void` | Colored script line in the McExec Console |
| `api.local(String message)` | `void` | Client-only chat message; also writes a console line |
| `api.chat(String message)` | `void` | Sends chat to the server. Strings beginning `/` are commands |
| `api.command(String command)` | `void` | Sends a command, with or without `/` |

`log('message')` is the shorthand form of `api.log('message')`. `api.local()` is the appropriate choice for status that needs to be seen while playing.

## Callbacks and sessions

| Method | Return | Notes |
| --- | --- | --- |
| `api.onTick { tick -> ... }` | `AutoCloseable` | Client-tick callback; argument is an integer counter |
| `api.onRender2D { canvas -> ... }` | `AutoCloseable` | HUD extraction callback, each rendered frame |
| `api.onRender3D { context -> ... }` | `AutoCloseable` | Raw Fabric `LevelRenderContext` at `LevelRenderEvents.END_MAIN`; does not supply a built-in cube renderer |
| `api.onKey('K') { code -> ... }` | `AutoCloseable` | Key press, once per press |
| `api.onKeyRelease('K') { code -> ... }` | `AutoCloseable` | Key release |
| `api.onKeyHeld('K') { code -> ... }` | `AutoCloseable` | Every tick while held |
| `api.onStop { ... }` | `void` | Register cleanup when the session stops |
| `api.defer { ... }` | `void` | Alias of `onStop` |
| `api.clearTicks()` | `void` | Remove tick callbacks belonging to current script |
| `api.clearKeys()` | `void` | Remove key callbacks belonging to current script |
| `api.keyCode(String key)` | `int` | SDL key constant, e.g. `K`, `F10`, `RSHIFT` |
| `api.keyDown(String key)` | `boolean` | Current key state; false when another GUI is open |
| `api.session()` | `ScriptSession` | Current active session |
| `api.cancelled()` | `boolean` | Whether the current session was cancelled |
| `api.activeScripts()` | `int` | Count of active script sessions |
| `api.stopScript(String name)` | `void` | Stop one named script |
| `api.stopAll()` | `void` | Stop all sessions and reset managed state |
| `api.scriptFolder()` | `Path` | User script directory |
| `api.client()` | `Minecraft` | Same instance as the `mc` global |
| `api.draw()` / `api.getDraw()` | `DrawAPI` | Same instance as `draw` |

You can call `.close()` on the `AutoCloseable` returned by a callback registration to unsubscribe it early. Press **Stop All** or use your assigned emergency key to stop scripts and remove managed HUD, movement, X-ray and glow changes. Unmanaged Java changes and network actions are not automatically reversed.

### Live UI

```groovy
api.onRender2D { canvas ->
    def localPlayer = mc.player
    if (localPlayer == null) return
    canvas.text(12, 12, String.format('HP %.1f', localPlayer.health), 0xFF6AFFAF as int)
}
```

## Drawing: `draw.*`

Named drawings stay on screen until replaced by another drawing using the same ID, removed, cleared or stopped. Values are Java `int` coordinates and ARGB colors.

| Method | Purpose |
| --- | --- |
| `draw.text(String id, int x, int y, String label, int argb)` | HUD text |
| `draw.centeredText(String id, int centerX, int y, String label, int argb)` | Centered HUD text |
| `draw.rect(String id, int x, int y, int width, int height, int argb)` | Solid rectangle |
| `draw.outline(String id, int x, int y, int width, int height, int argb)` | Rectangle border |
| `draw.line(String id, int x1, int y1, int x2, int y2, int argb)` | 1px line |
| `draw.remove(String id)` | Remove a named drawing owned by this script |
| `draw.clear()` | Clear all drawings owned by this script |

For moving drawings, use `api.onRender2D` and the callback's `canvas` argument instead of re-registering named drawings every tick.

## Drawing: `canvas.*`

`canvas` is a `DrawAPI.Canvas` available **only inside** `api.onRender2D { canvas -> ... }`.

| Method | Return / purpose |
| --- | --- |
| `canvas.width()` | Current GUI width (`int`), taking Minecraft's GUI scale into account |
| `canvas.height()` | Current GUI height (`int`) |
| `canvas.text(int x, int y, String label, int argb)` | Text |
| `canvas.centeredText(int centerX, int y, String label, int argb)` | Centered text |
| `canvas.rect(int x, int y, int width, int height, int argb)` | Solid rectangle |
| `canvas.outline(int x, int y, int width, int height, int argb)` | Border |
| `canvas.line(int x1, int y1, int x2, int y2, int argb)` | 1px line |
| `canvas.graphics()` | Raw `GuiGraphicsExtractor` for Minecraft 26.3 rendering methods |

Colors are 32-bit ARGB. For example, `0xFF55FFFF as int` is fully opaque aqua, and `0x8055FFFF as int` is 50% transparent aqua.

**Groovy numeric types:** `/` can return `BigDecimal`; `canvas.centeredText(canvas.width() / 2, ...)` will fail because the method needs `int`. Use `canvas.width().intdiv(2)` or `(canvas.width() / 2) as int`.

## Player movement and effects

| Method | Purpose |
| --- | --- |
| `api.move(String direction, boolean enabled)` | Press/release `forward`, `back`, `left`, `right`, `jump`, `sneak`, `sprint` |
| `api.stopMovement()` | Release script-held movement keys |
| `api.look(float yaw, float pitch)` | Set player rotation; pitch clamped to -90..90 |
| `api.velocity(double x, double y, double z)` | Set player velocity |
| `api.effect(String id, int seconds, int amplifier)` | Client-side potion effect |
| `api.clearEffect(String id)` | Remove an effect |
| `api.nightVision(int seconds)` | Shortcut for night vision |
| `api.speed(int seconds, int amplifier)` | Shortcut for speed |

McExec tracks previous values for supported effects, velocity and rotation and restores them on session stop where possible. This does not grant server privileges or guarantee that a multiplayer server will accept the change.

## X-ray and block visibility

| Method | Purpose |
| --- | --- |
| `api.xray(String... blockIds)` | Render only selected block IDs; no arguments use the built-in ore list |
| `api.hideBlocks(String... blockIds)` | Hide selected block IDs |
| `api.normalRendering()` | Restore standard terrain visibility |
| `api.xrayMode()` | Current render filter mode as `String` |

Block IDs can be `diamond_ore` or `minecraft:diamond_ore`. Filtering is client-side. X-ray is cleared by Stop All.

## Entity glow

| Method | Purpose |
| --- | --- |
| `api.coloredGlow()` | Enable default colored entity outlines |
| `api.coloredGlow(Map<String,String> colors)` | Configure colors for `players`, `hostile`, `animals`, `items`, `other` |
| `api.clearColoredGlow()` | Remove glow for the current script, or all if no script is active |

Example:

```groovy
api.coloredGlow([players: 'AQUA', hostile: 'RED', animals: 'GREEN', items: 'YELLOW', other: 'LIGHT_PURPLE'])
```

## Textures and resource packs

| Method | Return | Purpose |
| --- | --- | --- |
| `api.overrideTexture(String textureId, String pngPath)` | `Path` | Copy a PNG into the McExec resource pack |
| `api.solidTexture(String textureId, int argb)` | `Path` | Generate a solid 16x16 PNG |
| `api.transparentTexture(String textureId)` | `Path` | Generate a transparent 16x16 PNG |
| `api.removeTexture(String textureId)` | `void` | Delete an override |
| `api.clearTextures()` | `void` | Delete PNG overrides in the generated pack |
| `api.reloadTextures()` | `void` | Request a resource reload |

Files live under `resourcepacks/MCExecOverrides`. Enable this pack in Minecraft for overrides to appear. McExec records previous files and attempts to restore them when a managed session stops. Reloading textures is not a guarantee that the pack has been enabled.

## JVM and reflection: `api.*`

Low-level Java functions are available **directly under `api`**, not under a separate `advanced` namespace.

| Method | Return | Purpose |
| --- | --- | --- |
| `api.findField(Class<?> type, String name)` | `Field` | Search class hierarchy for a field and make it accessible |
| `api.getField(Object target, String name)` | `Object` | Read a field, including private fields; `target` may be a `Class` for static fields |
| `api.setField(Object target, String name, Object value)` | `void` | Write a field; changes are not automatically reverted |
| `api.setTrackedField(Object target, String name, Object value)` | `void` | Write a field and attempt to restore its original value on stop |
| `api.fields(Class<?> type)` | `List<String>` | Declared field signatures |
| `api.methods(Class<?> type)` | `List<String>` | Declared method signatures |
| `api.privateLookup(Class<?> type)` | `MethodHandles.Lookup` | Java `privateLookupIn()` for method handles |
| `api.arena()` | `java.lang.foreign.Arena` | Confined foreign-memory arena; must be closed by caller |

### Inspect the real state of an entity

```groovy
import net.minecraft.world.phys.EntityHitResult

def target = mc.hitResult instanceof EntityHitResult ? mc.hitResult.entity : mc.player
if (target != null) {
    log('Fields: ' + api.fields(target.class))
    log('tickCount: ' + api.getField(target, 'tickCount'))
}
```

`api.getField`, `api.setField` and `api.setTrackedField` use reflection. Access can fail for Java module boundaries, security restrictions or changed Minecraft mappings. Native memory, arbitrary reflection and raw Java process calls are not managed by Stop All.

For backwards compatibility, `api.advanced()` / `api.getAdvanced()` are retained as Java methods, but new scripts should use the direct `api.*` methods above.

## Lifecycle and persistence

- `Run` saves the code under its filename in `config/mcexec/scripts` and executes it. Re-running that filename replaces the previous session.
- `Save` and `Load` manage files in the script directory; `Examples` loads a bundled demo into the editor, without executing it.
- `api.onStop` is for script-defined cleanup; use `Stop All` to halt managed callbacks and reset built-in visual effects.
- The editor, console, theme, scale, hotkeys and code syntax colors are client-side features, not server features.
- Scripts have JVM-level permissions. Only run code you trust.
