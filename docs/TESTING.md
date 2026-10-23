# Validation report

## Network and toolchain

These exact read-only requests returned HTTP 200 after permissions were updated:

- https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
- https://meta.fabricmc.net/v2/versions/game
- https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml
- https://repo.maven.apache.org/maven2/org/ow2/asm/asm/maven-metadata.xml
- https://services.gradle.org/versions/current

The official manifest identifies 26.3 as the current release and requires Java
25. Fabric lists 26.3 stable. Loom, Loader, Fabric API, Minecraft libraries, and
Gradle resolved successfully; compilation used actual downloaded APIs. The
bootstrap JDK and Gradle distribution were SHA-256 verified. Minecraft client
was independently checked against its official SHA-1 metadata during inspection.

## Automated tests

`./scripts/dev.sh --no-daemon build runGameTest` passed: **12 required server tests**,
comprising 11 Companion tests and Fabric's built-in test. No JUnit tests exist;
Gradle's `test NO-SOURCE` is not counted as validation.

Companion tests exercise:

1. Entity registration, spawn, and health attributes.
2. Remaining in place after a stay command.
3. Actual pathfinding and movement toward home.
4. UUID, name, mode, and home serialization round trip.
5. Actual follow movement toward a mock server player.
6. Refusal to attack players or other companions.
7. Independent inventory and hunger serialization.
8. Eating actual bread, applying its nutrition, and decrementing the stack.
9. Moving iron boots from inventory into equipment without copying them.
10. Crafting boots from four actual ingots via the recipe manager.
11. Rejecting crafting without a workstation while preserving resources.

`./scripts/headless-client.sh` **passed** the integrated client test and printed
`COMPANION_CLIENT_TEST_PASSED`, followed by `BUILD SUCCESSFUL`. It runs the
integrated client test. It loads a world,
spawns an owned companion with boots and a sword, waits for entity synchronization,
captures the rendered scene, opens the real owner inventory menu, waits for the
vanilla container screen, and captures it. Inspect screenshots before claiming
visual quality; merely having compiled a renderer does not prove its appearance.
The captured images were inspected: the humanoid, nameplate, jacket, boots, held
sword, and vanilla 27-slot inventory screen are visible. The scene shows the
companion from behind, so its face and all animation transitions still require
further visual review. Retained screenshots are in `build/reports/companion-client/`.

The initial graphical attempt failed: no matching OpenGL GLX visual and no Vulkan
surface extension. Installing local Mesa lavapipe and selecting its Vulkan ICD
allowed the integrated client test to run. Offline Mojang account discovery and
missing audio hardware emitted warnings; these are separate from mod behavior.

## Manual acceptance procedures still required

Use a backed-up creative test world with cheats, then repeat in survival:

1. Spawn and name a companion; inspect skin, nameplate, armor, held tool, damage
   tint, walking, head tracking, running, and swimming.
2. Follow through wooden doors, around walls, across shallow water, and near lava
   and cliffs. Stay must stop travel; block the route and observe bounded retries.
3. Look at a bed, set home, move away, and return. Repeat after destroying the bed
   to evaluate the documented missing bed-invalidation behavior.
4. Transfer stacks, shift-click in both directions, disconnect while the menu is
   open, move out of range, and have another player attempt access. Recount items.
5. Gift weapons, armor, bread, and soup. Check consumed items, returned bowls,
   armor visibility, durability, and preservation of existing equipment.
6. Place a recipe in slots 1–9 (top inventory row), craft beside a table, and count
   output and ingredients. Test full inventory, invalid recipes, and remainders.
7. Save/reload the world and verify name, ownership, inventory, equipment, hunger,
   home, and mode. Serialization is automated; a full disk world reload is not.
8. Spawn multiple companions owned by different players; verify commands select
   only the nearest owned actor and inventories do not cross-contaminate.
9. Trigger a hostile attack and verify melee retaliation without PvP. Owner
   protection, retreat, and selectable combat modes are not implemented.

Specification scenarios 8–9, 13–20, and 22–23 require unfinished features and are
not passed. Full scenario 25 (duplication resistance) requires multiplayer and
failure-path testing beyond the successful inventory/crafting tests. No claim
is made that all 25 specification scenarios pass.
