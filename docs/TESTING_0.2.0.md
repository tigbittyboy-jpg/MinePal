# Companion 0.2.0 validation

The original specification, 0.1.0 artifact/instructions, earlier test report, and
all project source were preserved. Before changes, the complete working project
was archived at `/workspace/backups/MinePal-before-0.2.0.tar.gz`. Generated test
run-directory cleanup is now disabled; no Gradle clean was used.

## Executed server checks

`./scripts/dev.sh --no-daemon build` succeeded. **32 required server GameTests
passed: 31 Companion tests plus Fabric's built-in test.** These are development
regression tests, not a claim that all 25 specification scenarios are complete.
No JUnit tests exist; `test NO-SOURCE` is not counted as a passing test suite.

The original 11 Companion tests continue to pass: spawn/attributes, stay,
home navigation, identity/name/home/mode serialization, actual follow movement,
no PvP/friendly attacks, independent inventory/hunger serialization, consuming
real bread, transferring armor into equipment, recipe-backed iron-boots crafting,
and rejected crafting without a table.

The 20 new Companion tests cover:

- Real emergency catch-up onto a valid landing and its cooldown.
- Disabled teleport and stay-mode rejection without movement.
- Magma, solid-body obstruction, and adjacent lava rejection.
- Settings serialization and safe defaults for malformed enum data.
- Owner defense resulting in an actual vanilla melee hit.
- Passive-mode suppression after a threat and critical-health attack interruption.
- Aggressive target selection.
- Registered commands changing actual settings/tasks and rejecting invalid values.
- Private containers and disabled shared access leaving inventory unchanged.
- Real shared deposit/withdraw and replacement-container privacy.
- Insufficient destination capacity leaving both sides unchanged.
- Companion capability and exact block-entity identity persistence.
- Gifted-equipment toggle, worn-tool replacement retaining the old tool, and
  recovering both tools through unequip.
- A real log being removed and its actual drop collected.
- Iron ore removal, raw-iron pickup, and one tool-durability expenditure.
- Refusing diamond ore with an unsuitable tool without destroying it.
- Follow canceling an in-progress resource task before a block is broken.
- A Fabric protection callback vetoing harvesting without drops.
- Task serialization retaining its assignment without awarding its unearned loot.

An early aggressive-target test failed because a modulo tick check could miss
GoalSelector's evaluation phase. Replacing it with a scheduled next-scan time
fixed the actual behavior; the test now passes. Compilation errors from outdated
vanilla constant names were corrected against the downloaded 26.3 APIs. No
checksums, TLS verification, assertions, or tests were disabled to make this pass.

## Client validation

The integrated client test loads a world, synchronizes the equipped humanoid,
captures rendering, executes registered combat/distance/teleport commands on the
integrated server and checks actual state, opens the personal inventory through
the registered command, and waits for the real vanilla container screen. The run passed with
`COMPANION_CLIENT_TEST_PASSED` and `BUILD SUCCESSFUL`. The rendering screenshot
was inspected: the original face/clothing, nameplate, sword, and boots render
correctly. The actual 27-slot inventory screen opened successfully.
Screenshots and logs are retained under `build/reports/companion-client` and
`build/reports/companion-validation-0.2.0`.

## Manual checks still required

Use backed-up test worlds and retain a copy of the 0.1.0 mod:

1. Upgrade a real 0.1.0 world to 0.2.0 with only one Companion JAR active. Check
   identity, name, home, inventory/equipment, owner permissions, and mode after
   saving to disk and reloading. Automated serialization is not this full test.
2. Follow through doors and swimming routes; test blocked-path recovery, hazards,
   follower spacing, leashes, mounts, border edges, and emergency-teleport toggle.
3. Test passive, defensive, and aggressive behavior in normal survival, including
   owner attacks, low-health retreat routes/recovery, chase limits, and slow weapons.
4. Try log gathering in an explicitly permitted area. Verify the 16-block/8-block/
   one-minute limits and interruption during actual threats. Do not grant harvesting
   beside a wooden structure that should remain untouched.
5. Test ore tasks against several tool tiers, durability/enchantments, fluid/gravel
   hazards, blocked paths, inventory-full conditions, owner logout, unload/reload,
   and protection mods. The base iron/tool-veto/protection scenarios are automated.
6. Share one chest half/barrel explicitly, enable access, and transfer stacks while
   another player or hopper also interacts. Test locks, revocation, replacement,
   saving/reloading both actors and containers, and third-party claim integrations.
7. Repeat inventory exchange/disconnection/full-storage tests on a dedicated server
   with multiple owners and companions. No complete anti-duplication audit is claimed.

No autonomous ore search, smelting/progression planner, expedition system,
settings GUI, dialogue/personality system, or respawn/recovery is claimed ready.
