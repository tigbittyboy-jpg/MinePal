package dev.minepal;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;

public class CompanionSafetyTests {
 private static void floor(GameTestHelper h) {
  for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
 }
 @GameTest(padding=8) public void emergency_follow_catches_up_safely(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(1,2,1));
  var owner=h.makeMockServerPlayerInLevel();owner.snapTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(4.5,2,4.5)));
  c.claim(owner);c.command(CompanionEntity.Mode.FOLLOW);
  c.snapTo(owner.getX()+40,owner.getY(),owner.getZ());
  var before=c.position();c.settings().emergencyTeleport(true);
  h.assertFalse(c.tryEmergencyTeleport(false),"0.4 forbids navigation teleportation even for a separated follower");
  h.assertTrue(c.position().equals(before),"No fallback may change position instantly");
  c.snapTo(owner.position());h.succeed();
 }
 @GameTest(padding=8) public void teleport_setting_and_stay_are_respected(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(1,2,1));var owner=h.makeMockServerPlayerInLevel();
  owner.snapTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(4.5,2,4.5)));c.claim(owner);
  c.snapTo(owner.getX()+40,owner.getY(),owner.getZ());var initial=c.position();
  h.assertFalse(c.tryEmergencyTeleport(false),"A staying companion must never teleport");
  c.command(CompanionEntity.Mode.FOLLOW);c.settings().emergencyTeleport(false);
  h.assertFalse(c.tryEmergencyTeleport(true),"Disabled emergency teleport must not run");
  h.assertTrue(initial.equals(c.position()),"Rejected teleport must not move the actor");c.snapTo(owner.position());h.succeed();
 }
 @GameTest(padding=8) public void hazards_and_obstructions_reject_landing(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(1,2,1));var pos=h.absolutePos(new BlockPos(4,2,4));
  h.assertTrue(SafeTravel.safeLanding(c,h.getLevel(),pos),"Plain stone landing should be valid");
  h.setBlock(4,1,4,Blocks.MAGMA_BLOCK);
  h.assertFalse(SafeTravel.safeLanding(c,h.getLevel(),pos),"Must reject magma floor");
  h.setBlock(4,1,4,Blocks.STONE);h.setBlock(4,2,4,Blocks.STONE);
  h.assertFalse(SafeTravel.safeLanding(c,h.getLevel(),pos),"Must reject body collision");
  h.setBlock(4,2,4,Blocks.AIR);h.setBlock(5,2,4,Blocks.LAVA);
  h.assertFalse(SafeTravel.safeLanding(c,h.getLevel(),pos),"Must reject adjacent lava");h.succeed();
 }
 @GameTest(padding=8) public void settings_roundtrip_and_invalid_optional_data(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(1,2,1));c.settings().combat(CompanionSettings.Combat.PASSIVE);
  c.settings().distance(CompanionSettings.FollowDistance.RELAXED);c.settings().emergencyTeleport(false);
  c.settings().useGiftedEquipment(false);c.settings().sharedStorage(true);
  var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());c.saveWithoutId(out);
  var copy=new CompanionEntity(CompanionMod.TYPE,h.getLevel());
  copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
  h.assertTrue(copy.settings().combat()==CompanionSettings.Combat.PASSIVE && !copy.settings().emergencyTeleport()
   && !copy.settings().useGiftedEquipment() && copy.settings().sharedStorage()
   && copy.settings().distance()==CompanionSettings.FollowDistance.RELAXED,"All settings must persist independently");
  var bad=TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);bad.putString("Combat","BROKEN");bad.putString("FollowDistance","-100");
  var defaults=new CompanionSettings();defaults.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),bad.buildResult()));
  h.assertTrue(defaults.combat()==CompanionSettings.Combat.AGGRESSIVE && defaults.distance()==CompanionSettings.FollowDistance.NORMAL
   && !defaults.sharedStorage(),"Corrupt optional settings must have safe defaults");h.succeed();
 }
 @GameTest(maxTicks=180,padding=8) public void owner_defense_deals_real_damage(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(3,2,3));var owner=h.makeMockServerPlayerInLevel();
  owner.snapTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5,2,3.5)));c.claim(owner);
  var zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(4,2,3));zombie.setNoAi(true);
  zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
  owner.setLastHurtByMob(zombie);
  h.succeedWhen(()->h.assertTrue(zombie.getLastHurtByMob()==c && zombie.getHealth()<zombie.getMaxHealth(),"Defense must result in a real vanilla melee hit"));
 }
 @GameTest(maxTicks=120,padding=8) public void passive_mode_never_initiates_but_can_defend_itself(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(3,2,3));var zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(4,2,3));zombie.setNoAi(true);
  c.settings().combat(CompanionSettings.Combat.PASSIVE);
  h.runAfterDelay(20,()->{
   h.assertTrue(c.getTarget()==null && !c.canAttack(zombie),"Passive must not initiate combat");
   c.setLastHurtByMob(zombie);
   h.succeedWhen(()->h.assertTrue(zombie.getLastHurtByMob()==c,"Passive self-defense must use a real melee hit after provocation"));
  });
 }
 @GameTest(maxTicks=80,padding=8) public void critical_health_interrupts_combat(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(3,2,3));var zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(5,2,3));zombie.setNoAi(true);
  c.setTarget(zombie);c.setHealth(5);
  h.runAfterDelay(20,()->{
   h.assertTrue(c.retreating() && c.getTarget()==null && !c.canAttack(zombie),"Critical health must interrupt attack goals");h.succeed();
  });
 }
 @GameTest(maxTicks=100,padding=8) public void aggressive_mode_targets_hostiles(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));var zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(5,2,2));zombie.setNoAi(true);
  c.settings().combat(CompanionSettings.Combat.AGGRESSIVE);
  h.succeedWhen(()->h.assertTrue(c.getTarget()==zombie,"Aggressive mode must actually select a hostile target"));
 }
 @GameTest(padding=8) public void registered_commands_control_real_state(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));var owner=h.makeMockServerPlayerInLevel();
  owner.snapTo(c.position());c.claim(owner);var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();
  try {
   var source=owner.createCommandSourceStack();
   dispatcher.execute("companion combat passive",source);dispatcher.execute("companion distance close",source);
   dispatcher.execute("companion teleport off",source);dispatcher.execute("companion gifted off",source);
   dispatcher.execute("companion gathering off",source);dispatcher.execute("companion gather",source);
   h.assertTrue(c.settings().combat()==CompanionSettings.Combat.PASSIVE && c.settings().distance()==CompanionSettings.FollowDistance.CLOSE
    && !c.settings().emergencyTeleport() && !c.settings().useGiftedEquipment() && !c.resourceTask().active(),"Registered setting/task commands must affect the server actor");
   dispatcher.execute("companion gathering on",source);dispatcher.execute("companion gather",source);
   h.assertTrue(c.resourceTask().active(),"Gather command must schedule the task");
   dispatcher.execute("companion follow",source);
   h.assertTrue(c.mode()==CompanionEntity.Mode.FOLLOW && !c.resourceTask().active(),"Follow command must cancel the active resource task");
   boolean rejected=false;try { dispatcher.execute("companion distance invalid",source); } catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected) { rejected=true; }
   h.assertTrue(rejected && c.settings().distance()==CompanionSettings.FollowDistance.CLOSE,"Invalid settings must be rejected before mutation");
   h.succeed();
  } catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex) { throw new AssertionError(ex); }
 }
}
