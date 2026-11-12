# McExec

A client-side Groovy script executor for Minecraft 26.3, Fabric, and Java 25.

## Features

- Movable in-game editor with Executor, Console, Examples, Settings, and Credits
- Live Minecraft and Java access
- Keyboard events, movement, effects, X-ray, texture overrides, and colored entity glow
- 2D HUD drawing and raw Fabric 3D render callbacks
- Script sessions with cleanup and F10 Stop All

## Build

Use Java 25. Open the folder in IntelliJ as a Gradle project or run:

```powershell
.\gradlew.bat runClient
.\gradlew.bat build
```

The mod JAR is in `build/libs/`. Install Fabric API when using it outside the development environment.

## Controls

- **Right Shift**: open McExec
- **F10**: stop managed scripts and remove their overlays

Scripts are stored in `.minecraft/config/mcexec/scripts/`. The 11 curated examples are available in the **Examples** tab.

## Scripting

```groovy
log('Hello from McExec')
api.local('Hello in Minecraft')

api.onKey('K') { api.local('K pressed') }
api.onKeyHeld('H') { /* runs every tick while held */ }

draw.rect('panel', 10, 10, 140, 30, 0xAA111111 as int)
draw.text('title', 18, 20, 'McExec', 0xFFFFFFFF as int)

api.onStop { log('Script stopped') }
```

Built-in bindings: `mc`, `player`, `world`, `api`, `draw`, `session`, `log`.
For direct JVM work, use `api.getField`, `api.fields`, `api.privateLookup`, or import Java and Minecraft classes.

See [API reference](API.md) for the complete API, reflection helpers and drawing methods.

## Stop All

F10 and `api.stopAll()` remove McExec-managed callbacks, key states, HUD drawings, rendering filters, glow, and tracked modifications. They cannot undo arbitrary Java code, external file changes, server commands, or a frozen client thread. Run scripts you trust.

## License

MIT. See [LICENSE.md](LICENSE.md).
