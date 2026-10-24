package dev.minepal;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;

public class CompanionSearchTests {
 private static final java.util.Set<BlockPos> PROTECTED=new java.util.HashSet<>();
 static { net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,block)->!PROTECTED.contains(pos)); }
 private static CompanionEntity prepare(GameTestHelper h) {
  for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));var owner=h.makeMockServerPlayerInLevel();owner.snapTo(c.position());c.claim(owner);c.settings().basicCrafting(false);return c;
 }
 @GameTest(maxTicks=300,padding=20) public void clears_real_leaves_to_harvest_a_log(GameTestHelper h) {
  var c=prepare(h);h.setBlock(4,2,2,Blocks.OAK_LOG);h.setBlock(3,2,2,Blocks.OAK_LEAVES);h.setBlock(3,3,2,Blocks.OAK_LEAVES);
  c.resourceTask().begin(null);
  h.succeedWhen(()->{
   h.assertTrue(h.getBlockState(new BlockPos(3,3,2)).isAir(),"Must physically remove the blocking leaves");
   h.assertTrue(c.getInventory().countItem(Items.OAK_LOG)==1,"Must reach the log and collect its actual drop");
  });
 }
 @GameTest(maxTicks=160,padding=20) public void disabled_leaf_clearing_preserves_leaves(GameTestHelper h) {
  var c=prepare(h);h.setBlock(4,2,2,Blocks.OAK_LOG);h.setBlock(3,2,2,Blocks.OAK_LEAVES);h.setBlock(3,3,2,Blocks.OAK_LEAVES);
  c.settings().clearLeaves(false);c.resourceTask().begin(null);
  h.runAfterDelay(100,()->{
   h.assertTrue(h.getBlockState(new BlockPos(3,2,2)).is(Blocks.OAK_LEAVES) && h.getBlockState(new BlockPos(3,3,2)).is(Blocks.OAK_LEAVES),"Disabled clearing must not alter leaves");h.succeed();
  });
 }
 @GameTest(maxTicks=300,padding=20) public void nearest_mining_search_finds_ore_without_pointing(GameTestHelper h) {
  var c=prepare(h);h.setBlock(6,2,2,Blocks.IRON_ORE);c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));
  String result=c.resourceTask().beginNearestMine();
  h.assertTrue(c.resourceTask().active(),"Nearby mining must find and assign an exposed ore: "+result);
  h.succeedWhen(()->h.assertTrue(c.getInventory().countItem(Items.RAW_IRON)==1,"Nearest search must move, mine, and collect the ore: "+c.status()+" pos="+c.blockPosition()+" ore="+h.getBlockState(new BlockPos(6,2,2))));
 }
 @GameTest(maxTicks=600,padding=20) public void gathered_wood_drives_basic_tool_progression(GameTestHelper h) {
  var c=prepare(h);c.settings().basicCrafting(true);c.settings().useGiftedEquipment(false);
  for(var p:new int[][]{{4,2},{4,3},{5,2},{5,3}})h.setBlock(p[0],2,p[1],Blocks.OAK_LOG);
  h.setBlock(6,2,4,Blocks.CRAFTING_TABLE);c.resourceTask().begin(null);
  h.succeedWhen(()->h.assertTrue(c.getInventory().countItem(Items.WOODEN_PICKAXE)==1 && c.getInventory().countItem(Items.WOODEN_AXE)==1
   && c.getInventory().countItem(Items.WOODEN_SHOVEL)==1,"Actual wood gathering must trigger real supplies/tool crafting at the table: "+c.status()+" pos="+c.blockPosition()+" logs="+c.getInventory().countItem(Items.OAK_LOG)+" planks="+c.getInventory().countItem(Items.OAK_PLANKS)+" sticks="+c.getInventory().countItem(Items.STICK)));
 }
 @GameTest(maxTicks=160,padding=20) public void obstruction_clearing_respects_protection_veto(GameTestHelper h) {
  var c=prepare(h);h.setBlock(4,2,2,Blocks.OAK_LOG);h.setBlock(3,2,2,Blocks.OAK_LEAVES);h.setBlock(3,3,2,Blocks.OAK_LEAVES);
  var protectedLeaf=h.absolutePos(new BlockPos(3,3,2));PROTECTED.add(protectedLeaf);c.resourceTask().begin(null);
  h.runAfterDelay(100,()->{
   h.assertTrue(h.getBlockState(new BlockPos(3,3,2)).is(Blocks.OAK_LEAVES) && h.getBlockState(new BlockPos(4,2,2)).is(Blocks.OAK_LOG),"A protected obstruction must stop the harvest, not be bypassed");
   h.assertTrue(c.getInventory().countItem(Items.OAK_LOG)==0,"Denied clearing must not award the unreachable log");PROTECTED.remove(protectedLeaf);h.succeed();
  });
 }
 @GameTest(maxTicks=600,padding=20) public void mined_stone_drives_real_equipment_upgrade(GameTestHelper h) {
  var c=prepare(h);c.settings().basicCrafting(true);
  c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.WOODEN_PICKAXE));
  c.getInventory().setItem(0,new ItemStack(Items.WOODEN_AXE));c.getInventory().setItem(1,new ItemStack(Items.WOODEN_SHOVEL));
  c.getInventory().setItem(2,new ItemStack(Items.STICK,2));
  h.setBlock(2,2,5,Blocks.CRAFTING_TABLE);
  for(int z=2;z<=4;z++)h.setBlock(4,2,z,Blocks.STONE);
  c.resourceTask().beginNearestMine();
  h.succeedWhen(()->{
   h.assertTrue(c.getMainHandItem().is(Items.STONE_PICKAXE),"Mined stone must become a real crafted and equipped upgrade: "+c.status());
   h.assertTrue(c.getInventory().countItem(Items.WOODEN_PICKAXE)==1 && c.getInventory().countItem(Items.COBBLESTONE)==0,
    "Upgrade must consume the three mined cobbles and preserve the prior pickaxe");
  });
 }
}
