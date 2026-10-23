# CODEX PROJECT: MINECRAFT AUTONOMOUS SURVIVAL COMPANION

## PROJECT OVERVIEW

Develop a complete, functional Minecraft Java Edition mod introducing a persistent, autonomous humanoid companion NPC.

**Target Minecraft version:** Java Edition 26.3  
**Preferred mod loader:** Fabric  
**Programming language:** Java  
**Mod name:** Companion  
**Mod ID:** `companion`

The companion should feel like a believable character that belongs in vanilla Minecraft.

He is not a pet, a glorified follower, an overpowered assistant, or an AI chatbot.

He should feel like another survival player inhabiting the world, with his own inventory, equipment, priorities, routines, personality, and progression.

The player should be able to adventure alongside him, give him resources, issue commands, and interact through predefined dialogue.

He should also be capable of surviving and accomplishing useful tasks independently.

**CRITICAL: No language models, AI APIs, generated conversations, external servers, or internet connectivity should be required during gameplay.**

All decision-making must use deterministic, game-state-driven systems.

---

# 1. DEVELOPMENT REQUIREMENTS

Before implementing the mod:

1. Verify that Minecraft Java 26.3 is available and identify its correct modding APIs.
2. Verify Fabric Loader, Fabric API, mappings, Java, Gradle, and Loom compatibility.
3. Use the correct APIs for the target Minecraft version.
4. Do not invent classes, methods, dependencies, or mappings.
5. Generate a complete Gradle project.
6. Use a maintainable, modular architecture.
7. Compile the project and resolve all build errors.
8. Provide a working development environment and build instructions.

If any required dependency is unavailable, identify a compatible alternative rather than silently using an incompatible version.

Do not claim successful compilation unless the build actually succeeds.

The project must produce an installable `.jar` file.

---

# 2. COMPANION APPEARANCE

Create a humanoid companion that visually belongs in vanilla Minecraft.

### Visual requirements

- Standard Minecraft pixel-art aesthetic.
- Approximately player-sized.
- Unique appearance rather than a direct Steve clone.
- Simple, recognizable clothing.
- Vanilla-compatible proportions.
- Naturally animated arms, legs, and head.
- Appropriate walking, running, swimming, and idle animations.
- Visible equipped armor and held tools.
- Vanilla-style damage effects.
- Subtle head tracking when interacting with the player.

Avoid exaggerated animations, detailed realistic textures, or visual effects that clash with Minecraft.

The companion should look like a character Mojang could plausibly introduce in a vanilla update.

### Naming

Allow the player to name the companion.

Display the name above his head using a vanilla-style nameplate.

Names must persist across saves, deaths, and respawns.

---

# 3. COMPANION AI ARCHITECTURE

Create a modular behavior system using a finite-state machine, utility-based priorities, or another appropriate deterministic architecture.

The companion must have clearly defined states.

Examples:

- IDLE
- FOLLOWING
- STAYING
- RETURNING_HOME
- GATHERING
- MINING
- CRAFTING
- SMELTING
- EXPLORING
- FIGHTING
- FLEEING
- EATING
- SLEEPING
- RECOVERING_ITEMS
- RESPAWNING

Each state must have:

- Entry conditions.
- Execution logic.
- Completion conditions.
- Failure conditions.
- Recovery behavior.
- Appropriate interruption rules.

Do not allow incompatible behaviors to execute simultaneously.

Use a task scheduler with priorities.

### Priority order

1. Prevent imminent death.
2. Handle immediate threats.
3. Obey urgent player commands.
4. Maintain follower mode when enabled.
5. Return home when required.
6. Recover from injuries or resource shortages.
7. Complete assigned tasks.
8. Pursue independent survival goals.
9. Perform idle behaviors.

Avoid constant state switching, infinite task loops, or rapid changes in goals.

Implement cooldowns and safeguards against repeated failures.

---

# 4. AUTONOMOUS SURVIVAL

The companion must manage his own survival needs.

He should be able to:

- Monitor health.
- Monitor hunger.
- Eat food from his inventory.
- Recognize dangerous situations.
- Avoid unnecessary combat.
- Retreat when severely injured.
- Seek shelter during dangerous conditions.
- Equip appropriate tools and armor.
- Gather resources for personal use.
- Craft useful equipment.
- Improve his equipment over time.

He should not possess unlimited resources.

He must follow Minecraft's actual gameplay rules.

For example, crafting iron armor requires the necessary iron ingots and an appropriate crafting environment.

Do not simulate successful crafting by spawning finished items.

### Survival progression

The companion should gradually improve his equipment.

Example progression:

Wooden tools → Stone tools → Iron tools → Diamond equipment.

Progression should depend on actual resource availability.

He should not magically discover resource locations or receive equipment without earning it.

---

# 5. RESOURCE GATHERING

Implement functional resource gathering.

The companion should be able to:

- Locate nearby trees.
- Navigate to reachable logs.
- Break blocks using legitimate game mechanics.
- Collect dropped items.
- Gather stone.
- Mine accessible ores.
- Collect coal and iron.
- Gather basic food.
- Return home with resources.

### Resource ownership

The companion must have his own inventory.

Resources obtained independently belong to him.

Resources gifted by the player also become part of his inventory.

He must never automatically take items from player-owned chests.

Implement three storage categories:

**PRIVATE**

Player-owned storage.

Never accessible without explicit permission.

**SHARED**

Storage specifically designated by the player.

The companion may use it only when shared storage access is enabled.

**PERSONAL**

The companion's own inventory and designated storage.

The companion may use these resources for his own activities.

Unknown containers must be treated as private by default.

Do not assume that a chest is shared merely because it is near the companion's home.

---

# 6. CRAFTING AND EQUIPMENT

The companion should recognize equipment deficiencies and attempt to resolve them.

Example:

The companion wants iron boots.

1. Checks whether he already owns iron boots.
2. Checks available iron ingots.
3. Calculates the missing resources.
4. Searches for accessible iron ore.
5. Mines the ore.
6. Returns home.
7. Smelts the raw iron.
8. Crafts the boots.
9. Equips them.

Each step must use real game mechanics.

If a step fails, the companion should either recover or abandon the task with a meaningful explanation.

Implement tool durability checks.

The companion should replace damaged tools when resources permit.

Avoid wasteful crafting and unnecessary duplication.

---

# 7. HOME BASE AND DAILY ROUTINE

The companion should be primarily home-based.

He should not constantly wander away.

Allow him to claim a bed as his home.

His home position should persist across world reloads.

### Daily activities

During normal independent mode, he may:

- Walk around his home.
- Inspect nearby surroundings.
- Craft equipment.
- Manage personal resources.
- Gather nearby materials.
- Rest.
- Sleep.
- Perform occasional idle animations.
- Prepare for future expeditions.

He should spend most of his independent time within a configurable radius of home.

He should generally return home at night unless following the player or completing an urgent task.

### Home ownership

The companion may have:

- A claimed bed.
- A personal chest.
- A crafting table.
- A furnace.
- An expedition status sign.

He must not modify player structures without permission.

Building behavior must be optional and disabled by default.

---

# 8. EXPEDITION SYSTEM

Implement occasional autonomous resource-gathering expeditions.

These should be relatively rare.

**Default expedition frequency:** Once every 2–3 Minecraft days.

**Default maximum duration:** Approximately 5 real-world minutes.

**Default maximum travel distance:** 96 blocks from home.

The companion should prepare before departing.

Preparation may include:

- Checking health.
- Checking food.
- Equipping tools.
- Checking inventory capacity.
- Choosing a suitable destination.
- Determining a realistic objective.

### Expedition restrictions

The companion should:

- Avoid unnecessary danger.
- Prefer nearby destinations.
- Avoid traveling excessively far.
- Return before nighttime when practical.
- Return early if injured.
- Avoid beginning expeditions while assigned to follow the player.
- Avoid repeatedly departing immediately after returning.

### Expedition status sign

Implement a physical sign or vanilla-style status board near the companion's home.

When he departs, display information such as:

COMPANION

Out Mining

Back in ~5 min

Status: Safe

When he returns, update the sign.

Example:

COMPANION

At Home

Available

Status: Resting

The return estimate must reflect actual scheduled behavior.

If the expedition is delayed, update the estimate.

The status must not falsely claim he is safe if his condition is unknown.

### Unloaded chunks

Minecraft normally stops actively simulating entities in unloaded chunks.

Implement a safe solution that does not require permanently loading large areas.

Use lightweight abstract expedition progression where appropriate.

Abstract progression must respect resource availability, configured limitations, and believable survival rules.

Do not silently generate valuable resources without a defined, balanced simulation system.

When the companion returns to loaded terrain, reconcile his abstract expedition state with his actual inventory and location.

Avoid duplication exploits.

---

# 9. FOLLOWER MODE

The player must be able to request that the companion follow them.

Follower mode should interrupt nonessential independent tasks.

### Following behavior

The companion should:

- Maintain a natural following distance.
- Navigate around obstacles.
- Avoid dangerous terrain.
- Follow through doors.
- Swim when necessary.
- Avoid constantly blocking the player.
- Defend himself.
- Assist in combat when appropriate.
- Remain nearby during exploration.

Implement reliable pathfinding and stuck detection.

If navigation fails, attempt recovery.

Allow a configurable emergency teleport system for situations where the companion becomes irrecoverably stuck or excessively separated.

Teleportation should be a fallback, not his normal movement method.

### Follower commands

- Follow Me
- Stay Here
- Return Home
- Stop Current Task
- Gather Nearby Resources
- Help Me Mine
- Defend Me
- Avoid Combat

Commands must update actual behavior.

Do not implement buttons that merely change displayed text.

---

# 10. COMBAT SYSTEM

The companion must be able to participate in combat.

### Combat behaviors

- Detect nearby hostile mobs.
- Recognize threats to himself.
- Recognize threats to the player.
- Equip appropriate weapons.
- Attack within legitimate range.
- Respect weapon cooldowns.
- Avoid attacking friendly entities.
- Retreat when health becomes dangerously low.
- Avoid chasing enemies excessively far.

### Combat modes

**PASSIVE**

Avoids combat whenever possible.

**DEFENSIVE**

Fights when attacked or when the player is threatened.

**AGGRESSIVE**

Actively engages nearby hostile mobs within reasonable limits.

Default mode: DEFENSIVE.

The companion must not attack other players unless explicitly supported by a separately configured PvP system.

Do not implement PvP by default.

---

# 11. DEATH AND RESPAWNING

The companion must respawn after death.

He should not be permanently lost.

### Respawn behavior

- Respawn at his claimed bed.
- Default respawn delay: 15 seconds.
- Preserve name, settings, home assignment, and progression.
- Restore appropriate health and hunger.
- Reset temporary combat states.
- Preserve persistent personality and activity history.

### Inventory drops

Respect Minecraft's `keepInventory` gamerule.

If enabled, preserve his inventory.

If disabled, drop his carried items at the death location.

Prevent item duplication.

### Item recovery

When enabled, the companion may attempt to recover his dropped items.

Recovery should depend on:

- Distance.
- Available equipment.
- Known threats.
- Whether the items still exist.
- Whether the location is reachable.

He should not repeatedly run into lethal situations attempting recovery.

If his bed is destroyed or invalid, use a safe world-spawn fallback until a new bed is claimed.

Display vanilla-style death messages.

---

# 12. SCRIPTED DIALOGUE SYSTEM

Do not integrate a language model.

All dialogue must be predefined and contextual.

The companion should have a small but varied dialogue library.

### Interaction

Right-clicking the companion opens a vanilla-style interaction interface.

Example:

COMPANION

"What's up?"

1. Come With Me
2. Stay Here
3. Head Home
4. What Are You Doing?
5. Need Anything?
6. Got Any Plans?
7. Trade Items
8. Settings
9. Goodbye

### Contextual responses

Dialogue must reflect actual game state.

Examples:

**Player:** What are you doing?

**Companion, while mining:**
"Looking for iron. Need some for my armor."

**Companion, while resting:**
"Taking a break. Got pretty beat up earlier."

**Companion, while following:**
"Just sticking with you."

**Player:** Need anything?

**Companion, low on food:**
"Could use something to eat."

**Companion, adequately supplied:**
"I'm good for now."

### Dialogue requirements

- Never invent activities.
- Never report nonexistent resources.
- Never claim tasks are complete before completion.
- Avoid repeating the same response unnecessarily.
- Keep dialogue concise.
- Use contextual variations.
- Provide clear explanations when commands cannot be completed.

### Idle dialogue

Allow occasional scripted comments.

Examples:

- Reacting to rain.
- Noticing hostile mobs.
- Commenting on low health.
- Thanking the player for gifts.
- Celebrating rare discoveries.
- Reacting to successful combat.

Idle dialogue must be configurable.

Default frequency: Rare.

Do not spam chat.

---

# 13. PERSONALITY AND MEMORY

Create a lightweight personality system without generative AI.

The companion should feel like a consistent character.

Possible traits:

- Cautious or adventurous.
- Quiet or talkative.
- Combat-oriented or resource-oriented.
- Independent or sociable.

These traits may influence task preferences and dialogue selection.

### Persistent gameplay memory

Track meaningful events such as:

- Times the player helped him.
- Major resource discoveries.
- Dangerous encounters.
- Death locations.
- Completed expeditions.
- Equipment upgrades.
- Significant shared adventures.

Memories should be stored as structured gameplay data.

Do not generate fictional memories.

### Behavioral adaptation

The companion may become more cautious around threats that have repeatedly killed him.

He may become more comfortable exploring areas where he has successfully survived.

Adaptation should be subtle and understandable.

Avoid complex relationship meters or mandatory friendship grinding.

---

# 14. INVENTORY AND ITEM EXCHANGE

Create a functional inventory interface.

The player should be able to:

- Inspect the companion's inventory.
- Give him items.
- Retrieve items with permission.
- Equip him with armor.
- Give him weapons.
- Supply food.
- View tool durability.
- Inspect his current resource needs.

Items must be transferred using legitimate inventory transactions.

Prevent duplication, item loss, and desynchronization.

The companion must never silently consume or discard valuable gifted equipment without a valid reason.

---

# 15. COMPANION SETTINGS MENU

Create a complete vanilla-style graphical settings interface.

The interface should resemble Minecraft's existing menus.

Use:

- Pixel-consistent typography.
- Standard Minecraft buttons.
- Tooltips.
- Clear section headings.
- Simple navigation.
- Appropriate scrolling where necessary.

### Required settings

**GENERAL**

Independence:
- Low
- Balanced
- High

Default: Balanced

Resource Gathering:
- Enabled
- Disabled

Default: Enabled

Mining:
- Enabled
- Disabled

Default: Enabled

Building:
- Enabled
- Disabled

Default: Disabled

**EXPEDITIONS**

Frequency:
- Disabled
- Rare
- Normal
- Frequent

Default: Rare

Maximum Distance:
- Adjustable from 32 to 256 blocks

Default: 96 blocks

Maximum Duration:
- Adjustable from 2 to 15 minutes

Default: 5 minutes

Expedition Status Sign:
- Enabled
- Disabled

Default: Enabled

**FOLLOWING**

Follow Distance:
- Close
- Normal
- Relaxed

Default: Normal

Emergency Teleport:
- Enabled
- Disabled

Default: Enabled

**COMBAT**

Combat Behavior:
- Passive
- Defensive
- Aggressive

Default: Defensive

**INVENTORY**

Use Gifted Equipment:
- Enabled
- Disabled

Default: Enabled

Shared Chest Access:
- Enabled
- Disabled

Default: Disabled

Recover Dropped Items:
- Enabled
- Disabled

Default: Enabled

**PERSONALITY**

Idle Dialogue:
- Off
- Rare
- Normal

Default: Rare

**RESPAWNING**

Respawn Delay:
- Adjustable from 5 to 60 seconds

Default: 15 seconds

### Settings persistence

All settings must persist between sessions.

Settings should be stored separately for each companion.

Changes must take effect immediately when practical.

Use server-authoritative validation to prevent invalid settings.

---

# 16. ADDITIONAL CONTROL BUTTONS

Provide the following controls:

### RECALL COMPANION

Requests that the companion return home immediately.

If currently following, cancel follower mode.

If on an expedition, interrupt the expedition and begin return behavior.

Handle unloaded chunks safely.

### RESET CURRENT TASK

Cancels the active task.

Do not erase inventory, progression, memories, or settings.

### SET HOME

Allows the player to assign a valid bed as the companion's home.

### VIEW ACTIVITY

Displays the companion's current activity and recent history.

Example:

DAY 24

09:00 - Left home to gather iron.

11:30 - Found a cave.

12:10 - Encountered skeletons.

13:00 - Returned with 14 raw iron.

Current Goal: Craft iron boots.

Only display events that actually occurred or were explicitly simulated by the expedition system.

---

# 17. USER EXPERIENCE

The companion should feel natural to interact with.

Avoid excessive menus, unnecessary notifications, and constant interruptions.

Important actions should be accessible within a few clicks.

The player should always be able to determine:

- Where the companion is.
- What he is doing.
- Whether he is following.
- Whether he needs supplies.
- Whether he is injured.
- Whether he is away on an expedition.
- When he is expected to return.

Use subtle vanilla-style feedback.

Avoid intrusive HUD elements.

---

# 18. PERFORMANCE REQUIREMENTS

The mod must be suitable for normal survival gameplay.

Avoid:

- Constant full-world searches.
- Excessive pathfinding recalculations.
- Unbounded chunk loading.
- Expensive logic every tick.
- Repeated inventory scans without caching.
- Unnecessary entity spawning.
- Memory leaks.
- Excessive logging.

Use configurable update intervals.

Run expensive planning logic less frequently than movement and combat updates.

Ensure the companion behaves correctly in single-player integrated servers and compatible dedicated servers.

Keep authoritative gameplay state on the server.

---

# 19. SAVE DATA AND PERSISTENCE

Persist:

- Companion identity.
- Name.
- Home location.
- Claimed bed.
- Inventory.
- Equipment.
- Settings.
- Current mode.
- Progression.
- Structured memories.
- Expedition state.
- Respawn state.

Support world saving and reloading.

Prevent duplication when chunks unload and reload.

Ensure companion identity remains stable across death and respawning.

Handle missing or corrupted optional data gracefully.

---

# 20. DEVELOPMENT MILESTONES

Implement the project incrementally.

Each milestone must produce a functional, testable improvement.

### MILESTONE 1 — FOUNDATION

- Create Fabric project.
- Register companion entity.
- Implement vanilla-style rendering.
- Implement basic animations.
- Spawn companion.
- Implement persistent identity.
- Verify compilation.

### MILESTONE 2 — FOLLOWER

- Implement pathfinding.
- Implement follow commands.
- Implement stay and return-home commands.
- Add stuck detection.
- Add defensive combat.
- Verify behavior in-game.

### MILESTONE 3 — INVENTORY AND SURVIVAL

- Implement personal inventory.
- Implement item exchange.
- Implement food consumption.
- Implement equipment handling.
- Implement basic crafting.
- Implement ownership restrictions.

### MILESTONE 4 — INDEPENDENCE

- Implement task scheduling.
- Implement resource gathering.
- Implement mining.
- Implement resource-based progression.
- Implement autonomous survival priorities.
- Implement failure recovery.

### MILESTONE 5 — HOME AND EXPEDITIONS

- Implement claimed beds.
- Implement daily routines.
- Implement occasional expeditions.
- Implement status sign.
- Implement return scheduling.
- Implement unloaded-chunk handling.

### MILESTONE 6 — INTERACTIONS

- Implement dialogue menu.
- Implement contextual scripted dialogue.
- Implement activity history.
- Implement personality traits.
- Implement contextual idle reactions.

### MILESTONE 7 — SETTINGS

- Implement complete settings GUI.
- Implement every specified setting.
- Implement persistence.
- Implement immediate behavior updates.
- Implement companion management controls.

### MILESTONE 8 — RESPAWNING

- Implement death handling.
- Implement inventory drops.
- Implement bed respawning.
- Implement item recovery.
- Implement persistent identity restoration.

### MILESTONE 9 — POLISH AND TESTING

- Fix navigation issues.
- Fix inventory synchronization.
- Fix GUI issues.
- Improve animations.
- Optimize performance.
- Test saving and reloading.
- Test survival gameplay.
- Test edge cases.
- Produce final compiled JAR.

---

# 21. REQUIRED TESTING

Test the following scenarios:

1. Companion spawns successfully.
2. Companion follows the player.
3. Companion stops when commanded.
4. Companion returns home.
5. Companion avoids obvious hazards.
6. Companion fights hostile mobs.
7. Companion eats when hungry.
8. Companion gathers nearby wood.
9. Companion mines accessible ore.
10. Companion collects legitimate item drops.
11. Companion crafts equipment from actual resources.
12. Companion does not access private chests.
13. Companion can use explicitly shared storage.
14. Companion performs occasional expeditions.
15. Expedition status updates correctly.
16. Companion returns after an expedition.
17. Companion handles unloaded chunks safely.
18. Companion dies and respawns at his bed.
19. Inventory drops respect game rules.
20. Companion can recover dropped belongings.
21. Dialogue reflects actual activity.
22. Settings persist after world reload.
23. Companion identity persists after respawning.
24. Multiple companions do not corrupt each other's data.
25. No major inventory duplication exploits exist.

Create automated tests where practical.

For behaviors requiring a running Minecraft instance, provide reproducible manual test procedures.

Do not report untested behaviors as verified.

---

# 22. QUALITY REQUIREMENTS

This must be a real gameplay mod, not a visual mockup.

Do not create placeholder buttons that do nothing.

Do not fake resource gathering.

Do not fake crafting.

Do not fake inventory transfers.

Do not fake expedition history.

Do not claim systems are functional merely because their classes exist.

Avoid excessive hardcoded behavior that breaks during normal survival gameplay.

Prefer smaller, fully functioning systems over large collections of unfinished features.

If a feature is too complex to complete reliably in the current development session, isolate it behind a clearly identified development milestone.

Keep the existing functional systems intact while expanding the project.

---

# 23. FINAL DELIVERABLES

Provide:

1. Complete source code.
2. Complete Gradle project.
3. All necessary textures and assets.
4. Functional Minecraft GUI screens.
5. Compiled mod JAR.
6. Installation instructions.
7. Configuration documentation.
8. Controls and command reference.
9. Testing report.
10. Known limitations and unfinished features.

Document the exact Minecraft version, Fabric Loader version, Fabric API version, and Java version used.

Explain how to install the mod into an existing Minecraft world.

Include instructions for backing up the world before installation.

---

# 24. FINAL DEVELOPMENT DIRECTIVE

You are acting as the lead Minecraft mod developer for this project.

Your objective is to create a reliable autonomous survival companion that feels like a natural part of Minecraft.

Prioritize actual gameplay functionality over complexity.

The companion should be useful without being overpowered, independent without constantly disappearing, and personable without requiring a language model.

He should gather his own resources, respect the player's property, maintain his own equipment, occasionally explore, return home, and accompany the player whenever requested.

Every major system must be grounded in actual Minecraft mechanics.

**Begin by verifying the Minecraft 26.3 development environment and establishing a compiling Fabric project. Then implement the milestones in order, testing each system before advancing.**

Do not stop after producing an architectural outline.

Begin implementation immediately.

Continue developing, compiling, testing, and resolving errors until the maximum practical amount of functionality is complete.

Never conceal unfinished features or present mock functionality as working code.