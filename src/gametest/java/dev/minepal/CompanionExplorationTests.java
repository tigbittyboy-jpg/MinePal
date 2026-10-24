package dev.minepal;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Blocks;

public class CompanionExplorationTests {
 private static CompanionEntity prepare(GameTestHelper h,BlockPos pos){
  var c=h.spawn(CompanionMod.TYPE,pos);var owner=h.makeMockServerPlayerInLevel();owner.snapTo(c.position());c.claim(owner);c.settings().basicCrafting(false);return c;
 }
 private static void floor(GameTestHelper h){for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);}
 private static void tree(GameTestHelper h,int x,int z){
  h.setBlock(x,1,z,Blocks.DIRT);for(int y=2;y<=5;y++)h.setBlock(x,y,z,Blocks.OAK_LOG);
  h.setBlock(x+1,5,z,Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1));
 }
 @GameTest(maxTicks=600,padding=12) public void connected_tree_plan_harvests_crown_before_completion(GameTestHelper h){
  floor(h);var c=prepare(h,new BlockPos(2,2,2));tree(h,4,4);h.setBlock(4,6,4,Blocks.OAK_LOG);h.setBlock(5,5,4,Blocks.OAK_LOG);h.setBlock(6,5,4,Blocks.OAK_LOG);h.setBlock(6,6,4,Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1));
  h.setBlock(1,2,6,Blocks.OAK_LOG);c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
  c.resourceTask().assign(ResourceTaskGoal.Objective.WOOD,3);
  h.succeedWhen(()->{
   h.assertTrue(!c.resourceTask().active() && c.taskLedger().phase()==TaskLedger.Phase.COMPLETED,"Must complete the requested natural-tree task: "+c.status()+" / "+c.taskLedger().detail()+" pos="+c.blockPosition());
   for(int y=2;y<=6;y++)h.assertTrue(h.getBlockState(new BlockPos(4,y,4)).isAir(),"No identifiable trunk log may be left floating");
   h.assertTrue(h.getBlockState(new BlockPos(5,5,4)).isAir() && h.getBlockState(new BlockPos(6,5,4)).isAir(),"Connected horizontal branches must be harvested");
   h.assertTrue(h.getBlockState(new BlockPos(1,2,6)).is(Blocks.OAK_LOG),"Unrelated placed logs must remain untouched");
   h.assertTrue(c.getInventory().countItem(Items.OAK_LOG)==7,"Real drops from the entire selected tree must be collected");
  });
 }
 @GameTest(padding=12) public void tree_detection_rejects_plain_log_building(GameTestHelper h){
  floor(h);tree(h,4,4);var plan=new TreeHarvestPlan();h.assertTrue(plan.detect(h.getLevel(),h.absolutePos(new BlockPos(4,2,4))),"Natural soil/canopy tree should be recognized");
  h.setBlock(1,2,1,Blocks.OAK_LOG);h.setBlock(1,3,1,Blocks.OAK_LOG);
  h.assertFalse(plan.detect(h.getLevel(),h.absolutePos(new BlockPos(1,2,1))),"No-soil/no-canopy player log structure must be rejected");h.succeed();
 }
 @GameTest(maxTicks=700,padding=12) public void excavates_a_staircase_through_dirt_into_stone(GameTestHelper h){
  for(int x=0;x<8;x++)for(int z=0;z<8;z++){h.setBlock(x,1,z,Blocks.DIRT);for(int y=-4;y<=0;y++)h.setBlock(x,y,z,Blocks.STONE);}
  var c=prepare(h,new BlockPos(4,2,4));c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));c.getInventory().setItem(0,new ItemStack(Items.IRON_SHOVEL));var start=c.position();
  c.resourceTask().assign(ResourceTaskGoal.Objective.STONE,1);
  h.succeedWhen(()->{
   h.assertTrue(c.getInventory().countItem(Items.COBBLESTONE)>=1,"Must excavate dirt, reach real stone, and collect cobblestone: "+c.status()+" / "+c.taskLedger().detail());
   h.assertTrue(c.getY()<start.y && c.taskLedger().collected()>=1,"Must descend physically and count actual collected stone");
  });
 }
 @GameTest(padding=12) public void excavation_rejects_liquids_and_direct_support_removal(GameTestHelper h){
  floor(h);var c=prepare(h,new BlockPos(3,2,3));var support=h.absolutePos(new BlockPos(3,1,3));
  h.assertFalse(ExcavationPlanner.safeCut(c,h.getLevel(),support),"Cannot dig directly under one's feet");
  h.setBlock(4,1,3,Blocks.DIRT);h.setBlock(5,1,3,Blocks.LAVA);
  h.assertFalse(ExcavationPlanner.safeCut(c,h.getLevel(),h.absolutePos(new BlockPos(4,1,3))),"Do not open adjacent lava into a staircase");h.succeed();
 }
 @GameTest(maxTicks=100,padding=12) public void empty_search_is_blocked_and_keeps_objective(GameTestHelper h){
  floor(h);var c=prepare(h,new BlockPos(2,2,2));c.resourceTask().assign(ResourceTaskGoal.Objective.WOOD,64);
  h.runAfterDelay(60,()->{
   h.assertTrue(c.resourceTask().active() && c.taskLedger().requested()==64 && c.taskLedger().collected()==0,"Empty field must not silently complete or cancel the assigned task");
   h.assertTrue(c.taskLedger().phase()!=TaskLedger.Phase.COMPLETED,"No resources means no completion claim");h.succeed();
  });
 }
 @GameTest(maxTicks=6000,structure="companion-test:long_field",padding=8) public void discovers_and_walks_to_a_tree_over_100_blocks_away(GameTestHelper h){
  var c=prepare(h,new BlockPos(2,2,8));tree(h,108,8);c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));var start=c.position();
  c.resourceTask().assign(ResourceTaskGoal.Objective.WOOD,4);
  h.succeedWhen(()->{
   h.assertTrue(c.getInventory().countItem(Items.OAK_LOG)>=4,"Progressive search must find and harvest the distant loaded tree: "+c.status()+" / "+c.resourceTask().searchDiagnostics()+" pos="+c.blockPosition());
   h.assertTrue(c.position().distanceToSqr(start)>90*90,"Must actually travel instead of awarding distant resources");
   h.assertTrue(c.spatialMemory().searchedCount()>1,"Exploration must record searched cells");
  });
 }
 @GameTest(maxTicks=1600,structure="companion-test:long_field",padding=8) public void travels_to_a_remembered_workstation_beyond_old_radius(GameTestHelper h){
  var c=prepare(h,new BlockPos(2,2,8));h.setBlock(90,2,8,Blocks.CRAFTING_TABLE);var table=h.absolutePos(new BlockPos(90,2,8));
  c.spatialMemory().remember(h.getLevel().dimension().identifier().toString(),SpatialMemory.Kind.TABLE,table);
  for(int slot:new int[]{0,2,3,5})c.getInventory().setItem(slot,new ItemStack(Items.IRON_INGOT));c.workTask().grid();var start=c.position();
  h.succeedWhen(()->{
   h.assertTrue(c.getInventory().countItem(Items.IRON_BOOTS)==1 || c.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS),"Must use the distant remembered table for actual crafting: "+c.status());
   h.assertTrue(c.position().distanceToSqr(start)>70*70,"Workbench use must require real travel");
  });
 }
 @GameTest(maxTicks=1000,padding=12) public void requested_stone_quantity_survives_multiple_excavation_steps(GameTestHelper h){
  for(int x=0;x<8;x++)for(int z=0;z<8;z++){h.setBlock(x,1,z,Blocks.DIRT);for(int y=-8;y<=0;y++)h.setBlock(x,y,z,Blocks.STONE);}
  var c=prepare(h,new BlockPos(4,2,4));c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));c.getInventory().setItem(0,new ItemStack(Items.IRON_SHOVEL));
  c.resourceTask().assign(ResourceTaskGoal.Objective.STONE,4);
  h.succeedWhen(()->{
   h.assertTrue(!c.resourceTask().active() && c.taskLedger().phase()==TaskLedger.Phase.COMPLETED,"Quantity mining must continue through multiple steps: "+c.status());
   h.assertTrue(c.getInventory().countItem(Items.COBBLESTONE)>=4 && c.taskLedger().collected()>=4,"Completion must count real stone drops, not cleared dirt");
  });
 }
 @GameTest(maxTicks=1200,padding=12) public void unsupported_high_crown_is_not_reported_complete(GameTestHelper h){
  floor(h);var c=prepare(h,new BlockPos(2,2,2));h.setBlock(4,1,4,Blocks.DIRT);
  for(int y=2;y<=14;y++)h.setBlock(4,y,4,Blocks.OAK_LOG);
  h.setBlock(5,14,4,Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1));
  c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));c.resourceTask().assign(ResourceTaskGoal.Objective.WOOD,1);
  h.runAfterDelay(1000,()->{
   h.assertTrue(h.getBlockState(new BlockPos(4,14,4)).is(Blocks.OAK_LOG),"Fixture must retain an inaccessible high crown");
   h.assertTrue(c.resourceTask().active() && c.taskLedger().phase()!=TaskLedger.Phase.COMPLETED,"A remaining floating crown must never be claimed complete");
   h.assertTrue(h.getBlockState(new BlockPos(4,3,4)).isAir(),"Must perform real reachable harvesting before reporting the access limitation");h.succeed();
  });
 }
 @GameTest(padding=12) public void deferred_tree_frontier_reconciles_from_loaded_world_data(GameTestHelper h){
  floor(h);tree(h,4,4);var plan=new TreeHarvestPlan();plan.detect(h.getLevel(),h.absolutePos(new BlockPos(4,2,4)));
  var upper=h.absolutePos(new BlockPos(4,5,4));plan.removed(upper);
  var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess());plan.save(out);
  out.putBoolean("Incomplete",true);out.store("Frontier",com.mojang.serialization.Codec.LONG.listOf(),java.util.List.of(upper.asLong()));
  var restored=new TreeHarvestPlan();restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
  restored.reconcile(h.getLevel());
  h.assertTrue(restored.contains(upper) && !restored.incomplete(),"A now-loaded frontier must restore the actual log into the existing plan");
  h.assertTrue(h.getBlockState(new BlockPos(4,5,4)).is(Blocks.OAK_LOG),"Reconciling a plan must not create loot or alter blocks");h.succeed();
 }
}
