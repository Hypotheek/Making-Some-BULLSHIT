# Making Some BULLSHIT

A ProjectE addon for Minecraft 1.20.1 (Forge 47.4.0) by Hypotheek.

Planned: an interface that lists the EMC links you have running and what they cost you. Right now the mod
loads and does nothing; this is the scaffold to build that on.

## Run it

```powershell
.\gradlew runClient
```

The first run downloads and decompiles Minecraft and every dependency, which takes several minutes.
The game runs from `run/`.

You need a JDK 17 installed. Gradle finds it on its own (`gradle/gradle-daemon-jvm.properties` asks for
Java 17), so your default Java can be a newer one.

## What's in the dev instance

Only the mods needed to reproduce the ProjectE side of the High-altitude Alchemy 1.5.5-FINAL pack, at the
exact CurseForge files from its `manifest.json`:

| Mod | File | How it's used |
|---|---|---|
| ProjectE | ProjectE-1.20.1-PE1.0.1.jar | compile + runtime |
| Project Expansion | projectexpansion-1.20.1-1.1.3.jar | compile + runtime |
| Team ProjectE | teamprojecte-1.20.1-1.1.4.jar | runtime only |
| KubeJS ProjectE Fork | kjsprojecte-1.20.1-1.0.jar | runtime only |
| KubeJS | kubejs-forge-2001.6.5-build.16.jar | runtime only (needed by the fork) |
| Rhino | rhino-forge-2001.2.2-build.6.jar | runtime only (needed by KubeJS) |
| Architectury API | architectury-9.2.14-forge.jar | runtime only (needed by KubeJS) |
| Curios API | curios-forge-5.14.1+1.20.1.jar | runtime only (ProjectE and Project Expansion integrate with it) |

The file IDs are in `gradle.properties`; the repository and dependency lines are in `build.gradle`.

## Configs and names

The dev instance runs with every mod's own default config, names and textures. None of the pack's configs,
KubeJS scripts or resource overrides are applied, and its other ~150 mods aren't loaded. The mods listed above
are the pack's exact versions, so the code behaves the same as it would inside the pack.

The EMC links screen doesn't hard-code tier names or colours. Names come from the link block's own translated
name, and each tier's colour is sampled from its link texture as the game has it loaded. If you drop a resource
pack or a KubeJS `assets` override into `run/`, the screen follows it.

## Layout

- `src/main/java/com/hypotheek/makingsomebullshit/MakingSomeBullshit.java` – `@Mod` entry point
- `src/main/java/com/hypotheek/makingsomebullshit/client/ClientSetup.java` – client-only setup (screens, keybinds, overlays)
- `src/main/resources/META-INF/mods.toml` – mod metadata; values come from `gradle.properties`
- `src/main/resources/assets/making_some_bullshit/` – assets and lang files
- `src/main/java/com/hypotheek/makingsomebullshit/mixin/` and `src/main/resources/making_some_bullshit.mixins.json` – mixins into other mods (currently one, to read the EMC value of imports)
- `docs/` – notes on the mods' APIs and EMC link internals, for building the link tracker

New code goes under `src/main/java/com/hypotheek/makingsomebullshit/`, in subpackages next to `client/`
(for example `tracker/` and `command/`). `.\gradlew compileJava` is a quick way to check that it builds
without launching the game.

## License

MIT, see [LICENSE](LICENSE). That covers the code in this repository only. The mods it builds on (ProjectE,
Project Expansion, Team ProjectE, KubeJS and the rest) are not included here and keep their own licences.
