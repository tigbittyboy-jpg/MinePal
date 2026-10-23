package dev.minepal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;

public class CompanionResourceTests {
 private static final Set<BlockPos> DENIED=ConcurrentHashMap.newKeySet();
 static { PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,block)->!DENIED.contains(pos)); }
 private static CompanionEntity prepare(GameTestHelper h) {
  for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));
  var owner=h.makeMockServerPlayerInLevel();owner.snapTo(c.position());c.claim(owner);return c;
 }
 @GameTest(maxTicks=240,padding=10) public void gathers_real_logs_and_collects_the_drop(GameTestHelper h) {
  var c=prepare(h);h.setBlock(4,2,2,Blocks.OAK_LOG);c.resourceTask().begin(null);
  h.succeedWhen(()->{
   h.assertTrue(h.getBlockState(new BlockPos(4,2,2)).isAir(),"Gathering must actually remove the log");
   h.assertTrue(c.getInventory().countItem(Items.OAK_LOG)==1,"Must collect the legitimate log drop");
  });
 }
 @GameTest(maxTicks=180,padding=10) public void mines_ore_with_real_drop_and_durability(GameTestHelper h) {
  var c=prepare(h);h.setBlock(4,2,2,Blocks.IRON_ORE);var pick=new ItemStack(Items.IRON_PICKAXE);c.setItemSlot(EquipmentSlot.MAINHAND,pick);
  c.resourceTask().begin(h.absolutePos(new BlockPos(4,2,2)));
  h.succeedWhen(()->{
   h.assertTrue(h.getBlockState(new BlockPos(4,2,2)).isAir(),"Ore must actually be broken");
   h.assertTrue(c.getInventory().countItem(Items.RAW_IRON)==1,"Ore must yield its real raw-iron drop");
   h.assertTrue(pick.getDamageValue()==1,"Mining must consume tool durability once");
  });
 }
 @GameTest(maxTicks=80,padding=10) public void unsuitable_tool_never_destroys_valuable_ore(GameTestHelper h) {
  var c=prepare(h);h.setBlock(4,2,2,Blocks.DIAMOND_ORE);
  c.resourceTask().begin(h.absolutePos(new BlockPos(4,2,2)));
  h.runAfterDelay(40,()->{
   h.assertTrue(h.getBlockState(new BlockPos(4,2,2)).is(Blocks.DIAMOND_ORE),"Must not destroy ore without the required tool");
   h.assertTrue(c.getInventory().countItem(Items.DIAMOND)==0 && !c.resourceTask().active(),"Failure must not manufacture resources or loop");h.succeed();
  });
 }
 @GameTest(maxTicks=130,padding=10) public void follows_commands_without_continuing_to_break_blocks(GameTestHelper h) {
  var c=prepare(h);h.setBlock(4,2,2,Blocks.OAK_LOG);c.resourceTask().begin(null);
  h.runAfterDelay(10,()->c.command(CompanionEntity.Mode.FOLLOW));
  h.runAfterDelay(90,()->{
   h.assertTrue(h.getBlockState(new BlockPos(4,2,2)).is(Blocks.OAK_LOG),"Follow must cancel a nonessential gather before block removal");
   h.assertTrue(!c.resourceTask().active() && c.mode()==CompanionEntity.Mode.FOLLOW,"Command must update actual scheduling");h.succeed();
  });
 }
 @GameTest(maxTicks=180,padding=10) public void respects_protection_event_cancellation(GameTestHelper h) {
  var c=prepare(h);h.setBlock(4,2,2,Blocks.IRON_ORE);c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));
  var pos=h.absolutePos(new BlockPos(4,2,2));DENIED.add(pos);c.resourceTask().begin(pos);
  h.runAfterDelay(100,()->{
   h.assertTrue(h.getBlockState(new BlockPos(4,2,2)).is(Blocks.IRON_ORE),"Protection callback must prevent block destruction");
   h.assertTrue(c.getInventory().countItem(Items.RAW_IRON)==0,"Protected ore cannot produce resources");DENIED.remove(pos);h.succeed();
  });
 }
 @GameTest public void resource_assignment_persists_without_awarding_loot(GameTestHelper h) {
  var c=prepare(h);h.setBlock(4,2,2,Blocks.IRON_ORE);c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));
  c.resourceTask().begin(h.absolutePos(new BlockPos(4,2,2)));
  var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());c.saveWithoutId(out);
  var copy=new CompanionEntity(CompanionMod.TYPE,h.getLevel());copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
  h.assertTrue(copy.resourceTask().active() && copy.mode()==CompanionEntity.Mode.GATHER,"Assignment must survive serialization");
  h.assertTrue(copy.getInventory().countItem(Items.RAW_IRON)==0 && h.getBlockState(new BlockPos(4,2,2)).is(Blocks.IRON_ORE),"Loading a task must never fabricate its result");h.succeed();
 }
}
