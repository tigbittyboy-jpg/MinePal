package dev.minepal;

import java.util.*;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;

public class CompanionTaskReliabilityTests {
 private static CompanionEntity prepare(GameTestHelper h){
  for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));var owner=h.makeMockServerPlayerInLevel();owner.snapTo(c.position());c.claim(owner);return c;
 }
 @GameTest(maxTicks=180,padding=12) public void server_attack_callback_registers_intent_without_faking_a_hit(GameTestHelper h){
  var c=prepare(h);var goat=h.spawn(EntityTypes.GOAT,new BlockPos(4,2,2));goat.setNoAi(true);
  AttackEntityCallback.EVENT.invoker().interact(c.ownerPlayer(),h.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,goat,new net.minecraft.world.phys.EntityHitResult(goat));
  h.assertTrue(goat.getHealth()==goat.getMaxHealth(),"Registering attack intent must not apply arbitrary damage");
  h.succeedWhen(()->h.assertTrue(goat.getLastHurtByMob()==c && goat.getHealth()<goat.getMaxHealth(),"Intent callback must lead to a real approached melee hit"));
 }
 @GameTest(padding=12) public void progressive_quantity_and_exploration_survive_reload(GameTestHelper h){
  var c=prepare(h);c.resourceTask().assign(ResourceTaskGoal.Objective.WOOD,64);c.taskLedger().add(14);c.taskLedger().state(TaskLedger.Phase.SEARCHING,"Looking for the next tree");
  var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());c.saveWithoutId(out);
  var copy=new CompanionEntity(CompanionMod.TYPE,h.getLevel());copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
  h.assertTrue(copy.resourceTask().active() && copy.mode()==CompanionEntity.Mode.GATHER && copy.taskLedger().requested()==64 && copy.taskLedger().collected()==14,"Ordinary reload must preserve the assignment and completed collection count");
  h.assertTrue(copy.getInventory().countItem(Items.OAK_LOG)==0,"Progress metadata must never create items");h.succeed();
 }
 @GameTest(maxTicks=80,padding=12) public void unloaded_drop_waits_without_duplication_or_false_completion(GameTestHelper h){
  var c=prepare(h);c.resourceTask().assign(ResourceTaskGoal.Objective.WOOD,64);
  var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());c.saveWithoutId(saved);
  var tag=saved.buildResult();var child=tag.getCompoundOrEmpty("CompanionResourceTask");
  var id=UUID.randomUUID();var far=new BlockPos(25_000_000,64,25_000_000);
  var dropData=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());
  dropData.store("Drops",Codec.STRING.listOf(),List.of(id.toString()));dropData.store("QuotaDrops",Codec.STRING.listOf(),List.of(id.toString()));
  dropData.store("DropPositions",Codec.STRING.listOf(),List.of(id+"|"+far.asLong()));
  child.merge(dropData.buildResult());tag.put("CompanionResourceTask",child);
  c.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));
  h.runAfterDelay(30,()->{
   h.assertTrue(c.resourceTask().active() && c.taskLedger().phase()==TaskLedger.Phase.BLOCKED && c.taskLedger().collected()==0,"Known-unloaded drops must retain the task without claiming success");
   h.assertTrue(c.getInventory().countItem(Items.OAK_LOG)==0 && !h.getLevel().hasChunkAt(far),"No item or chunk may be fabricated");h.succeed();
  });
 }
}
