package dev.minepal;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;

public class CompanionGameTests {
 private static void floor(GameTestHelper h) {
  for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
 }
 @GameTest(padding=8) public void spawn(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));
  h.assertTrue(c.isAlive(),"Registered entity must spawn alive");
  h.assertTrue(c.getMaxHealth()==20,"Companion health attribute");h.succeed();
 }
 @GameTest(maxTicks=100,padding=8) public void stay(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));c.command(CompanionEntity.Mode.STAY);
  var start=c.position();h.runAfterDelay(60,()->{
   h.assertTrue(c.position().distanceToSqr(start)<1,"Stay must not wander");h.succeed();
  });
 }
 @GameTest(maxTicks=200,padding=8) public void home_navigation(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(1,2,1));
  var target=h.absolutePos(new BlockPos(6,2,6));c.setHome(target);c.command(CompanionEntity.Mode.HOME);
  h.succeedWhen(()->h.assertTrue(c.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target))<5,"Must navigate home"));
 }
 @GameTest(padding=8) public void persistence(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));
  var home=h.absolutePos(new BlockPos(5,2,5));c.setHome(home);c.setCustomName(Component.literal("Rowan"));
  c.command(CompanionEntity.Mode.HOME);
  var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());
  c.saveWithoutId(out);
  var copy=new CompanionEntity(CompanionMod.TYPE,h.getLevel());
  copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
  h.assertTrue(copy.getUUID().equals(c.getUUID()),"Identity must survive serialization");
  h.assertTrue(home.equals(copy.home()),"Home must survive serialization");
  h.assertTrue(copy.getName().getString().equals("Rowan"),"Name must survive serialization");
  h.assertTrue(copy.status().startsWith("Returning home"),"Mode must survive serialization");h.succeed();
 }
 @GameTest(maxTicks=160,padding=8) public void following(GameTestHelper h) {
  floor(h);var c=h.spawn(CompanionMod.TYPE,new BlockPos(1,2,1));
  var player=h.makeMockServerPlayerInLevel();player.snapTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(6.5,2,6.5)));
  c.claim(player);c.command(CompanionEntity.Mode.FOLLOW);double start=c.distanceToSqr(player);
  h.succeedWhen(()->h.assertTrue(c.distanceToSqr(player)<start-10,"Follow must actually move toward its owner"));
 }
 @GameTest(padding=8) public void refuses_pvp(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));var player=h.makeMockServerPlayerInLevel();
  h.assertFalse(c.canAttack(player),"Companion must never attack players");
  var other=h.spawn(CompanionMod.TYPE,new BlockPos(3,2,2));
  h.assertFalse(c.canAttack(other),"Companions must not attack each other");h.succeed();
 }
 @GameTest(padding=8) public void personal_inventory_roundtrip(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));
  c.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
  c.getInventory().setHunger(9);
  var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());c.saveWithoutId(out);
  var copy=new CompanionEntity(CompanionMod.TYPE,h.getLevel());
  copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
  h.assertTrue(copy.getInventory().getItem(0).getCount()==7,"Personal items must survive save/load");
  h.assertTrue(copy.getInventory().hunger()==9,"Hunger must survive save/load");
  h.assertTrue(c.getInventory()!=copy.getInventory(),"Inventories must be independent");h.succeed();
 }
 @GameTest(padding=8) public void consumes_real_food(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));
  c.getInventory().setHunger(10);
  c.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD,2));
  for(int i=0;i<40;i++)c.getInventory().tickSurvival();
  h.assertTrue(c.getInventory().hunger()==15,"Bread nutrition must restore hunger");
  h.assertTrue(c.getInventory().getItem(0).getCount()==1,"Eating must consume exactly one owned item");h.succeed();
 }
 @GameTest(padding=8) public void equips_without_copying(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));
  c.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BOOTS));
  c.getInventory().equipEmptySlots();
  h.assertTrue(c.getInventory().getItem(0).isEmpty(),"Equipping must remove the storage item");
  h.assertTrue(c.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(net.minecraft.world.item.Items.IRON_BOOTS),"Must equip real boots");h.succeed();
 }
 @GameTest(padding=8) public void craft_consumes_ingredients(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));h.setBlock(3,2,2,Blocks.CRAFTING_TABLE);
  for(int slot:new int[]{0,2,3,5})c.getInventory().setItem(slot,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
  String result=CompanionCrafting.craft(c);
  int boots=0,iron=0;
  for(int i=0;i<27;i++) {var stack=c.getInventory().getItem(i);if(stack.is(net.minecraft.world.item.Items.IRON_BOOTS))boots+=stack.getCount();if(stack.is(net.minecraft.world.item.Items.IRON_INGOT))iron+=stack.getCount();}
  h.assertTrue(boots==1 && iron==0,"Must craft one pair of boots from exactly four real ingots: "+result);h.succeed();
 }
 @GameTest(padding=8) public void craft_requires_workstation(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));
  c.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG));
  String result=CompanionCrafting.craft(c);
  h.assertTrue(result.contains("crafting table"),"Must require real workstation");
  h.assertTrue(c.getInventory().getItem(0).getCount()==1,"Failed craft must preserve ingredients");h.succeed();
 }
}
