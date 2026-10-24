# Companion 0.3.0 validation

## Preservation and build

The original specification and prior release folders are preserved. The project
was archived before changes at `/workspace/backups/MinePal-before-0.3.0.tar.gz`.
The previous README is retained as `docs/README_0.2.0.md`. No Gradle clean or
release deletion was used.

`./scripts/dev.sh --no-daemon build` succeeded. **47 required server GameTests
passed: 46 Companion tests and Fabric's built-in test.** This includes the prior
32-test regression suite. Gradle's `test NO-SOURCE` is not counted as JUnit tests.
These are not a claim that the specification's entire 25-scenario list passes.

## Added server tests

- Walk to a table initially more than three blocks away, craft real boots, and
  consume the required ingots.
- Turn eight owned oak logs into a real placed bench and wooden pickaxe/axe/shovel,
  spending exactly four logs and retaining one plank, two sticks, and four logs.
- Automatic preparation retains its owned bench instead of placing without approval.
- Insufficient inventory capacity preserves the entire input transaction.
- Use seven earned iron ingots for three real iron tools, without inferior duplicates.
- Persist a work assignment and new settings without performing crafting on load.
- Following cancels an in-progress workstation job without consuming ingredients.
- Reject other players' menu actions and invalid shared-storage slot values.
- Equip an earned stone-tool upgrade while retaining the previous wooden tool.
- Physically remove blocking leaves and collect an actual log.
- Disabled clearing leaves leaf blocks unchanged.
- Nearby mining finds, approaches, breaks, and collects ore without a pointed target.
- Actual wood harvesting triggers real supply/tool preparation at a nearby table.
- A protection veto on an obstruction prevents clearing and unearned harvesting.
- Actual nearest stone mining yields three cobbles, which are consumed in a real
  stone-pickaxe recipe; the new pick is equipped and the prior wooden pick is retained.

## Diagnosed issues

Early searches could reject a newly spawned actor before its first physics tick
made navigation available. Reachability is now deferred in that initial state.
Pickup pathfinding could also stop at an adjacent node just beyond item reach,
causing ore collection and subsequent crafting to stall. Collection now paths to
the drop's actual block with zero target-node tolerance. The complete server suite
passed after these fixes; assertions and required tests were not disabled.

Obstruction tests require the actually blocking leaf to be removed; they do not
require unnecessary leaves to be cleared after the route becomes available. A
separate veto test verifies protected obstructions remain untouched.

## Client/UI validation

The integrated client test uses the actual right-click interaction to open the
custom menu over the network. It clicks Follow/Stay and setting buttons, verifies
server-side behavior/state, edits and saves the companion name, opens the storage
page, and checks the real vanilla inventory screen. It retains the previous
rendering/command tests. The final run additionally validates real menu-driven
withdrawal/deposit and that delayed updates do not reopen a closed menu.

Screenshots are inspected for readable vanilla-style buttons, status feedback,
page navigation, name/storage inputs, and intact humanoid/equipment rendering.
Logs and images are retained under `build/reports/companion-validation-0.3.0`.
The integrated client run passed with both `COMPANION_MENU_TEST_PASSED` and
`COMPANION_CLIENT_TEST_PASSED`, followed by `BUILD SUCCESSFUL`. Shared withdrawal
and deposit moved the actual three-diamond test stack in both directions without
changing its total. Closing the screen was also checked against delayed refreshes.

## Manual validation still needed

- Upgrade a backed-up real 0.2.0 world and save/reload it on disk. Identity/data
  serialization is automated; that is not a complete world-upgrade test.
- Use the menu at different GUI scales/window sizes, with keyboard navigation,
  screen resizing, latency, disconnections, and several owners/companions.
- Test storage transfers during concurrent player/hopper actions on a dedicated
  server and with third-party lock/claim mods.
- Test table searches with enclosed/unreachable tables and complex terrain, table
  destruction during travel, and inventory edits while a queued recipe is pending.
- Test many wood species, mixed plank types, custom/datapack recipes, full storage,
  gifted/enhanced equipment, and permission denial during owned-bench placement.
- Test leaf clearing and ore selection in real forests/caves near liquids, gravel,
  protected areas, and player builds. Gathering permissions are explicit and bounded,
  not an automatic classification of player versus natural structures.
- Continue terrain, swimming/doors, combat balance, and long survival testing.

Smelting, complete autonomous planning, bed/routine systems, expeditions, personality/
history, remaining settings, respawning, and death-item recovery are not verified
or claimed implemented.
