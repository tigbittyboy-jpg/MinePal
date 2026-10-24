# MinePal 0.4.0 verification

## Executed checks

- `./scripts/dev.sh --no-daemon build`: BUILD SUCCESSFUL; all **66 required server GameTests passed**, including the retained framework test. Common, client and GameTest sources compiled against Minecraft 26.3 and Java 25.
- `./scripts/headless-client.sh`: BUILD SUCCESSFUL; real integrated client and server. Both COMPANION_MENU_TEST_PASSED and COMPANION_CLIENT_TEST_PASSED markers recorded.
- Captured and visually inspected the main menu, resources, combat/settings and storage screenshots. Six primary choices have nonoverlapping bounds. Tests enter quantity 8, assign/cancel a real task, change modes, rename, follow/stay, open inventory and transfer three real diamonds without duplication. Closing stays closed.

## Server coverage

Original 47-test baseline passed before expansion. Nineteen added tests cover real owner-goat combat with sword wear; target/friendly exclusions; work resumption after fighting; relative chicken/golem threat and walking retreat; ledger/memory/settings serialization; complete connected oak branches and unrelated-log preservation; rejection of a simple log building; staircase dirt-to-stone mining and multi-segment requested quantities; support/lava safety; retained empty-search assignments; physical travel to a previously unknown tree 108 blocks away; walking to a remembered table 90 blocks away and crafting boots; server attack-intent callback without synthetic damage; saved quantity metadata; unloaded-drop blocking; unresolved tall-crown status; and deferred tree-frontier reconciliation.

Existing tests remain active. Requirement-driven adaptations: legacy teleport test now asserts no teleport; Passive permits self-defense while never initiating; malformed/new combat settings default to Aggressive. Arena padding separates aggressive-default actors. No tests are disabled. Tree test leaves explicitly use natural distance 1 so fixture trees survive until discovery.

Development failures were fixed before release: an owner-null threat classification, tree-drop pickup interrupting unfinished branch harvesting, stale test references after inventory stack splitting, and a fixture canopy decaying before distant exploration. Later checks found unloaded exploration frontiers and tree frontiers needed explicit waiting/reconciliation; coverage now exercises these cases.

## Evidence and practical limits

Full server/client logs are packaged beside this report in the release folder. The graphical test used local Xvfb and Mesa lavapipe Vulkan. Optional OpenGL probing and account-service discovery warnings occurred; offline tests still completed. This does not establish live Mojang account-service connectivity. Required build artifacts resolved successfully from the configured dependency environment/cache; no fresh external-service availability claim is made.

Save tests cover serialization and restored controller data, not a complete long-lived disk upgrade/playthrough. PvP configuration and friendly exclusions are tested, but comprehensive multiplayer/PvP combat, all mob species, moving-target terrain combinations, every tree species, and long survival soak remain manual work. No scaffold placement, bench recovery, shelter construction, forced chunk loading, smelting or death recovery is claimed. Tall crowns correctly remain blocked. See README limitations and milestone status.
