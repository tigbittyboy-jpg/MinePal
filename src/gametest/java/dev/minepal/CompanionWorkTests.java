package dev.minepal;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;

public class CompanionWorkTests {
 private static CompanionEntity prepare(GameTestHelper h) {
  for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(1,2,2));var owner=h.makeMockServerPlayerInLevel();owner.snapTo(c.position());c.claim(owner);
  c.settings().useGiftedEquipment(false);return c;
 }
 @GameTest(maxTicks=300,padding=20) public void walks_to_distant_workbench_and_crafts(GameTestHelper h) {
  var c=prepare(h);h.setBlock(7,2,2,Blocks.CRAFTING_TABLE);
  for(int slot:new int[]{0,2,3,5})c.getInventory().setItem(slot,new ItemStack(Items.IRON_INGOT));
  var start=c.position();String result=CompanionCrafting.craft(c);
  h.assertTrue(c.workTask().active(),"Craft should queue a workstation search: "+result);
  h.succeedWhen(()->{
   h.assertTrue(c.getInventory().countItem(Items.IRON_BOOTS)==1,"Must arrive and craft real boots: "+c.status());
   h.assertTrue(c.position().distanceToSqr(start)>1,"Must actually walk to the workstation");
   h.assertTrue(c.getInventory().countItem(Items.IRON_INGOT)==0,"Must consume actual materials");
  });
 }
 @GameTest(maxTicks=450,padding=20) public void creates_and_places_owned_bench_then_tools(GameTestHelper h) {
  var c=prepare(h);c.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,8));c.workTask().prepare(true);
  h.succeedWhen(()->{
   h.assertTrue(c.getInventory().countItem(Items.WOODEN_PICKAXE)==1 && c.getInventory().countItem(Items.WOODEN_AXE)==1
    && c.getInventory().countItem(Items.WOODEN_SHOVEL)==1,"Must create actual basic tools");
   h.assertTrue(c.getInventory().countItem(Items.OAK_LOG)==4,"Recipe chain must consume exactly four logs");
   h.assertTrue(c.getInventory().countItem(Items.OAK_PLANKS)==1 && c.getInventory().countItem(Items.STICK)==2,"Must retain remaining planks and sticks");
   h.assertTrue(WorkstationGoal.nearest(c,4,false)!=null && c.getInventory().countItem(Items.CRAFTING_TABLE)==0,"Must place and spend an owned crafting table");
  });
 }
 @GameTest(maxTicks=300,padding=20) public void automatic_planning_never_places_without_permission(GameTestHelper h) {
  var c=prepare(h);c.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,5));c.workTask().prepare(false);
  h.succeedWhen(()->{
   h.assertTrue(!c.workTask().active(),"Automatic preparation should finish with a useful explanation");
   h.assertTrue(c.getInventory().countItem(Items.CRAFTING_TABLE)==1 && WorkstationGoal.nearest(c,16,false)==null,"Bench must stay an owned item until placement is authorized");
   h.assertTrue(c.getInventory().countItem(Items.WOODEN_PICKAXE)==0,"Cannot craft 3x3 tools without a real table");
  });
 }
 @GameTest public void insufficient_capacity_preserves_recipe_inputs(GameTestHelper h) {
  var c=prepare(h);for(int i=0;i<27;i++)c.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
  c.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,2));
  h.assertTrue(BasicCrafting.step(c,false)==BasicCrafting.Step.FULL,"Full inventory must reject conversion atomically");
  h.assertTrue(c.getInventory().getItem(0).getCount()==2 && c.getInventory().countItem(Items.OAK_PLANKS)==0,"Failure must preserve logs and not manufacture outputs");h.succeed();
 }
 @GameTest(maxTicks=300,padding=20) public void uses_earned_iron_for_common_tools(GameTestHelper h) {
  var c=prepare(h);h.setBlock(3,2,2,Blocks.CRAFTING_TABLE);
  c.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,4));c.getInventory().setItem(1,new ItemStack(Items.IRON_INGOT,7));c.workTask().prepare(false);
  h.succeedWhen(()->{
   h.assertTrue(c.getInventory().countItem(Items.IRON_PICKAXE)==1 && c.getInventory().countItem(Items.IRON_AXE)==1
    && c.getInventory().countItem(Items.IRON_SHOVEL)==1,"Must craft tools from owned iron");
   h.assertTrue(c.getInventory().countItem(Items.IRON_INGOT)==0 && c.getInventory().countItem(Items.WOODEN_PICKAXE)==0,"Do not waste resources on an inferior duplicate");
  });
 }
 @GameTest public void work_job_and_new_settings_survive_serialization(GameTestHelper h) {
  var c=prepare(h);c.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,4));c.workTask().prepare(true);
  c.settings().clearLeaves(false);c.settings().basicCrafting(false);
  var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());c.saveWithoutId(out);
  var copy=new CompanionEntity(CompanionMod.TYPE,h.getLevel());copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
  h.assertTrue(copy.workTask().active() && copy.mode()==CompanionEntity.Mode.WORK && !copy.settings().clearLeaves()
   && !copy.settings().basicCrafting(),"Work and settings must survive save/load without loot creation");
  h.assertTrue(copy.getInventory().countItem(Items.OAK_LOG)==4,"Save/load must not execute crafting");h.succeed();
 }
 @GameTest(maxTicks=120,padding=20) public void follow_cancels_crafting_work(GameTestHelper h) {
  var c=prepare(h);h.setBlock(7,2,2,Blocks.CRAFTING_TABLE);
  for(int slot:new int[]{0,2,3,5})c.getInventory().setItem(slot,new ItemStack(Items.IRON_INGOT));
  CompanionCrafting.craft(c);c.command(CompanionEntity.Mode.FOLLOW);
  h.runAfterDelay(60,()->{
   h.assertTrue(!c.workTask().active() && c.getInventory().countItem(Items.IRON_INGOT)==4,"Follow must interrupt work without spending resources");h.succeed();
  });
 }
 @GameTest public void menu_rejects_other_players_and_invalid_slots(GameTestHelper h) {
  var c=prepare(h);var other=h.makeMockServerPlayerInLevel();other.snapTo(c.position());
  CompanionMenuProtocol.apply(other,c,"gather","");CompanionMenuProtocol.apply(other,c,"name","Stolen");
  h.assertTrue(!c.resourceTask().active() && !c.getName().getString().equals("Stolen"),"Menu actions must validate owner on the server");
  var owner=(net.minecraft.server.level.ServerPlayer)c.ownerPlayer();
  String result=CompanionMenuProtocol.apply(owner,c,"withdraw","-1");
  h.assertTrue(result.contains("1 to 27"),"Invalid menu slot must not reach inventory mutation");h.succeed();
 }
 @GameTest public void earned_tool_upgrade_preserves_previous_equipment(GameTestHelper h) {
  var c=prepare(h);c.settings().useGiftedEquipment(true);
  var old=new ItemStack(Items.WOODEN_PICKAXE);c.setItemSlot(EquipmentSlot.MAINHAND,old);
  c.getInventory().setItem(0,new ItemStack(Items.STONE_PICKAXE));c.getInventory().equipEmptySlots();
  h.assertTrue(c.getMainHandItem().is(Items.STONE_PICKAXE) && c.getInventory().getItem(0)==old,"Earned upgrades must equip and preserve the original tool");h.succeed();
 }
}
