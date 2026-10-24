package dev.minepal;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;

public class CompanionIntelligenceTests {
 private static CompanionEntity prepare(GameTestHelper h){
  for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));var owner=h.makeMockServerPlayerInLevel();owner.snapTo(c.position());c.claim(owner);return c;
 }
 @GameTest(maxTicks=180,padding=12) public void actual_owner_punch_on_goat_triggers_melee_assistance(GameTestHelper h){
  var c=prepare(h);var owner=(net.minecraft.server.level.ServerPlayer)c.ownerPlayer();
  var goat=h.spawn(EntityTypes.GOAT,new BlockPos(4,2,2));goat.setNoAi(true);
  var sword=new ItemStack(Items.IRON_SWORD);c.getInventory().setItem(0,sword);owner.attack(goat);
  h.assertTrue(owner.getLastHurtMob()==goat,"Owner attack record missing; goat health="+goat.getHealth()+" owner mode="+owner.gameMode());
  h.succeedWhen(()->{
   h.assertTrue(goat.getLastHurtByMob()==c,"Companion must physically hit the goat selected by an actual owner attack: "+c.status()+" selected="+(c.getTarget()==goat)+" allowed="+c.canAttack(goat)+" ownerTarget="+(owner.getLastHurtMob()==goat)+" goatHealth="+goat.getHealth());
   h.assertTrue(c.getMainHandItem().is(Items.IRON_SWORD),"Combat must select the owned sword");
   h.assertTrue(c.getMainHandItem().getDamageValue()>0,"A real weapon hit must expend weapon durability");
  });
 }
 @GameTest(maxTicks=80,padding=12) public void unprovoked_animals_and_owner_remain_friendly(GameTestHelper h){
  var c=prepare(h);var cow=h.spawn(EntityTypes.COW,new BlockPos(4,2,2));cow.setNoAi(true);
  h.runAfterDelay(40,()->{h.assertTrue(c.getTarget()==null && !c.canAttack(cow) && !c.canAttack(c.ownerPlayer()),"Aggressive default must not start hunting peaceful animals or the owner");h.succeed();});
 }
 @GameTest(maxTicks=180,padding=12) public void work_resumes_after_real_owner_assist_combat(GameTestHelper h){
  var c=prepare(h);c.settings().basicCrafting(false);
  c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));c.getInventory().setItem(0,new ItemStack(Items.IRON_SWORD));
  h.setBlock(4,2,3,Blocks.IRON_ORE);c.resourceTask().begin(h.absolutePos(new BlockPos(4,2,3)));
  var goat=h.spawn(EntityTypes.GOAT,new BlockPos(3,2,2));goat.setNoAi(true);c.ownerPlayer().attack(goat);
  h.succeedWhen(()->{
   h.assertTrue(goat.getLastHurtByMob()==c,"Owner assistance must occur before resuming mining");
   h.assertTrue(c.getInventory().countItem(Items.RAW_IRON)==1,"Assigned mining must resume and collect a legitimate drop");
   h.assertTrue(c.taskLedger().collected()==1,"Interruption must not reset or duplicate collection progress");
  });
 }
 @GameTest(maxTicks=180,padding=12) public void overwhelming_golem_causes_walking_retreat(GameTestHelper h){
  var c=prepare(h);c.setHealth(10);var golem=h.spawn(EntityTypes.IRON_GOLEM,new BlockPos(4,2,2));golem.setNoAi(true);golem.setTarget(c);
  c.hurtServer(h.getLevel(),h.getLevel().damageSources().mobAttack(golem),2);var start=c.position();
  h.succeedWhen(()->h.assertTrue(c.retreating() && c.getTarget()==null && c.position().distanceToSqr(start)>1,"Must retreat using real navigation from an overwhelming threat"));
 }
 @GameTest(padding=12) public void chicken_and_golem_have_distinct_threat_scores(GameTestHelper h){
  var c=prepare(h);c.setHealth(10);var chicken=h.spawn(EntityTypes.CHICKEN,new BlockPos(4,2,2));chicken.setNoAi(true);c.setLastHurtByMob(chicken);
  double low=ThreatAssessment.score(c);h.assertFalse(ThreatAssessment.shouldRetreat(c),"Half-health companion must not panic at a harmless chicken");
  var golem=h.spawn(EntityTypes.IRON_GOLEM,new BlockPos(4,2,3));golem.setNoAi(true);golem.setTarget(c);
  h.assertTrue(ThreatAssessment.score(c)>low+8 && ThreatAssessment.shouldRetreat(c),"Threat scoring must distinguish a powerful active attacker");h.succeed();
 }
 @GameTest(padding=12) public void ledger_memory_and_pvp_settings_persist(GameTestHelper h){
  var c=prepare(h);c.taskLedger().begin("Gather wood",64);c.taskLedger().add(14);c.taskLedger().state(TaskLedger.Phase.COLLECTING,"Picking up logs");
  c.taskLedger().interrupt(false,true);c.spatialMemory().searched("test#WOOD",c.blockPosition(),10);
  c.spatialMemory().remember("test",SpatialMemory.Kind.TABLE,c.blockPosition().offset(80,0,0));c.settings().pvpAssist(true);
  var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());c.saveWithoutId(out);
  var copy=new CompanionEntity(CompanionMod.TYPE,h.getLevel());copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
  h.assertTrue(copy.taskLedger().requested()==64 && copy.taskLedger().collected()==14 && copy.taskLedger().phase()==TaskLedger.Phase.COLLECTING,"Save/load must restore underlying task progress, not a stale combat state");
  h.assertTrue(copy.spatialMemory().searchedCount()==1 && copy.spatialMemory().locations("test",SpatialMemory.Kind.TABLE).size()==1 && copy.settings().pvpAssist(),"Exploration/workstation memory and settings must persist");
  h.assertFalse(CombatSafety.valid(copy,c.ownerPlayer()),"Owner remains protected even with PvP enabled");h.succeed();
 }
}
