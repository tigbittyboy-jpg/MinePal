# Companion 0.2.0 — development build

[Download the mod JAR](companion-0.2.0.jar?raw=true).
The previous [0.1.0 download](../companion-0.1.0) is unchanged.

Requires **Minecraft Java 26.3**, **Java 25**, **Fabric Loader 0.20.1**, and
**Fabric API 0.165.0+26.3**. Back up your world before upgrading. Move the old
Companion JAR to a backup folder outside `mods/`; only one Companion version can
be active. Put this JAR and Fabric API in `mods/` on both server and clients.

Activate with `/companion spawn`. Existing companions retain the same entity ID
and old save fields; new settings have safe defaults. Test-world upgrading is
recommended; real-world disk upgrade/reload validation is still manual.

New in this build:

- Safe, configurable emergency catch-up; configurable follow distance.
- Passive, defensive, and aggressive combat, owner defense, pursuit limits,
  low-health retreat, and weapon-aware attack intervals. No PvP.
- `/companion gather`: explicitly allow up to 16 reachable logs within eight
  blocks of the assignment point, with a one-minute limit. Do not use beside a
  wooden build you want preserved. He cannot distinguish all placed/natural logs.
- `/companion mine`: mine one selected reachable stone/ore block using a proper
  tool. Actual breaking time, loot-table drops, pickups, and tool wear; no fake ore.
- `/companion share`, `/companion shared on`, `/companion deposit <slot>`, and
  `/companion withdraw <slot>` for explicitly designated chests/barrels only.
  Access is off by default, permission survives saves but not container replacement.
- Safe worn-tool replacement, `/companion unequip`, `/companion stop`, and
  `/companion recall`; persistent per-companion settings and resource assignments.

[Full command reference and build instructions](../../README.md).
[Testing and unfinished work](../../docs/TESTING_0.2.0.md).

**32 required server GameTests passed** (31 mod tests plus the framework test).
The integrated client rendering, inventory GUI, and real server-command test passed.
Those are not the specification's full 25 acceptance scenarios.

**Death is still permanent.** Respawning/keepInventory/recovery, smelting and
independent progression planning, expeditions, full dialogue/settings menus, and
personality/history remain unfinished. Gathering is assigned local work, not a
recurring autonomous expedition. Use a backed-up test world.
