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
 public enum Objective { WOOD,STONE,ORE }
 private boolean progressive,treePickupAllowed;
 private Objective objective=Objective.WOOD;
 private String taskDimension="";
 private final ExplorationPlanner exploration=new ExplorationPlanner();
 private final TreeHarvestPlan tree=new TreeHarvestPlan();
 private final ExcavationPlanner excavation=new ExcavationPlanner();
 private long blockedRetry,nextReposition;
 private boolean active, mining, nearestMining;
 private BlockPos lastBreaking;
 private int cleared;
 private BlockPos origin, target;
 private BlockState expected;
 private long deadline, nextSearch;
 private int harvested, failures, repath;
 private float progress;
 private Vec3 previous;
 private final Set<BlockPos> failed=new HashSet<>();
 private final List<UUID> drops=new ArrayList<>();
 private final Set<UUID> quotaDrops=new HashSet<>();
 private final java.util.Map<UUID,BlockPos> dropPositions=new java.util.HashMap<>();
 public ResourceTaskGoal(CompanionEntity companion) { this.companion=companion;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
 public String begin(BlockPos selected) {
  if(!(companion.level() instanceof ServerLevel server))return "Tasks require a server.";
  if(!server.getGameRules().get(GameRules.MOB_GRIEFING) || !server.getGameRules().get(GameRules.BLOCK_DROPS))return "Resource tasks require mobGriefing and blockDrops.";
  boolean mine=selected!=null;
  if(mine?!companion.settings().mining():!companion.settings().gathering())return "That resource task is disabled in my settings.";
  if(companion.ownerPlayer()==null)return "My owner must be present in this dimension.";
  if(mine && (!server.hasChunkAt(selected) || companion.distanceToSqr(Vec3.atCenterOf(selected))>16*16
   || !mineable(server.getBlockState(selected))))return "Select accessible stone or ore within 16 blocks of me.";
  companion.command(CompanionEntity.Mode.GATHER);
  progressive=false;active=true;mining=mine;nearestMining=false;cleared=0;lastBreaking=null;origin=companion.blockPosition();deadline=server.getGameTime()+1200;
  harvested=0;failures=0;repath=0;progress=0;nextSearch=0;failed.clear();drops.clear();quotaDrops.clear();dropPositions.clear();treePickupAllowed=false;
  target=selected==null?null:selected.immutable();expected=target==null?null:server.getBlockState(target);
  companion.taskLedger().begin(mine?"Mine selected block":"Local log harvesting",mine?1:16);
  companion.taskActivity(mine?"Preparing to mine the selected block":"Searching for nearby logs");
  return mine?"Mining assigned. I will break the selected block if my tool and the route are safe."
   :"Gathering assigned: permission to harvest up to 16 reachable logs within eight blocks of here, for at most one minute.";
 }
 public String assign(Objective objective,int quantity) {
  if(quantity<0 || quantity>4096)return "Choose a quantity from 0 (open-ended) to 4096.";
  if(!(companion.level() instanceof ServerLevel server) || companion.ownerPlayer()==null)return "My owner must be present.";
  if(!server.getGameRules().get(GameRules.MOB_GRIEFING) || !server.getGameRules().get(GameRules.BLOCK_DROPS) || (objective==Objective.WOOD?!companion.settings().gathering():!companion.settings().mining()))return "This resource task is disabled.";
  companion.command(CompanionEntity.Mode.GATHER);cancel();
  active=true;progressive=true;taskDimension=server.dimension().identifier().toString();this.objective=objective;mining=objective!=Objective.WOOD;nearestMining=true;
  origin=companion.blockPosition();harvested=cleared=failures=repath=0;progress=0;target=null;expected=null;nextSearch=blockedRetry=0;failed.clear();drops.clear();quotaDrops.clear();dropPositions.clear();treePickupAllowed=false;
  tree.clear();
  excavation.arrived();exploration.reset(origin);
  companion.taskLedger().begin(objective==Objective.WOOD?"Gather wood":objective==Objective.STONE?"Mine stone":"Mine exposed ores",quantity);
  companion.taskActivity("Planning resource exploration");
  return "Assigned "+(quantity==0?"open-ended":quantity)+" "+objective.name().toLowerCase(java.util.Locale.ROOT)+". Work resumes after combat; unloaded or unsafe terrain is reported.";
 }
 private void phase(TaskLedger.Phase phase,String detail) { companion.taskLedger().state(phase,detail);companion.taskActivity(detail); }
 private void blocked(String detail) { companion.getNavigation().stop();phase(TaskLedger.Phase.BLOCKED,detail); }
 private boolean visibleDiscovery(ServerLevel server,BlockPos p) {
  var hit=server.clip(new ClipContext(companion.getEyePosition(),Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,companion));
  return hit.getBlockPos().equals(p) || (server.hasChunkAt(hit.getBlockPos()) && server.getBlockState(hit.getBlockPos()).is(BlockTags.LEAVES)
   && Vec3.atCenterOf(hit.getBlockPos()).distanceToSqr(Vec3.atCenterOf(p))<=8*8);
 }
 private boolean rootCandidate(ServerLevel server,BlockPos p) {
  return server.getBlockState(p).is(BlockTags.LOGS) && server.hasChunkAt(p.below()) && server.getBlockState(p.below()).is(BlockTags.DIRT);
 }
 private boolean cNavigationRunning(){return companion.getNavigation().isInProgress();}
 private boolean stepAside(ServerLevel server) {
  if(server.getGameTime()<nextReposition || cNavigationRunning())return true;nextReposition=server.getGameTime()+20;
  for(int y:new int[]{-1,0,-2})for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}}) {
   var p=companion.blockPosition().offset(d[0]*2,y,d[1]*2);
   if(!SafeTravel.safeLanding(companion,server,p))continue;var path=companion.getNavigation().createPath(p,0);
   if(path!=null && path.canReach()){companion.getNavigation().moveTo(path,1);phase(TaskLedger.Phase.TRAVELING,"Repositioning before harvesting the supporting log");return true;}
  }
  blocked("A supporting log remains; no safe step-aside route");return false;
 }
 private void progressiveFind(ServerLevel server) {
  if(server.getGameTime()<nextSearch)return;nextSearch=server.getGameTime()+5;
  if(objective==Objective.WOOD && tree.active() && !treePickupAllowed) {
   tree.reconcile(server);
   for(var p:tree.nearest(companion))if(server.hasChunkAt(p) && !failed.contains(p)) {
    if(p.equals(companion.blockPosition().below())){stepAside(server);return;}
    treePickupAllowed=false;target=p;expected=server.getBlockState(p);failures=0;repath=0;previous=companion.position();return;
   }
   if(tree.active() || tree.incomplete()) {
    treePickupAllowed=true;blocked("Tree still has "+tree.remaining()+" logs; a safe access route or loaded crown is needed");
    if(server.getGameTime()>=blockedRetry){blockedRetry=server.getGameTime()+200;failed.clear();}return;
   }
  }
  if(objective==Objective.WOOD && tree.incomplete() && !tree.active()){blocked("Unloaded or oversized tree crown remains unverified");return;}
  if(companion.taskLedger().quotaMet() && !tree.active() && !excavation.cutting()) { finish("Requested resources collected");return; }
  if(objective==Objective.STONE && excavation.active()) {
   while(excavation.cutting() && server.hasChunkAt(excavation.next()) && server.getBlockState(excavation.next()).isAir())excavation.removed(excavation.next());
   if(excavation.cutting()) { target=excavation.next();expected=server.getBlockState(target);failures=0;repath=0;return; }
   if(companion.distanceToSqr(Vec3.atBottomCenterOf(excavation.landing()))>1) {
    companion.getNavigation().moveTo(companion.getNavigation().createPath(excavation.landing(),0),1);phase(TaskLedger.Phase.TRAVELING,"Descending the inspected staircase");return;
   }
   excavation.arrived();
  }
  BlockPos known=null;
  if(objective==Objective.WOOD)known=companion.spatialMemory().locations(server.dimension().identifier().toString(),SpatialMemory.Kind.FOREST).stream()
   .filter(p->!failed.contains(p) && server.hasChunkAt(p) && rootCandidate(server,p))
   .min(Comparator.comparingDouble(p->companion.distanceToSqr(Vec3.atCenterOf(p)))).orElse(null);
  BlockPos found=known!=null?known:exploration.scan(companion,objective.name(),p->{
   if(failed.contains(p))return false;var state=server.getBlockState(p);
   if(objective==Objective.WOOD)return rootCandidate(server,p) && visibleDiscovery(server,p);
   if(p.getY()<companion.blockPosition().getY() || !(objective==Objective.STONE?(state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE)):state.is(BlockTags.ORES)))return false;
   if(!companion.getInventory().hasToolFor(state) || !visibleDiscovery(server,p))return false;
   if(objective==Objective.STONE && !server.getBlockState(p.above()).isAir())return false;
   for(var d:net.minecraft.core.Direction.values()) {var q=p.relative(d);if(server.hasChunkAt(q) && server.getBlockState(q).isAir())return true;}
   return false;
  });
  if(found!=null) {
   if(objective==Objective.WOOD) {
    if(!tree.detect(server,found)){failed.add(found);tree.clear();phase(TaskLedger.Phase.SEARCHING,"Tree shape/canopy needs manual review; seeking another forest");return;}
    companion.spatialMemory().remember(server.dimension().identifier().toString(),SpatialMemory.Kind.FOREST,found);
    target=tree.nearest(companion).getFirst();
   } else {target=found;companion.spatialMemory().remember(server.dimension().identifier().toString(),SpatialMemory.Kind.CAVE,found);}
   expected=server.getBlockState(target);failures=0;repath=0;previous=companion.position();phase(TaskLedger.Phase.TRAVELING,"Traveling to discovered resources");return;
  }
  if(objective==Objective.STONE && excavation.plan(companion)) {target=excavation.next();expected=server.getBlockState(target);phase(TaskLedger.Phase.EXCAVATING,"Planning a safe staircase through ordinary terrain");return;}
  if(exploration.travel(companion))phase(TaskLedger.Phase.SEARCHING,"Exploring loaded terrain for "+objective.name().toLowerCase(java.util.Locale.ROOT));
  else if(exploration.scanning() || exploration.hasLoadedFrontier(companion))phase(TaskLedger.Phase.SEARCHING,"Inspecting available loaded terrain");
  else phase(TaskLedger.Phase.BLOCKED,"Waiting for a reachable route or newly loaded terrain");
 }
 public String beginNearestMine() {
  if(!(companion.level() instanceof ServerLevel server) || companion.ownerPlayer()==null)return "My owner must be present.";
  if(!companion.settings().mining() || !server.getGameRules().get(GameRules.MOB_GRIEFING))return "Mining is disabled.";
  var selected=nearestMine(server,companion.blockPosition());
  if(selected==null)return "No exposed, reachable ore/stone with a suitable tool within 16 blocks.";
  String result=begin(selected);if(!active || !mining || !selected.equals(target))return result;nearestMining=true;
  return "Searching and mining up to eight nearby exposed ore/stone blocks within 16 blocks.";
 }
 private BlockPos nearestMine(ServerLevel server,BlockPos center) {
  var candidates=new ArrayList<BlockPos>();
  for(var p:BlockPos.betweenClosed(center.offset(-16,-3,-16),center.offset(16,3,16))) {
   if(!server.hasChunkAt(p) || failed.contains(p) || Vec3.atCenterOf(p).distanceToSqr(Vec3.atCenterOf(center))>256 || companion.distanceToSqr(Vec3.atCenterOf(p))>256
    || p.getY()<companion.blockPosition().getY())continue;
   var state=server.getBlockState(p);
   if(!mineable(state) || (state.requiresCorrectToolForDrops() && !companion.getMainHandItem().isCorrectToolForDrops(state)))continue;
   boolean exposed=false;
   for(var d:net.minecraft.core.Direction.values()) {
    var q=p.relative(d);if(server.hasChunkAt(q) && (server.getBlockState(q).isAir() || server.getBlockState(q).is(BlockTags.LEAVES)))exposed=true;
   }
   if(exposed)candidates.add(p.immutable());
  }
  candidates.sort(Comparator.<BlockPos>comparingInt(p->server.getBlockState(p).is(BlockTags.ORES)?0:1)
   .thenComparingDouble(p->companion.distanceToSqr(Vec3.atCenterOf(p))));
  for(var p:candidates.stream().limit(16).toList()) {
   var hit=server.clip(new ClipContext(companion.getEyePosition(),Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,companion));
   if(!hit.getBlockPos().equals(p) && !clearable(server,hit.getBlockPos()))continue;
   var path=companion.getNavigation().createPath(p,1);
   if(!companion.onGround() || (path!=null && path.canReach()) || companion.distanceToSqr(Vec3.atCenterOf(p))<=9 || clearable(server,hit.getBlockPos()))return p;
  }
  return null;
 }
 String searchDiagnostics(){return exploration.diagnostics()+" tree="+tree.remaining()+" searched="+companion.spatialMemory().searchedCount();}
 public boolean active() { return active; }
 public void cancel() {
  if(active)companion.taskLedger().state(TaskLedger.Phase.CANCELED,"Resource task canceled");
  if(companion.level() instanceof ServerLevel world)for(var id:drops) {
   if(world.getEntityInAnyDimension(id) instanceof ItemEntity item)item.setTarget(null);
  }
  active=false;progress=0;
  if(lastBreaking!=null && companion.level() instanceof ServerLevel world)world.destroyBlockProgress(companion.getId(),lastBreaking,-1);lastBreaking=null;
  if(target!=null && companion.level() instanceof ServerLevel server)server.destroyBlockProgress(companion.getId(),target,-1);
  target=null;expected=null;drops.clear();quotaDrops.clear();dropPositions.clear();
 }
 private void finish(String reason) {
  cancel();companion.command(companion.home()!=null?CompanionEntity.Mode.HOME:CompanionEntity.Mode.STAY);
  companion.taskActivity(reason+"; harvested "+harvested+" block(s)");
  companion.taskLedger().state(progressive && !companion.taskLedger().quotaMet()?TaskLedger.Phase.BLOCKED:TaskLedger.Phase.COMPLETED,reason);
  if(harvested>0 && companion.settings().basicCrafting() && companion.getInventory().hasAnyMatching(stack->stack.is(net.minecraft.tags.ItemTags.LOGS)
   || stack.is(net.minecraft.tags.ItemTags.PLANKS) || stack.is(net.minecraft.world.item.Items.STICK)
   || stack.is(net.minecraft.world.item.Items.COBBLESTONE) || stack.is(net.minecraft.world.item.Items.IRON_INGOT)
   || stack.is(net.minecraft.world.item.Items.DIAMOND)))companion.workTask().prepare(false);
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
  if(lastBreaking!=null && companion.level() instanceof ServerLevel server)server.destroyBlockProgress(companion.getId(),lastBreaking,-1);lastBreaking=null;
 }
 private static boolean mineable(BlockState s) {
  return s.is(Blocks.STONE)||s.is(Blocks.DEEPSLATE)||s.is(BlockTags.ORES);
 }
 private boolean eligible(ServerLevel server,BlockPos pos,BlockState state) {
  boolean excavating=progressive && excavation.contains(pos);
  if(!(excavating?ExcavationPlanner.safeCut(companion,server,pos):(mining?mineable(state):state.is(BlockTags.LOGS))) || !server.hasChunkAt(pos)
   || (!progressive && origin!=null && Vec3.atCenterOf(pos).distanceToSqr(Vec3.atCenterOf(origin))>(mining?256:64))
   || !server.getWorldBorder().isWithinBounds(pos) || server.getBlockEntity(pos)!=null
   || pos.equals(companion.blockPosition().below()) || state.getDestroySpeed(server,pos)<=0
)return false;
  if(!companion.getInventory().hasToolFor(state))return false;
  if(!progressive && pos.getY()>companion.blockPosition().getY()+2)return false;
  for(var direction:net.minecraft.core.Direction.values()) {
   var adjacent=pos.relative(direction);
   if(!server.hasChunkAt(adjacent) || !server.getFluidState(adjacent).isEmpty())return false;
   var neighbor=server.getBlockState(adjacent);
   if(direction==net.minecraft.core.Direction.UP && (neighbor.getBlock() instanceof net.minecraft.world.level.block.FallingBlock))return false;
  }
  return true;
 }
 private boolean clearable(ServerLevel server,BlockPos pos) {
  if(!companion.settings().clearLeaves() || (!progressive && cleared>=12) || !server.hasChunkAt(pos)
   || !server.getBlockState(pos).is(BlockTags.LEAVES) || server.getBlockEntity(pos)!=null
   || companion.distanceToSqr(Vec3.atCenterOf(pos))>9 || pos.equals(companion.blockPosition().below())
   || !server.getWorldBorder().isWithinBounds(pos))return false;
  if(!progressive && origin!=null && Vec3.atCenterOf(pos).distanceToSqr(Vec3.atCenterOf(origin))>(mining?256:64))return false;
  for(var d:net.minecraft.core.Direction.values()) {
   var q=pos.relative(d);if(!server.hasChunkAt(q) || !server.getFluidState(q).isEmpty())return false;
   if(d==net.minecraft.core.Direction.UP && server.getBlockState(q).getBlock() instanceof net.minecraft.world.level.block.FallingBlock)return false;
  }
  return true;
 }
 private void findLog(ServerLevel server) {
  if(progressive){progressiveFind(server);return;}
  if(server.getGameTime()<nextSearch)return;
  nextSearch=server.getGameTime()+20;
  if(mining) {
   var selected=nearestMine(server,origin);
   if(selected==null){finish("No more exposed reachable ore/stone nearby");return;}
   target=selected;expected=server.getBlockState(selected);progress=0;failures=0;repath=0;previous=companion.position();return;
  }
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
   failed.add(target);if(progressive && excavation.contains(target))excavation.arrived();
  }
  target=null;expected=null;progress=0;companion.getNavigation().stop();
  if(progressive){phase(TaskLedger.Phase.BLOCKED,reason+"; retaining the assigned objective");return;}
  if((mining && !nearestMining) || failed.size()>=32)finish(reason);else companion.taskActivity(reason+(mining?"; trying another ore/stone":"; trying another log"));
 }
 @Override public void tick() {
  var server=(ServerLevel)companion.level();
  if(progressive && !taskDimension.equals(server.dimension().identifier().toString())){blocked("Task belongs to another dimension; cancel and reassign here");return;}
  if(!progressive && server.getGameTime()>deadline) { finish("Resource task time limit reached");return; }
  var owner=companion.ownerPlayer();
  if(owner==null || !owner.mayBuild() || !server.getGameRules().get(GameRules.MOB_GRIEFING) || !server.getGameRules().get(GameRules.BLOCK_DROPS)
   || (mining?!companion.settings().mining():!companion.settings().gathering())) { if(progressive)blocked("Waiting for owner permission/presence or the required setting");else finish("Resource task no longer permitted");return; }
  if(progressive && companion.taskLedger().quotaMet() && !tree.active() && !excavation.cutting()
   && drops.stream().noneMatch(quotaDrops::contains)){finish("Requested resources collected");return;}
  // Finish a connected tree/cut segment before chasing drops that may be resting on its remaining blocks.
  if(!drops.isEmpty() && !(progressive && (excavation.cutting() || (objective==Objective.WOOD && tree.active() && !treePickupAllowed)))) { collect(server);return; }
  if(!progressive && harvested>=(mining?(nearestMining?8:1):16)) { finish("Assigned harvest complete");return; }
  if(target==null) { findLog(server);return; }
  if(!server.hasChunkAt(target)) { abandon("Target chunk is unloaded");return; }
  if(progressive && objective==Objective.WOOD && target.equals(companion.blockPosition().below())){stepAside(server);return;}
  var state=server.getBlockState(target);
  companion.getInventory().equipForBlock(state);
  if(!state.equals(expected) || !eligible(server,target,state)) { abandon("Block changed, unsafe terrain, or unsuitable tool");return; }
  var center=Vec3.atCenterOf(target);
  var hit=server.clip(new ClipContext(companion.getEyePosition(),center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,companion));
  boolean obstruction=!hit.getBlockPos().equals(target) && clearable(server,hit.getBlockPos());
  BlockPos breaking=obstruction?hit.getBlockPos():target;
  boolean inReach=progressive?companion.getEyePosition().distanceToSqr(center)<=4.5*4.5:companion.distanceToSqr(center)<=9;
  if(!obstruction && (!inReach || !hit.getBlockPos().equals(target))) {
   progress=0;
   if(--repath<=0) {
    repath=20;
    if(previous!=null && companion.position().distanceToSqr(previous)<0.1)failures++;else failures=0;
    previous=companion.position();
    if(failures>=5) { abandon("Path to resource is blocked");return; }
    companion.getNavigation().moveTo(companion.getNavigation().createPath(target,1),1.0);
   }
   phase(TaskLedger.Phase.TRAVELING,mining?"Approaching nearby ore/stone":"Approaching connected tree logs");return;
  }
  if(server.getServer().isUnderSpawnProtection(server,breaking,owner) || owner.blockActionRestricted(server,breaking,owner.gameMode())) {
   abandon("That block is protected");return;
  }
  if(!breaking.equals(lastBreaking)) {
   if(lastBreaking!=null)server.destroyBlockProgress(companion.getId(),lastBreaking,-1);
   lastBreaking=breaking;progress=0;
  }
  state=server.getBlockState(breaking);center=Vec3.atCenterOf(breaking);
  companion.getNavigation().stop();companion.getLookControl().setLookAt(center.x,center.y,center.z);
  var tool=companion.getMainHandItem();float speed=tool.getDestroySpeed(state);
  var haste=companion.getEffect(net.minecraft.world.effect.MobEffects.HASTE);
  if(haste!=null)speed*=1+0.2F*(haste.getAmplifier()+1);
  var fatigue=companion.getEffect(net.minecraft.world.effect.MobEffects.MINING_FATIGUE);
  if(fatigue!=null)speed*=switch(fatigue.getAmplifier()) { case 0 -> 0.3F;case 1 -> 0.09F;case 2 -> 0.0027F;default -> 0.00081F; };
  if(!companion.onGround())speed/=5;
  if(companion.isUnderWater())speed/=5;
  progress+=speed/Math.max(0.01F,state.getDestroySpeed(server,breaking))/(state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)?100:30);
  server.destroyBlockProgress(companion.getId(),breaking,Math.min(9,(int)(progress*10)));
  phase(obstruction?TaskLedger.Phase.CLEARING_OBSTRUCTIONS:excavation.contains(breaking)?TaskLedger.Phase.EXCAVATING:TaskLedger.Phase.WORKING,obstruction?"Clearing permitted leaves":mining?"Mining terrain/resources":"Harvesting connected logs");
  if(progress<1)return;
  if(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(server,owner,breaking,state,null)) {
   abandon("Block protection denied the harvest");return;
  }
  var area=new AABB(breaking).inflate(2);var before=new HashSet<UUID>();
  for(var item:server.getEntitiesOfClass(ItemEntity.class,area))before.add(item.getUUID());
  var harvestedPos=breaking;
  if(!server.destroyBlock(breaking,false,companion)) { abandon("Block could not be harvested");return; }
  Block.dropResources(state,server,harvestedPos,null,companion,tool);
  var component=tool.get(DataComponents.TOOL);
  if(!tool.isEmpty())tool.hurtAndBreak(component==null?1:component.damagePerBlock(),companion,EquipmentSlot.MAINHAND);
  PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(server,owner,harvestedPos,state,null);
  for(var item:server.getEntitiesOfClass(ItemEntity.class,area))if(!before.contains(item.getUUID())) {
   drops.add(item.getUUID());dropPositions.put(item.getUUID(),item.blockPosition());item.setTarget(companion.getUUID());
   if(!progressive || (objective==Objective.WOOD?state.is(BlockTags.LOGS):objective==Objective.STONE?
    (state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE)):state.is(BlockTags.ORES)))quotaDrops.add(item.getUUID());
  }
  server.destroyBlockProgress(companion.getId(),harvestedPos,-1);
  lastBreaking=null;progress=0;
  if(obstruction)cleared++;else { harvested++;tree.removed(harvestedPos);excavation.removed(harvestedPos);target=null;expected=null; }
 }
 private void collect(ServerLevel server) {
  if(progressive)for(int i=0;i<drops.size();i++)if(quotaDrops.contains(drops.get(i))){java.util.Collections.swap(drops,0,i);break;}
  UUID id=drops.getFirst();var entity=server.getEntityInAnyDimension(id);
  if(entity==null && progressive && dropPositions.containsKey(id) && !server.hasChunkAt(dropPositions.get(id))) {
   blocked("Owned drops are in unloaded terrain; waiting without recreating resources");return;
  }
  if(!(entity instanceof ItemEntity item) || item.level()!=server || !item.isAlive()) { drops.removeFirst();quotaDrops.remove(id);dropPositions.remove(id);return; }
  dropPositions.put(id,item.blockPosition());
  if(companion.distanceToSqr(item)>12*12) { finish("Dropped resource is no longer nearby");return; }
  if(companion.distanceToSqr(item)>2.25) {
   if(--repath<=0) { repath=20;companion.getNavigation().moveTo(companion.getNavigation().createPath(item.blockPosition(),0),1); }
   phase(TaskLedger.Phase.COLLECTING,"Collecting my actual harvested drops");return;
  }
  if(item.hasPickUpDelay())return;
  var original=item.getItem().copy();var remainder=companion.getInventory().addItem(original.copy());
  int accepted=original.getCount()-remainder.getCount();
  boolean counts=!progressive || quotaDrops.contains(id);
  if(counts)companion.taskLedger().add(accepted);
  if(remainder.isEmpty()) { item.discard();drops.removeFirst();quotaDrops.remove(id);dropPositions.remove(id); }
  else { item.setItem(remainder);if(progressive)blocked("Inventory full; owned drops remain in the world until space is available");else finish("Inventory full; remaining drops left in the world"); }
 }
 public void save(ValueOutput out) {
  out.putBoolean("Active",active);if(!active)return;
  out.putString("TaskDimension",taskDimension);
  out.putBoolean("TreePickupAllowed",treePickupAllowed);
  out.store("DropPositions",Codec.STRING.listOf(),dropPositions.entrySet().stream().map(e->e.getKey()+"|"+e.getValue().asLong()).toList());
  out.putBoolean("Progressive",progressive);out.putString("Objective",objective.name());
  exploration.save(out.child("Exploration"));tree.save(out.child("Tree"));excavation.save(out.child("Excavation"));
  out.putBoolean("Mining",mining);out.putBoolean("NearestMining",nearestMining);out.putInt("Cleared",cleared);out.putLong("Origin",origin.asLong());out.putLong("Deadline",deadline);out.putInt("Harvested",harvested);
  if(target!=null) { out.putLong("Target",target.asLong());out.store("Expected",BlockState.CODEC,expected); }
  out.store("Drops",Codec.STRING.listOf(),drops.stream().map(UUID::toString).toList());
  out.store("QuotaDrops",Codec.STRING.listOf(),quotaDrops.stream().map(UUID::toString).toList());
 }
 public void load(ValueInput in) {
  taskDimension=in.getStringOr("TaskDimension",companion.level().dimension().identifier().toString());
  treePickupAllowed=in.getBooleanOr("TreePickupAllowed",false);dropPositions.clear();
  for(String value:in.read("DropPositions",Codec.STRING.listOf()).orElse(List.of()).stream().limit(4096).toList()) {
   int split=value.indexOf('|');if(split<0)continue;try{dropPositions.put(UUID.fromString(value.substring(0,split)),BlockPos.of(Long.parseLong(value.substring(split+1))));}catch(IllegalArgumentException ignored){}
  }
  progressive=in.getBooleanOr("Progressive",false);try{objective=Objective.valueOf(in.getStringOr("Objective","WOOD"));}catch(IllegalArgumentException ignored){objective=Objective.WOOD;}
  exploration.load(in.childOrEmpty("Exploration"));tree.load(in.childOrEmpty("Tree"));excavation.load(in.childOrEmpty("Excavation"));
  active=in.getBooleanOr("Active",false);mining=in.getBooleanOr("Mining",false);nearestMining=in.getBooleanOr("NearestMining",false);cleared=Math.clamp(in.getIntOr("Cleared",0),0,12);
  origin=in.getLong("Origin").map(BlockPos::of).orElse(companion.blockPosition());
  deadline=in.getLongOr("Deadline",0);harvested=Math.clamp(in.getIntOr("Harvested",0),0,16);
  target=in.getLong("Target").map(BlockPos::of).orElse(null);expected=in.read("Expected",BlockState.CODEC).orElse(null);
  if(expected==null)target=null;
  quotaDrops.clear();for(String value:in.read("QuotaDrops",Codec.STRING.listOf()).orElse(List.of()).stream().limit(4096).toList()){try{quotaDrops.add(UUID.fromString(value));}catch(IllegalArgumentException ignored){}}
  drops.clear();for(String value:in.read("Drops",Codec.STRING.listOf()).orElse(List.of()).stream().limit(4096).toList()) {
   try { drops.add(UUID.fromString(value)); } catch(IllegalArgumentException ignored) { }
  }
 }
}
