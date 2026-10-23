# Companion 0.1.0 — development build

Download `companion-0.1.0.jar` in this folder (open the file and select **Download raw file**).

Requires Minecraft Java **26.3**, Java **25**, Fabric Loader **0.20.1**, and
Fabric API **0.165.0+26.3**. Install Companion and Fabric API in your `mods` folder.
For multiplayer, both the server and clients need them.

Back up your world and use a test world first. **Death is permanent in this build;
respawning and keepInventory support are unfinished.**

Enable commands, then use `/companion spawn`. Right-click to toggle following;
sneak-right-click to stay. Other commands:

- `/companion follow`, `/companion stay`
- `/companion home` while looking at a bed; `/companion return`
- `/companion name Rowan`, `/companion status`
- `/companion inventory`, `/companion equip`
- `/companion craft`: one recipe from the first nine personal inventory slots,
  interpreted in row-major 3×3 order; requires a crafting table within three blocks.

Includes humanoid rendering, movement, bed-home assignment, persistent identity,
personal inventory, simplified hunger/eating, equipment, and recipe-backed crafting.
Autonomous gathering, mining, smelting, expeditions, dialogue/settings menus,
respawning, and item recovery are not implemented.

Built successfully with Loom 1.19.3 and Gradle 9.8.2. All 12 required server
GameTests and the integrated client rendering/inventory test passed. Full survival
and multiplayer testing remains incomplete. This is not the completed mod.
