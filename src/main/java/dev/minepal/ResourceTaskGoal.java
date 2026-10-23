package dev.minepal;

import java.util.*;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Explicitly assigned, bounded harvesting. Real block loot, real durability, no chunk loading. */
public final class ResourceTaskGoal extends Goal {
 private final CompanionEntity companion;
 private boolean active, mining;
 private BlockPos origin, target;
 private BlockState expected;
 private long deadline, nextSearch;
 private int harvested, failures, repath;
 private float progress;
 private Vec3 previous;
 private final Set<BlockPos> failed=new HashSet<>();
 private final List<UUID> drops=new ArrayList<>();
 public ResourceTaskGoal(CompanionEntity companion) { this.companion=companion;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
 public String begin(BlockPos selected) {
  if(!(companion.level() instanceof ServerLevel server))return "Tasks require a server.";
  if(!server.getGameRules().get(GameRules.MOB_GRIEFING))return "Resource tasks are disabled by mobGriefing.";
  boolean mine=selected!=null;
  if(mine?!companion.settings().mining():!companion.settings().gathering())return "That resource task is disabled in my settings.";
  if(companion.ownerPlayer()==null)return "My owner must be present in this dimension.";
  if(mine && (!server.hasChunkAt(selected) || companion.distanceToSqr(Vec3.atCenterOf(selected))>8*8
   || !mineable(server.getBlockState(selected))))return "Select accessible stone or ore within eight blocks of me.";
  companion.command(CompanionEntity.Mode.GATHER);
  active=true;mining=mine;origin=companion.blockPosition();deadline=server.getGameTime()+1200;
  harvested=0;failures=0;repath=0;progress=0;nextSearch=0;failed.clear();drops.clear();
  target=selected==null?null:selected.immutable();expected=target==null?null:server.getBlockState(target);
  companion.taskActivity(mine?"Preparing to mine the selected block":"Searching for nearby logs");
  return mine?"Mining assigned. I will break the selected block if my tool and the route are safe."
   :"Gathering assigned: permission to harvest up to 16 reachable logs within eight blocks of here, for at most one minute.";
 }
 public boolean active() { return active; }
 public void cancel() {
  active=false;progress=0;
  if(target!=null && companion.level() instanceof ServerLevel server)server.destroyBlockProgress(companion.getId(),target,-1);
  target=null;expected=null;drops.clear();
 }
 private void finish(String reason) {
  cancel();companion.command(companion.home()!=null?CompanionEntity.Mode.HOME:CompanionEntity.Mode.STAY);
  companion.taskActivity(reason+"; harvested "+harvested+" block(s)");
 }
 @Override public boolean canUse() {
  return active && companion.mode()==CompanionEntity.Mode.GATHER && companion.getTarget()==null && !companion.retreating();
 }
 @Override public boolean canContinueToUse() { return canUse(); }
 @Override public boolean requiresUpdateEveryTick() { return true; }
 @Override public void start() { repath=0;previous=companion.position(); }
 @Override public void stop() {
  companion.getNavigation().stop();progress=0;
  if(target!=null && companion.level() instanceof ServerLevel server)server.destroyBlockProgress(companion.getId(),target,-1);
 }
 private static boolean mineable(BlockState s) {
  return s.is(Blocks.STONE)||s.is(Blocks.DEEPSLATE)||s.is(BlockTags.ORES);
 }
 private boolean eligible(ServerLevel server,BlockPos pos,BlockState state) {
  if(!(mining?mineable(state):state.is(BlockTags.LOGS)) || !server.hasChunkAt(pos)
   || (origin!=null && Vec3.atCenterOf(pos).distanceToSqr(Vec3.atCenterOf(origin))>64)
   || !server.getWorldBorder().isWithinBounds(pos) || server.getBlockEntity(pos)!=null
   || pos.equals(companion.blockPosition().below()) || state.getDestroySpeed(server,pos)<=0
   || !(mining?mineable(state):state.is(BlockTags.LOGS)))return false;
  if(state.requiresCorrectToolForDrops() && !companion.getMainHandItem().isCorrectToolForDrops(state))return false;
  if(pos.getY()>companion.blockPosition().getY()+2)return false;
  for(var direction:net.minecraft.core.Direction.values()) {
   var adjacent=pos.relative(direction);
   if(!server.hasChunkAt(adjacent) || !server.getFluidState(adjacent).isEmpty())return false;
   var neighbor=server.getBlockState(adjacent);
   if(direction==net.minecraft.core.Direction.UP && (neighbor.getBlock() instanceof net.minecraft.world.level.block.FallingBlock))return false;
  }
  return true;
 }
 private void findLog(ServerLevel server) {
  if(server.getGameTime()<nextSearch)return;
  nextSearch=server.getGameTime()+20;
  BlockPos best=null;double distance=Double.MAX_VALUE;
  for(BlockPos candidate:BlockPos.betweenClosed(origin.offset(-8,-2,-8),origin.offset(8,3,8))) {
   if(failed.contains(candidate) || !server.hasChunkAt(candidate)
    || Vec3.atCenterOf(candidate).distanceToSqr(Vec3.atCenterOf(origin))>64)continue;
   var state=server.getBlockState(candidate);
   if(!eligible(server,candidate,state))continue;
   double d=companion.distanceToSqr(Vec3.atCenterOf(candidate));
   if(d<distance) { distance=d;best=candidate.immutable(); }
  }
  if(best==null) { finish("No reachable permitted logs nearby");return; }
  target=best;expected=server.getBlockState(best);progress=0;failures=0;repath=0;previous=companion.position();
 }
 private void abandon(String reason) {
  if(target!=null) {
   if(companion.level() instanceof ServerLevel server)server.destroyBlockProgress(companion.getId(),target,-1);
   failed.add(target);
  }
  target=null;expected=null;progress=0;companion.getNavigation().stop();
  if(mining || failed.size()>=32)finish(reason);else companion.taskActivity(reason+"; trying another log");
 }
 @Override public void tick() {
  var server=(ServerLevel)companion.level();
  if(server.getGameTime()>deadline) { finish("Resource task time limit reached");return; }
  var owner=companion.ownerPlayer();
  if(owner==null || !owner.mayBuild() || !server.getGameRules().get(GameRules.MOB_GRIEFING)
   || (mining?!companion.settings().mining():!companion.settings().gathering())) { finish("Resource task no longer permitted");return; }
  if(!drops.isEmpty()) { collect(server);return; }
  if(harvested>=(mining?1:16)) { finish("Assigned harvest complete");return; }
  if(target==null) { findLog(server);return; }
  if(!server.hasChunkAt(target)) { abandon("Target chunk is unloaded");return; }
  var state=server.getBlockState(target);
  if(!state.equals(expected) || !eligible(server,target,state)) { abandon("Block changed, unsafe terrain, or unsuitable tool");return; }
  if(server.getServer().isUnderSpawnProtection(server,target,owner) || owner.blockActionRestricted(server,target,owner.gameMode())) {
   abandon("That block is protected");return;
  }
  var center=Vec3.atCenterOf(target);
  var hit=server.clip(new ClipContext(companion.getEyePosition(),center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,companion));
  if(companion.distanceToSqr(center)>3*3 || !hit.getBlockPos().equals(target)) {
   progress=0;
   if(--repath<=0) {
    repath=20;
    if(previous!=null && companion.position().distanceToSqr(previous)<0.1)failures++;else failures=0;
    previous=companion.position();
    if(failures>=5) { abandon("Path to resource is blocked");return; }
    companion.getNavigation().moveTo(companion.getNavigation().createPath(target,1),1.0);
   }
   companion.taskActivity(mining?"Approaching selected ore/stone":"Approaching a nearby log");return;
  }
  companion.getNavigation().stop();companion.getLookControl().setLookAt(center.x,center.y,center.z);
  var tool=companion.getMainHandItem();float speed=tool.getDestroySpeed(state);
  var haste=companion.getEffect(net.minecraft.world.effect.MobEffects.HASTE);
  if(haste!=null)speed*=1+0.2F*(haste.getAmplifier()+1);
  var fatigue=companion.getEffect(net.minecraft.world.effect.MobEffects.MINING_FATIGUE);
  if(fatigue!=null)speed*=switch(fatigue.getAmplifier()) { case 0 -> 0.3F;case 1 -> 0.09F;case 2 -> 0.0027F;default -> 0.00081F; };
  if(!companion.onGround())speed/=5;
  if(companion.isUnderWater())speed/=5;
  progress+=speed/state.getDestroySpeed(server,target)/(state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)?100:30);
  server.destroyBlockProgress(companion.getId(),target,Math.min(9,(int)(progress*10)));
  companion.taskActivity(mining?"Mining the selected block":"Harvesting a log");
  if(progress<1)return;
  if(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(server,owner,target,state,null)) {
   abandon("Block protection denied the harvest");return;
  }
  var area=new AABB(target).inflate(2);var before=new HashSet<UUID>();
  for(var item:server.getEntitiesOfClass(ItemEntity.class,area))before.add(item.getUUID());
  var harvestedPos=target;
  if(!server.destroyBlock(target,false,companion)) { abandon("Block could not be harvested");return; }
  Block.dropResources(state,server,harvestedPos,null,companion,tool);
  var component=tool.get(DataComponents.TOOL);
  if(!tool.isEmpty())tool.hurtAndBreak(component==null?1:component.damagePerBlock(),companion,EquipmentSlot.MAINHAND);
  PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(server,owner,harvestedPos,state,null);
  for(var item:server.getEntitiesOfClass(ItemEntity.class,area))if(!before.contains(item.getUUID()))drops.add(item.getUUID());
  server.destroyBlockProgress(companion.getId(),harvestedPos,-1);
  harvested++;target=null;expected=null;progress=0;
 }
 private void collect(ServerLevel server) {
  UUID id=drops.getFirst();var entity=server.getEntityInAnyDimension(id);
  if(!(entity instanceof ItemEntity item) || item.level()!=server || !item.isAlive()) { drops.removeFirst();return; }
  if(companion.distanceToSqr(item)>12*12) { finish("Dropped resource is no longer nearby");return; }
  if(companion.distanceToSqr(item)>2.25) {
   if(--repath<=0) { repath=20;companion.getNavigation().moveTo(item,1); }
   companion.taskActivity("Collecting my harvested drops");return;
  }
  if(item.hasPickUpDelay())return;
  var remainder=companion.getInventory().addItem(item.getItem().copy());
  if(remainder.isEmpty()) { item.discard();drops.removeFirst(); }
  else { item.setItem(remainder);finish("Inventory full; remaining drops left in the world"); }
 }
 public void save(ValueOutput out) {
  out.putBoolean("Active",active);if(!active)return;
  out.putBoolean("Mining",mining);out.putLong("Origin",origin.asLong());out.putLong("Deadline",deadline);out.putInt("Harvested",harvested);
  if(target!=null) { out.putLong("Target",target.asLong());out.store("Expected",BlockState.CODEC,expected); }
  out.store("Drops",Codec.STRING.listOf(),drops.stream().map(UUID::toString).toList());
 }
 public void load(ValueInput in) {
  active=in.getBooleanOr("Active",false);mining=in.getBooleanOr("Mining",false);
  origin=in.getLong("Origin").map(BlockPos::of).orElse(companion.blockPosition());
  deadline=in.getLongOr("Deadline",0);harvested=Math.clamp(in.getIntOr("Harvested",0),0,16);
  target=in.getLong("Target").map(BlockPos::of).orElse(null);expected=in.read("Expected",BlockState.CODEC).orElse(null);
  if(expected==null)target=null;
  drops.clear();for(String value:in.read("Drops",Codec.STRING.listOf()).orElse(List.of()).stream().limit(64).toList()) {
   try { drops.add(UUID.fromString(value)); } catch(IllegalArgumentException ignored) { }
  }
 }
}
