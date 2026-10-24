# MinePal 0.4.0 — Autonomous Survival, Combat & Intelligence Overhaul

**Repository:** https://github.com/tigbittyboy-jpg/MinePal  
**Minecraft:** Java 26.3  
**Framework:** Fabric  
**Current version:** 0.3.0  
**Target version:** 0.4.0

## PROJECT OBJECTIVE

Continue developing MinePal into a genuinely autonomous Minecraft survival companion.

The companion should behave like a competent second player rather than a simple NPC with scripted commands.

He must independently navigate terrain, explore for resources, harvest complete trees, excavate underground materials, craft equipment, assist in combat, recognize danger, and manage his assigned tasks.

**Do not rebuild the project from scratch.**

Inspect the existing source code, architecture, project specification, tests, and release documentation before making changes.

Preserve all functioning features from versions 0.1.0 through 0.3.0.

Prioritize functional intelligence and reliable behavior over cosmetic improvements.

---

# MILESTONE 1 — UNRESTRICTED RESOURCE EXPLORATION

## Problem

MinePal currently has restrictive search distances that prevent him from gathering resources outside a small area.

For example, he may fail to gather wood because trees are too far away.

## Required behavior

Remove arbitrary distance restrictions from resource discovery.

Implement progressive, long-distance exploration.

MinePal must:

- Search nearby loaded chunks first.
- Explore farther when resources cannot be found.
- Navigate toward unexplored terrain.
- Discover resources as new chunks load.
- Remember previously explored locations.
- Avoid repeatedly searching exhausted areas.
- Remember discovered forests, caves, and workstations.
- Continue searching until the task is complete, canceled, or legitimately blocked.
- Recalculate routes when terrain changes.
- Recover from failed navigation attempts.
- Avoid infinite loops.
- Avoid scanning unloaded chunks or forcing unlimited chunk loading.

Exploration should be incremental and performance-conscious.

**Example:**

If I command "Gather Wood" in an empty field, MinePal should travel until he finds trees, harvest the requested amount, and return when appropriate.

He should not stop merely because the nearest tree is 100 blocks away.

Implement sensible resource-search priorities and pathfinding costs.

---

# MILESTONE 2 — COMPLETE TREE HARVESTING

## Problem

MinePal currently harvests only portions of trees, leaving floating logs and unfinished structures.

## Required behavior

Implement complete tree detection and harvesting.

MinePal must:

1. Identify natural tree structures.
2. Detect connected trunk and branch logs.
3. Recognize vertical and horizontal branches.
4. Harvest every reachable log belonging to the selected tree.
5. Look upward for remaining logs.
6. Reposition himself to reach elevated blocks.
7. Use safe temporary access structures when necessary.
8. Remove temporary access blocks where practical.
9. Collect dropped wood.
10. Confirm that harvesting is complete before selecting another tree.

Support common Minecraft tree types, including large and branching varieties.

Use tree-specific heuristics and bounded searches to distinguish natural trees from buildings.

Do not destroy player-built log structures without permission.

Avoid breaking unrelated nearby trees or wooden structures.

A tree should not be considered harvested while identifiable logs remain floating above its original trunk.

---

# MILESTONE 3 — INTELLIGENT MINING AND EXCAVATION

## Problem

MinePal currently mines only exposed stone and ore within a limited distance.

He cannot intelligently dig through dirt or other obstructing blocks to reach underground resources.

## Required behavior

Implement excavation-aware mining.

MinePal must:

- Recognize that stone exists beneath ordinary terrain.
- Safely excavate downward using staircases or other accessible routes.
- Break permitted obstructing blocks.
- Select appropriate tools.
- Mine stone and ore using legitimate Minecraft mechanics.
- Collect mined drops.
- Avoid digging directly downward into unknown hazards.
- Detect dangerous falls, lava, water, and unstable terrain.
- Navigate around dangerous obstacles.
- Recalculate excavation routes when necessary.
- Continue mining until the requested quantity is collected.
- Explore new areas when local resources are exhausted.

**Example:**

If I command "Mine Stone" while standing in a grassy field, MinePal should locate a suitable excavation point, dig safely through dirt, reach stone, and begin mining.

He should not require exposed stone to begin working.

### Mining intelligence

Implement a reusable excavation planner capable of:

- Identifying reachable targets.
- Planning permitted block removal.
- Estimating excavation cost.
- Selecting appropriate tools.
- Maintaining safe routes.
- Avoiding unnecessary destruction.
- Recovering from blocked paths.

This system should eventually support underground expeditions and ore mining.

---

# MILESTONE 4 — AUTONOMOUS CRAFTING AND WORKSTATIONS

## Problem

MinePal currently searches for crafting tables within approximately 16 blocks.

## Required behavior

Remove this restrictive search radius.

MinePal should:

1. Check remembered crafting-table locations.
2. Search nearby loaded areas.
3. Navigate to distant known workstations.
4. Explore for workstations when necessary.
5. Determine whether crafting a new table would be more efficient.
6. Craft a table using legitimately owned resources.
7. Place the table when appropriate.
8. Use the table for crafting.
9. Remember its location.
10. Recover temporary crafting tables when practical.

Preserve existing crafting capabilities.

Maintain automatic equipment upgrades.

Never duplicate resources or generate items without legitimate crafting inputs.

---

# MILESTONE 5 — AUTOMATIC COMBAT ASSISTANCE

## Problem

MinePal currently does not reliably fight alongside the player.

He may ignore attacks against animals, neutral mobs, or hostile mobs.

## Required behavior

**Whenever I attack a living entity, MinePal should recognize that entity as my target and automatically assist.**

This includes:

- Zombies
- Skeletons
- Spiders
- Creepers
- Goats
- Cows
- Sheep
- Pigs
- Chickens
- Other supported living entities

MinePal should not require a separate combat command.

### Target recognition

Implement server-side detection of player attacks.

When the owner attacks an entity:

1. Register the attacked entity as the owner's current combat target.
2. Determine whether combat assistance is permitted.
3. Interrupt lower-priority tasks when appropriate.
4. Equip the best available weapon.
5. Navigate toward the target.
6. Attack using legitimate Minecraft mechanics.
7. Pursue the target while doing so remains safe.
8. Stop when the target dies, becomes invalid, or combat is canceled.
9. Resume the previous task.

**Example:**

If I punch a goat, MinePal should immediately join the fight and attack that goat.

He should not ignore it because goats are normally passive or neutral.

### Combat mechanics

Implement:

- Realistic melee attack range.
- Minecraft attack cooldowns.
- Weapon damage calculations.
- Weapon durability.
- Automatic weapon selection.
- Target pursuit.
- Knockback handling.
- Repositioning during combat.
- Basic obstacle navigation.
- Avoidance of dangerous terrain.
- Protection against accidental friendly fire.

Do not simulate combat by deleting entities or applying arbitrary damage.

MinePal must physically approach and attack targets.

### Defensive behavior

MinePal must automatically recognize threats to himself and his owner.

He should:

- Defend himself when attacked.
- Defend the owner against attacking mobs.
- Prioritize immediate threats.
- Avoid attacking neutral mobs without provocation unless explicitly ordered.
- Resume interrupted activities when combat ends.

### Combat settings

Implement three combat modes:

**Passive**
- Never initiate combat.
- Defend himself when necessary.

**Defensive**
- Defend himself and the owner.
- Do not automatically attack peaceful animals.

**Aggressive**
- Defend himself.
- Defend the owner.
- Automatically attack any valid living entity the owner attacks.

Default to Aggressive.

Persist combat settings across world reloads.

Player-versus-player assistance must be disabled by default and separately configurable.

Never attack the owner or registered friendly companions.

---

# MILESTONE 6 — SURVIVAL-AWARE COMBAT AND RETREAT

## Objective

MinePal should recognize when he is losing a fight and make intelligent decisions about survival.

He should not blindly fight until death.

This is especially important because companion death is currently permanent.

### Health awareness

Continuously evaluate:

- Current health.
- Maximum health.
- Available armor.
- Equipped weapon.
- Nearby enemies.
- Incoming damage.
- Environmental hazards.
- Available escape routes.
- Available healing resources.

### Retreat behavior

When MinePal's survival is seriously threatened, he should:

1. Recognize the danger.
2. Stop aggressive pursuit.
3. Identify a safe escape direction.
4. Retreat away from dangerous enemies.
5. Avoid lava, cliffs, and other hazards.
6. Seek temporary shelter when practical.
7. Use legitimate healing resources when available.
8. Resume normal behavior only when safe.

### Example

If MinePal has three hearts remaining and is fighting an iron golem, he should recognize the overwhelming threat and attempt to escape.

He should not continue attacking until he dies.

### Threat evaluation

Implement a threat-scoring system based on:

- Enemy type.
- Enemy count.
- Current health.
- Recent damage received.
- Armor protection.
- Weapon effectiveness.
- Distance to enemies.
- Escape-route availability.

Avoid relying exclusively on fixed health thresholds.

A companion with half health fighting a chicken is in a very different situation from one with half health fighting several zombies.

### Combat priorities

Use the following priorities:

1. Escape immediately life-threatening situations.
2. Defend against immediate threats.
3. Protect the owner.
4. Assist with the owner's selected target.
5. Resume assigned work.

### Additional requirements

- Retreat should interrupt combat immediately when necessary.
- Avoid repeatedly switching between fighting and fleeing.
- Use cooldowns and hysteresis to prevent unstable behavior.
- Do not retreat endlessly from harmless enemies.
- Do not teleport to safety.
- Do not grant artificial invulnerability.
- Preserve realistic survival mechanics.

MinePal should value survival without becoming too afraid to fight ordinary enemies.

---

# MILESTONE 7 — SIMPLIFIED COMPANION INTERFACE

## Problem

The current right-click interface is overly complicated.

Too many options are exposed simultaneously.

## Required behavior

Redesign the interface to feel like a polished, native Minecraft menu.

### Primary menu

Display only the most important actions:

- Follow Me
- Stay Here
- Gather Resources
- Craft Equipment
- Inventory
- More Options

Move advanced settings into secondary menus.

### Companion status

Display essential information:

**MinePal**

Health: 20/20  
Hunger: 18/20  
Current Task: Gathering Oak Logs  
Progress: 14/32  
Status: Searching for Trees  
Combat Mode: Aggressive

### Interface requirements

- Clear visual hierarchy.
- Consistent spacing.
- Readable text.
- No overlapping elements.
- Minimal unnecessary buttons.
- Logical menu navigation.
- Visible task progress.
- Simple task cancellation.
- Persistent companion naming.
- Existing inventory and storage functionality.
- Easy access to combat settings.

Do not remove functioning features simply to reduce menu complexity.

Instead, organize them intelligently.

---

# MILESTONE 8 — PERSISTENT TASK INTELLIGENCE

Implement a unified task-management system.

Tasks should have clearly defined states:

- Planning
- Searching
- Traveling
- Excavating
- Clearing Obstructions
- Working
- Collecting
- Fighting
- Retreating
- Returning
- Completed
- Blocked
- Canceled

### Task interruption

MinePal must be able to interrupt work temporarily.

Example:

1. I command MinePal to gather 64 logs.
2. He begins chopping trees.
3. A zombie attacks me.
4. MinePal stops gathering and fights the zombie.
5. The zombie dies.
6. MinePal returns to gathering wood.
7. He finishes collecting 64 logs.

Do not restart the entire task unnecessarily.

### Task persistence

Tasks should survive ordinary interruptions and world reloads when appropriate.

Store:

- Current objective.
- Requested quantity.
- Collected quantity.
- Previous task state.
- Known resource locations.
- Known workstation locations.
- Exploration progress.
- Temporary task structures.
- Combat settings.

Prevent infinite retries, task duplication, and deadlocked behavior.

For open-ended gathering commands, continue until canceled or legitimately blocked.

---

# MILESTONE 9 — TESTING AND RELIABILITY

Preserve the existing test suite.

Add comprehensive tests for all new behavior.

### Exploration tests

- Finding distant resources.
- Traveling across loaded chunks.
- Exploring new terrain.
- Remembering searched locations.
- Recovering from unreachable targets.
- Avoiding infinite searches.

### Tree harvesting tests

- Complete oak harvesting.
- Tall trees.
- Branching trees.
- Elevated logs.
- Drop collection.
- Avoiding unrelated structures.

### Mining tests

- Excavating through dirt.
- Reaching buried stone.
- Clearing obstructions.
- Tool selection.
- Avoiding dangerous excavation.
- Recovering from blocked routes.

### Combat tests

- Attacking a goat after the owner punches it.
- Attacking hostile mobs.
- Defending the owner.
- Using realistic attack cooldowns.
- Pursuing moving targets.
- Preventing friendly fire.
- Resuming interrupted tasks.

### Survival tests

- Retreating at critically low health.
- Recognizing overwhelming enemies.
- Avoiding environmental hazards.
- Preventing fight/retreat loops.
- Resuming normal activity after danger passes.

### UI tests

- Correct menu navigation.
- Persistent settings.
- Inventory functionality.
- Task cancellation.
- No overlapping controls.

### Regression tests

All previously functioning features must continue working.

Keep gameplay server-authoritative and compatible with the existing Fabric environment.

---

# DEVELOPMENT REQUIREMENTS

Follow these rules throughout implementation:

1. Inspect existing code before modifying it.
2. Preserve the existing project specification.
3. Do not rebuild working systems unnecessarily.
4. Use modular, maintainable architecture.
5. Avoid introducing conflicting AI controllers.
6. Prioritize performance and stability.
7. Keep all resource gathering legitimate.
8. Never duplicate items.
9. Never teleport the companion to solve navigation problems.
10. Never silently abandon tasks.
11. Test each milestone independently.
12. Fix discovered regressions before release.
13. Do not claim unimplemented features are complete.
14. Document any remaining limitations.

If the entire scope cannot be completed reliably in one development pass, implement it incrementally, clearly report which milestones are complete, and continue from the existing architecture.

Do not replace unfinished features with superficial demonstrations.

---

# RELEASE REQUIREMENTS

Prepare MinePal 0.4.0 with:

- Compiled Fabric JAR.
- Updated source code.
- Updated automated tests.
- Test results.
- Release notes.
- Installation instructions.
- Updated UI screenshots.
- Documented known limitations.

Keep previous release artifacts intact.

Do not overwrite earlier versions.

## FINAL DESIGN PHILOSOPHY

MinePal should feel like a second survival player.

If I tell him to gather wood, he should find trees and harvest them completely.

If I tell him to mine stone, he should figure out how to reach stone even when it is underground.

If I attack an animal or monster, he should help me fight it.

If he is about to die, he should recognize the danger and attempt to survive.

If I assign him work and something interrupts him, he should eventually return to that work.

**The ultimate objective is a companion that can independently reason through ordinary Minecraft survival tasks without requiring constant player supervision.**

Implement real functionality, not the appearance of functionality.