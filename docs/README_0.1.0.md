# Companion for Minecraft Java 26.3

An offline, deterministic Fabric companion mod. This **0.1.0 development build**
implements the foundation, following/home commands, and part of inventory and
survival. It is not the complete autonomous survival mod yet. The original
[project specification](docs/PROJECT_SPECIFICATION.md) remains unchanged.

## Install

1. Back up the entire world folder before installing. Use a test world first.
2. Install Minecraft Java **26.3**, Java **25**, and Fabric Loader **0.20.1**.
3. Put `companion-0.1.0.jar` and **Fabric API 0.165.0+26.3** in `mods/`.
4. For dedicated multiplayer, install the mod and Fabric API on both the server
   and every client. The server owns gameplay and inventory state.
5. Start the world, enable commands, and run `/companion spawn`.

The installable artifact is `build/libs/companion-0.1.0.jar`. Do not install the
`-sources.jar` or the test mod. Gameplay code makes no network or AI API requests.

## Controls

Commands select your nearest loaded owned companion within 64 blocks.

| Command | Effect |
| --- | --- |
| `/companion spawn` | Create and own a companion at your position. Refuses another spawn when you already have one loaded within 128 blocks. |
| `/companion follow` | Follow you in the same dimension; stops within roughly four blocks. |
| `/companion stay` | Cancel travel and wait; self-defense remains enabled. |
| `/companion home` | Assign the bed you are looking at, within six blocks. |
| `/companion return` | Cancel following and walk toward the assigned home. |
| `/companion name Rowan` | Set a persistent name, up to 32 characters. |
| `/companion status` | Show current travel activity, hunger, and assigned home coordinates. |
| `/companion inventory` | Open the vanilla 27-slot personal inventory and exchange actual items. |
| `/companion equip` | Move suitable owned equipment into empty equipment slots. |
| `/companion craft` | Craft once from the first nine inventory slots, arranged as a 3×3 recipe grid. Requires a crafting table within three blocks. |

Right-click your companion to toggle follow/stay. Sneak-right-click to stay.
Unknown and player-owned containers are never accessed. Only the owner can open
the inventory; the menu closes when the companion is dead or more than eight
blocks away. Personal inventory items may be retrieved by that owner.

Food in personal storage is consumed when hunger is 14 or lower. Food effects
and container remainders use vanilla item consumption. This version uses a
simplified hunger model: one point per 30 seconds, food-based regeneration,
and no starvation damage. Equipment is checked every ten seconds; existing
gear is never silently discarded for an upgrade. Crafting uses the world's
actual recipe manager, checks space before committing, consumes ingredients,
and retains recipe remainders. Autonomous crafting plans are not implemented.

## Build and development

Verified toolchain:

| Component | Version |
| --- | --- |
| Minecraft | 26.3 release |
| Java used | Oracle OpenJDK-compatible JDK 25.0.5.0.2+1-LTS-2 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.165.0+26.3 |
| Fabric Loom | 1.19.3 |
| Gradle wrapper | 9.8.2 |
| Mappings | Mojang's unobfuscated names; no Yarn dependency |

On this cloud environment:

```sh
cd /workspace/MinePal
./scripts/bootstrap.sh                 # verified local JDK, wrapper, build
./scripts/dev.sh --no-daemon runGameTest
./scripts/dev.sh --no-daemon runClient  # requires a graphical display
```

On a normal workstation with Java 25, use `./gradlew build`, `./gradlew runGameTest`,
and `./gradlew runClient`. The Gradle distribution has a pinned SHA-256 checksum.
The bootstrap JDK also has a pinned checksum; if Oracle's `latest` download changes,
bootstrap fails safely instead of accepting an unverified archive. Retained cloud
snapshots already contain that verified JDK. For a changed proxy configuration,
update proxy host/port in `$GRADLE_USER_HOME/gradle.properties`; the helper preserves
an existing file. It uses the machine's managed Java truststore and keeps TLS
verification enabled.

Optional cloud graphical tests, using local Xvfb and Mesa lavapipe:

```sh
./scripts/install-headless.sh
./scripts/headless-client.sh
```

The installer uses signed Debian package indexes and extracts packages locally.
It does not replace system packages. Graphical software rendering is slower than
hardware rendering. Do not run the client and server GameTest tasks concurrently:
Loom clears their shared generated test directory. Test worlds under `build/run/`
are disposable. Do not use this directory for a real survival world.

Cloud tasks are already isolated: use the existing checkout without creating a
Git worktree. See [testing and limitations](docs/TESTING.md).

## Current limitations

- No autonomous gathering, mining, smelting, progression planning, expeditions,
  status signs, settings GUI, personality, activity-history UI, or dialogue menu.
- **Death is currently permanent.** Inventory drops, but `keepInventory`, respawn,
  and recovery are not yet implemented. Use a backed-up test world.
- Following does not cross dimensions, force-load chunks, or teleport. An unloaded
  companion pauses with normal Minecraft entity simulation. Commands find loaded
  companions only. Blocked paths retry with backoff; no guarantee of escape.
- Combat currently retaliates against non-player attackers using vanilla melee
  goals. Owner defense, retreat priorities, and combat settings are unfinished.
- Home is a saved bed coordinate in the companion's current dimension; bed claims,
  bed invalidation, sleep, and daily routines are later work.
- Rendering uses vanilla humanoid animation and equipment layers. Full terrain,
  swimming, combat, and multiplayer soak testing remains manual.

The next milestone is to finish and test follower safety/combat, then complete
milestone 3 before adding autonomous world modification. See the specification
for milestones 4–9. There are no pretend expedition rewards or resource gathering.
