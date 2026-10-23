# Companion — Minecraft Java 26.3

An offline, deterministic Fabric survival companion mod (`companion`). **Current
build: 0.2.0.** The [original specification](docs/PROJECT_SPECIFICATION.md) is unchanged.
The complete 0.1.0 download and its instructions remain in
[`downloads/companion-0.1.0`](downloads/companion-0.1.0); its earlier development
README is retained in [docs/README_0.1.0.md](docs/README_0.1.0.md).

## Download and install

Download [`companion-0.2.0.jar`](downloads/companion-0.2.0/companion-0.2.0.jar).
Back up your world first and use a test world. Requirements are unchanged:
Minecraft Java **26.3**, Java **25**, Fabric Loader **0.20.1**, and
Fabric API **0.165.0+26.3**. Place Companion and Fabric API in `mods/`; on a
multiplayer server both the server and clients need them.

Keep only one Companion version active: move 0.1.0 to a backup folder outside
`mods/`, then install 0.2.0. Do not install the `-sources.jar`. Existing companions
use the same entity ID and save keys; new settings have safe defaults when loading
older data. Full real-world upgrade/reload testing remains manual.

In a world with commands enabled, run `/companion spawn`. Right-click your
companion to toggle follow/stay; sneak-right-click to stay. Gameplay makes no AI
API requests and requires no external AI service.

## Commands

Commands select your nearest loaded owned companion within 64 blocks. Settings
are per-companion and saved. Unknown containers are private by default.

| Command | Behavior |
| --- | --- |
| `/companion spawn` | Spawn and own a companion; prevents another nearby loaded owned spawn. |
| `/companion follow` | Follow the owner; cancel nonessential resource work. |
| `/companion stay` or `/companion stop` | Cancel the task and wait without erasing inventory, home, identity, or settings. |
| `/companion home` | Assign the bed you are looking at, within six blocks. |
| `/companion return` or `/companion recall` | Cancel the task and walk home. |
| `/companion name Rowan` | Set a persistent name (1–32 characters). |
| `/companion status` | Report real current activity, hunger, home, and settings. |
| `/companion inventory` | Open personal storage; only the owner can exchange items. |
| `/companion equip` | Equip suitable stored gear, respecting gifted-equipment setting. |
| `/companion unequip` | Recover all equipped items into personal storage; fails without moving anything if storage is full. |
| `/companion craft` | Craft once from the first nine storage slots in row-major 3×3 order, beside a crafting table. |
| `/companion gather` | Explicitly authorize a bounded local log-harvesting task. |
| `/companion mine` | Mine the stone/ore block you point at, if reachable and properly equipped. |
| `/companion settings` | List current server-side settings. |
| `/companion combat passive\|defensive\|aggressive` | Avoid hostiles, defend self/owner, or engage nearby hostiles. Default defensive. |
| `/companion distance close\|normal\|relaxed` | Follow at approximately 2, 4, or 6 blocks. Default normal. |
| `/companion teleport on\|off` | Enable/disable safe emergency catch-up. Default on. |
| `/companion gifted on\|off` | Enable/disable automatic equipment selection/replacement. Default on. Already-equipped gear remains equipped. |
| `/companion gathering on\|off` | Permit/disable assigned log harvesting. Default on. |
| `/companion mining on\|off` | Permit/disable assigned mining. Default on. |
| `/companion share` | Designate the chest/barrel you point at as a shared container. |
| `/companion shared on\|off` | Enable/disable access to the designated container. Default off. |
| `/companion unshare` | Revoke the container designation. |
| `/companion deposit <slot>` | Move a whole personal-storage stack (slot 1–27) to shared storage, if all of it fits. |
| `/companion withdraw <slot>` | Move a whole designated-container stack (slot 1–27) to personal storage, if all of it fits. |

Choose one alternative in commands containing `|`; do not type the bars.
Commands are the current settings interface; a graphical settings/dialogue menu
has not been implemented.

## What's new in 0.2.0

Following now has loaded-terrain-only emergency catch-up with collision, hazards,
world-border, and cooldown checks. It activates beyond 32 blocks, or after repeated
path failures beyond 12 blocks. It does not run during combat/critical-health
retreat, while leashed/riding, in stay mode, or when disabled. No chunks are forced
and dimensions are not crossed. Navigation retries use bounded backoff.

Combat now supports passive/defensive/aggressive modes, recent threats to the
owner, pursuit limits, and low-health retreat with recovery hysteresis. Players
and friendly non-hostile entities are never attack targets. Attacks use vanilla
melee mechanics with a cooldown no shorter than the held weapon's attack speed
or the normal mob attack interval.

Resource tasks use actual blocks and loot tables. `/companion gather` grants
permission to break **up to 16 reachable logs within eight blocks of the assignment
position, for at most one minute**. Do not issue it beside a wooden build you want
to preserve: Minecraft does not reliably distinguish player-placed logs from
natural ones. Gathering is assigned, not yet a recurring autonomous home routine.
`/companion mine` works on **one selected stone/deepslate/ore block**, within six
blocks of the player and eight blocks of the companion. It needs a correct tool
where vanilla requires one, and rejects immediate fluid/falling-block hazards,
containers, blocks under the companion's feet, and inaccessible paths.

Harvesting takes time, applies tool durability, uses vanilla loot/enchantments,
respects mobGriefing, spawn protection, adventure restrictions, and Fabric's block
break veto event. Haste/fatigue and airborne/underwater penalties apply; advanced
mining-speed modifiers are not all implemented. The companion picks up only the
actual drops tracked from his own harvest, not arbitrary nearby player items.
Tasks persist across entity serialization, pause during threats, and stop on
follow/stay/recall commands. Missing/unloaded drops are never regenerated.

Shared access requires both an explicit designation and the enabled setting.
The companion must be within four blocks of the selected unlocked, previously
opened chest/barrel. Permission is tied to the exact container's persisted identity;
a replacement defaults to private. A double chest grants only the selected half.
Whole-stack transfers check capacity before committing. This is explicit exchange,
not yet autonomous chest logistics. Other mods' complete claim/protection behavior
still needs compatibility testing.

Near-broken equipment can be replaced with a healthier stored copy of the same
item type; the old item is retained in storage. Existing good gear is not silently
discarded. Simplified hunger/eating and recipe-backed crafting remain from 0.1.0.

## Build and tests

Verified versions: Minecraft 26.3, Java 25.0.5.0.2+1-LTS-2, Fabric Loader 0.20.1,
Fabric API 0.165.0+26.3, Loom 1.19.3, Gradle 9.8.2. Minecraft uses Mojang's
unobfuscated class names; no Yarn dependency is used.

```sh
cd /workspace/MinePal
./scripts/dev.sh --no-daemon build     # builds JAR and runs server GameTests
./scripts/headless-client.sh          # integrated client/GUI/command test
```

For a new machine, `scripts/bootstrap.sh` installs a checksum-verified local JDK
and builds. `scripts/install-headless.sh` optionally prepares signed Debian Xvfb
and Mesa packages extracted locally without replacing system packages. On a normal
Java 25 workstation, use `./gradlew build` and `./gradlew runClient` with a display.
The Gradle wrapper's distribution checksum is pinned. TLS verification stays on.

Use the existing checkout: cloud tasks are already isolated, so no additional
Git worktree is needed. No cleanup or Gradle `clean` is required. Generated test
run directories are retained (`clearRunDirectory=false`); previous downloads are
kept. Run graphical/server test tasks sequentially. See
[the 0.2.0 test report](docs/TESTING_0.2.0.md) for executed checks and manual gaps.

## Milestones and limitations

Milestones 1–3 now have functional foundation/following, survival inventory,
crafting, equipment, and explicit storage exchange. Milestone 4 has started with
bounded real-resource tasks and failure recovery; automatic equipment planning,
smelting, and independent resource goals remain unfinished. This does not claim
all milestone acceptance conditions or the specification's 25 scenarios pass.

**Death is still permanent.** Respawning, keepInventory, death recovery, claimed-bed
validation, sleep, daily routines, expeditions, signs, contextual dialogue,
personality/history interfaces, and the full settings GUI are later milestones.
Unloaded actors pause normally; cross-dimension following and recall are not
implemented. Larger terrain, swimming/door routes, combat balance, multiplayer
transactions, disk save/reload, and installation upgrades still need manual tests.
