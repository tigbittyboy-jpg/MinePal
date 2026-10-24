# MinePal / Companion — Minecraft Java 26.3

**Current build: 0.4.0**, an incremental survival and intelligence update.

[Download JAR](downloads/companion-0.4.0/companion-0.4.0.jar?raw=true) · [Release notes](downloads/companion-0.4.0/README.md) · [Test report](docs/TESTING_0.4.0.md) · [Original specification](docs/PROJECT_SPECIFICATION.md) · [0.4.0 request](docs/REQUEST_0.4.0.md)

## Install and activate

Use **Minecraft Java 26.3**, **Java 25**, **Fabric Loader 0.20.1**, and **Fabric API 0.165.0+26.3**. Install the same Companion version on server and clients. Back up your world; move the previous Companion JAR outside `mods/` and install the new JAR alongside Fabric API. Only one Companion version should be active.

On macOS, open Finder → Go → Go to Folder and enter:

```text
~/Library/Application Support/minecraft/mods
```

Launch the Fabric profile. Use `/companion spawn` once, then **right-click your companion**. Existing companions retain their names, inventories, settings, and home. Sneak-right-click remains quick Stay. `/companion menu` is optional.

**Existing companions preserve their combat mode.** To enable assistance when you attack an animal, choose **More Options → Combat & Settings → Combat: Aggressive**. Newly created companions default to Aggressive. Passive defends himself; Defensive defends himself and his owner; Aggressive also assists with the owner's selected living target. PvP assistance is separately configurable and off by default. Owner, companions, and tame pets are excluded.

## Menu and tasks

The main menu has six choices: **Follow Me**, **Stay Here**, **Gather Resources**, **Craft Equipment**, **Inventory**, and **More Options**. It displays actual health, hunger, combat mode, objective, collected/requested quantity, task state, and explanation. Resources offers wood, stone, and exposed ore with a quantity; **0 means open-ended**. More Options contains cancellation, home, storage, equipment/name, workstation crafting, and settings. Inventory exchange still requires proximity; task management can reach your loaded, owned companion in the same dimension.

Resource exploration scans loaded terrain incrementally and walks toward new search cells instead of abandoning an assignment at an arbitrary radius. It remembers searched cells and discovered resources/tables. Unloaded terrain does not get force-loaded. Tasks retain objectives, collected quantities, exploration progress, and working state across serialization and combat interruptions. Blocked work reports why and retains the assignment.

Natural-tree harvesting tracks connected trunks and branches and collects real drops. It finishes the selected accessible tree even if this exceeds the requested quantity. Identifiable remaining logs prevent a completed status. Soil, canopy, wood-family, and bounded connectivity checks reject simple log buildings; Minecraft cannot reliably prove whether an old tree-like structure was player-built. Avoid assigning work beside valuable builds.

Mine Stone can cut inspected staircase segments through dirt into stone. It avoids digging directly below itself, fluid-adjacent cuts, containers, falling blocks, and unsupported landings. Mine Exposed Ore searches visible loaded resources, not hidden ore through walls. Breaking takes time and respects tools, durability, loot rules, protection callbacks, and inventory capacity. Progress counts accepted task-resource drops, not dirt or invented items.

Craft Equipment uses owned resources and real recipes to make planks, sticks, a bench, and common tools, upgrading tiers when materials permit. It may place one owned bench safely. Automatic post-gather preparation does not place blocks. Craft inventory recipe retains the first-nine-slots recipe grid. Workstation tasks remember tables, explore for them, and can walk to distant known benches. Shared storage retains explicit container permission and real stack transfers.

Combat uses server-side owner attack detection, actual melee, owned weapon selection, cooldowns and durability. Threat scoring considers health, enemy type/count/distance, recent damage, armor, weapon, and terrain. Retreat uses reachable walking routes and hysteresis. **Navigation teleports are disabled**, including the old emergency setting, as required by the 0.4.0 request. Work resumes after combat when safe.

## Scope and remaining work

This is an incremental release, **not completion of the entire 0.4.0 request**. Exploration, excavation, workstations, survival, and complete-tree milestones remain partial:

- No temporary scaffolding: unreachable high crowns remain blocked, never falsely completed. Giant/complex trees need more work.
- Exploration depends on normally loaded chunks, uses bounded memory, and cannot guarantee a route through arbitrary terrain. No permanent chunk loader or full expedition return planner.
- Natural-tree recognition is heuristic; no universal protection guarantee for tree-like player builds.
- Staircase excavation is conservative; no advanced cave engineering, buried-ore planning, smelting, or full independent progression.
- Placed crafting benches are remembered but not automatically recovered.
- Retreat has no shelter construction or ranged tactics. Food remains legitimate existing inventory consumption.
- Death remains permanent; respawning, keepInventory and death recovery are unfinished. Long survival sessions, multiplayer/PvP edge cases and full disk upgrade/reload need manual testing.
- Daily routines, expeditions/signs, personality, activity history and full dialogue remain later milestones.

Previous artifacts remain intact: [0.1.0](downloads/companion-0.1.0), [0.2.0](downloads/companion-0.2.0), [0.3.0](downloads/companion-0.3.0). Previous instructions are archived in [README 0.3.0](docs/README_0.3.0.md); the original specification is unchanged.

## Build and verification

Verified toolchain: Java 25.0.5.0.2, Minecraft 26.3 unobfuscated Mojang names, Fabric Loader 0.20.1, Fabric API 0.165.0+26.3, Loom 1.19.3 and Gradle 9.8.2. No gameplay AI service is required.

```sh
./scripts/dev.sh --no-daemon build
./scripts/headless-client.sh
```

The build compiles common/client code and runs server GameTests. The second command runs the real integrated client/menu test using local Xvfb and Vulkan software rendering. On a Java 25 graphical workstation, use `./gradlew build` and `./gradlew runClient`. Existing bootstrap/headless setup scripts remain available. See the release test report for coverage and limits.
