# Companion — Minecraft Java 26.3

**Current build: 0.3.0.** An offline, deterministic Fabric companion with real
inventory, gathering, crafting, and a dedicated interaction UI.

[Download 0.3.0](downloads/companion-0.3.0/companion-0.3.0.jar?raw=true) ·
[Release notes](downloads/companion-0.3.0/README.md) ·
[Testing](docs/TESTING_0.3.0.md) ·
[Original specification](docs/PROJECT_SPECIFICATION.md)

## Install and activate

Use **Minecraft Java 26.3**, **Java 25**, **Fabric Loader 0.20.1**, and
**Fabric API 0.165.0+26.3**. Back up the world before upgrading. Move the old
Companion JAR into a backup folder outside `mods/`; only one version should be
active. Add the new Companion JAR and Fabric API to `mods/`. Dedicated servers
and all clients need the same 0.3.0 mod.

On macOS, the normal launcher folder is:

```text
~/Library/Application Support/minecraft/mods
```

Launch the Fabric profile. Use `/companion spawn` once to create your companion.
Existing companions keep their entity ID and old save fields. **Right-click him
to open the menu.** Sneak-right-click remains a quick Stay shortcut. `/companion
menu` is an optional alternative. Ordinary actions no longer require commands.

## The menu

Use `<` and `>` at the bottom to switch between four pages. Escape/Done closes
the menu. It stays live while the world runs and shows actual server activity.

- **Actions:** Follow, Stay, Return Home, Stop Task, Gather Wood, Mine Nearby,
  Craft Basic Tools, Craft Inventory Recipe, Trade/Inventory, and Set Nearest Bed.
- **Settings:** combat mode, follow distance, emergency teleport, automatic gifted
  gear use, gathering, mining, basic crafting after resource work, leaf clearing,
  and explicitly shared storage access.
- **Equipment & name:** rename him, equip gear, recover equipment, or trade.
  Recovering equipment disables automatic re-equipping so the action stays useful.
- **Storage:** designate the nearest chest/barrel, enable access, choose a source
  slot (1–27), then deposit or withdraw that whole stack. Revocation is one click.
  Unknown containers remain private, replacement containers lose permission, and
  locked/unopened-loot containers are not accessed. A double chest grants only
  the selected half. Transfers require him within four blocks of that container.

Buttons send bounded, validated actions to the server. Ownership, dimension,
distance, values, and inventory conditions are checked there; the screen cannot
create items or grant access to someone else's companion. The menu closes when
the companion dies, disappears, or goes beyond its 16-block interaction range.
This is the implemented settings subset, not a pretend GUI for unfinished systems.

## Workstations and logical crafting

**Craft Inventory Recipe** uses his first nine storage slots as a row-major 3×3
recipe grid, as in previous builds. If a usable table is not beside him, he searches
for a reachable table within **16 blocks**, walks there, then crafts. If the recipe
stacks change while he travels, he cancels without crafting a different recipe.

**Craft Basic Tools** needs no manually arranged grid. He uses his owned resources
to make planks, sticks, a crafting table when needed, and a pickaxe, axe, and shovel.
Each step is an actual recipe-manager transaction. The button explicitly permits
placing **one owned bench** on a safe, empty, supported spot if no reachable table
exists. Placement spends that inventory item, uses vanilla placement rules, and
checks owner permissions, spawn protection, mobGriefing, and block-use callbacks.
No free table is spawned and no player block is replaced.

With **Basic crafting: On** (default), completing a resource task starts
preparing useful supplies automatically when he owns relevant crafting materials. This automatic job **does not place blocks**:
it uses an existing reachable table, or retains its newly crafted bench as an item
and explains what is missing. Press Craft Basic Tools to authorize placement.

The planner avoids duplicate tool tiers. It can use earned cobblestone, iron ingots,
and diamonds for better tools, preserves older gear when equipping an upgrade,
and recognizes existing netherite tools. It stops with an explanation when wood,
materials, inventory space, a recipe, or a reachable workstation is missing.
It does not smelt raw ore into ingots or invent missing ingredients.

For example, with eight oak logs, no table, and authorized placement, the tested
wooden kit spends exactly **four logs** on one placed bench, one wooden pickaxe,
one wooden axe, and one wooden shovel. It retains the other four logs, one plank,
and two sticks. Inventory capacity is checked before each recipe commits.

## Mining, gathering, and obstruction clearing

**Mine Nearby** searches loaded terrain for exposed, reachable stone/deepslate/ore
within **16 blocks**, favoring ores for which he owns a correct tool. He walks to
it, breaks it over time, collects actual drops, and can continue for up to eight
blocks within the task area/time limit. It does not scan hidden ore through solid
walls, dig shafts, or force chunks to load. Automatic selection avoids blocks below
his feet; the legacy pointed-block mining command remains available.

**Gather Wood** authorizes up to 16 reachable logs within eight blocks of the
assignment position, for at most one minute. Minecraft does not reliably identify
player-placed logs: do not grant this task beside a wooden build you want preserved.

With **Clear leaves: On** (default), an assigned resource task can remove up to
**12 obstructing leaf blocks** to reach its resource. Breaking has real time, loot,
tool wear, and permission/veto checks. Containers, supporting blocks under him,
fluid/falling-block hazards, and arbitrary stone/player structures are not cleared.
Turn the setting off to preserve leaves. It is not an unrestricted tunneling AI.

Resource jobs pause for combat/retreat and cancel on Follow/Stay/Stop/Recall.
Tasks, settings, identity, home, inventory, and equipment serialize across saves.
Unloaded actors pause normally; missing drops are never regenerated. Item pickup
now paths to the actual drop block instead of stopping at an adjacent path node.

## Existing features retained

Following with safe emergency catch-up, defensive/passive/aggressive combat,
owner defense, pursuit limits, low-health retreat, weapon-aware attack intervals,
food consumption, personal item exchange, recipe-backed crafting, home assignment,
shared-container capabilities, and all previous commands remain available.
Commands are now optional: see [the retained command reference](docs/README_0.2.0.md).
Additional commands are `/companion menu`, `/companion prepare`, and
`/companion mine_nearby`.

Previous releases are preserved: [0.1.0](downloads/companion-0.1.0) and
[0.2.0](downloads/companion-0.2.0). Their JARs are unchanged. Earlier documentation
and test reports remain under `docs/`.

## Development and milestone status

Verified toolchain: Minecraft 26.3, Java 25.0.5.0.2+1-LTS-2, Fabric Loader 0.20.1,
Fabric API 0.165.0+26.3, Loom 1.19.3, Gradle 9.8.2. Mojang's unobfuscated class
names are used, without Yarn. No gameplay AI model/API/server is required.

```sh
cd /workspace/MinePal
./scripts/dev.sh --no-daemon build   # compiles and runs required server GameTests
./scripts/headless-client.sh        # real menu/input/network/inventory test
```

Use `scripts/bootstrap.sh` to prepare missing local tooling, and
`scripts/install-headless.sh` for optional local Xvfb/Mesa graphical tests.
On a normal Java 25 workstation, `./gradlew build` and `./gradlew runClient` work
with a graphical display. Checksums and TLS verification stay enabled. Use the
existing checkout; cloud tasks are isolated and do not need another Git worktree.
Previous downloads and source backups are retained, and no Gradle clean is used.

This advances milestone 4's actual resource/crafting/equipment progression and
adds functional portions of milestones 6–7 through interaction/settings screens.
It does not claim every milestone or all 25 specification scenarios are complete.

**Still unfinished:** smelting and complete independent resource planning,
claimed-bed validation, daily routines/sleep, expeditions/signs, personality and
activity-history systems, full scripted dialogue, and remaining settings.
**Death remains permanent:** respawning, keepInventory, and death recovery are
not implemented. Continue using backed-up test worlds. Multiplayer edge cases,
complex cave routes, long survival play, and complete disk upgrade/reload testing
still need manual validation.
