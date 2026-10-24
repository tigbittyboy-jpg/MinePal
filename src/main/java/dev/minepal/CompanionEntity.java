package dev.minepal;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Server-owned companion. Vanilla UUID and custom name are persisted by Entity. */
public class CompanionEntity extends PathfinderMob implements net.minecraft.world.entity.npc.InventoryCarrier {
 public enum Mode { STAY, FOLLOW, HOME, GATHER, WORK }
 private final CompanionInventory inventory = new CompanionInventory(this);
 private final CompanionSettings settings = new CompanionSettings();
 private final SharedStorage sharedStorage = new SharedStorage(this);
 private boolean retreating;
 private long teleportAfter;
 private BlockPos combatAnchor;
 private ResourceTaskGoal resourceTask;
 private WorkstationGoal workTask;
 private long nextMenuAction,nextMenuRefresh;
 private UUID owner;
 private BlockPos home;
 private Mode mode = Mode.STAY;
 private String activity = "Waiting";
 public CompanionEntity(EntityType<? extends PathfinderMob> type, Level level) {
  super(type, level);
  setPersistenceRequired();
  getNavigation().setCanFloat(true);
  getNavigation().setCanOpenDoors(true);
  for(var danger:new net.minecraft.world.level.pathfinder.PathType[]{
    net.minecraft.world.level.pathfinder.PathType.LAVA,net.minecraft.world.level.pathfinder.PathType.FIRE,
    net.minecraft.world.level.pathfinder.PathType.DAMAGING,net.minecraft.world.level.pathfinder.PathType.POWDER_SNOW})setPathfindingMalus(danger,-1);
  setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.FIRE_IN_NEIGHBOR,16);
  setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DAMAGING_IN_NEIGHBOR,16);
 }
 public static AttributeSupplier.Builder attributes() {
  return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,0.28)
   .add(Attributes.ATTACK_DAMAGE,3).add(Attributes.ATTACK_SPEED,4).add(Attributes.FOLLOW_RANGE,32);
 }
 @Override protected void registerGoals() {
  goalSelector.addGoal(0,new FloatGoal(this));
  goalSelector.addGoal(1,new AvoidEntityGoal<LivingEntity>(this,LivingEntity.class,10,1.2,1.4,
   candidate->candidate instanceof net.minecraft.world.entity.monster.Enemy) {
    @Override public boolean canUse() {
     return (retreating() || settings().combat()==CompanionSettings.Combat.PASSIVE) && super.canUse();
    }
    @Override public boolean canContinueToUse() {
     return (retreating() || settings().combat()==CompanionSettings.Combat.PASSIVE) && super.canContinueToUse();
    }
  });
  goalSelector.addGoal(2,new MeleeAttackGoal(this,1.1,false) {
   @Override protected int getAttackInterval() {
    return Math.max(20,(int)Math.ceil(20/Math.max(0.1,getAttributeValue(Attributes.ATTACK_SPEED))));
   }
  });
  goalSelector.addGoal(3,new TravelGoal());
  resourceTask=new ResourceTaskGoal(this);
  goalSelector.addGoal(4,resourceTask);
  workTask=new WorkstationGoal(this);goalSelector.addGoal(4,workTask);
  goalSelector.addGoal(5,new OpenDoorGoal(this,true));
  goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,6));
  goalSelector.addGoal(7,new RandomLookAroundGoal(this));
  targetSelector.addGoal(1,new CompanionCombatGoal(this));
 }
 @Override public CompanionInventory getInventory() { return inventory; }
 public WorkstationGoal workTask() { return workTask; }
 public boolean allowMenuAction(boolean refresh) {
  long time=level().getGameTime();
  if(refresh){if(time<nextMenuRefresh)return false;nextMenuRefresh=time+20;return true;}
  if(time<nextMenuAction)return false;nextMenuAction=time+2;return true;
 }
 public ResourceTaskGoal resourceTask() { return resourceTask; }
 void taskActivity(String text) { activity=text; }
 public CompanionSettings settings() { return settings; }
 public SharedStorage sharedStorage() { return sharedStorage; }
 public Mode mode() { return mode; }
 public Player ownerPlayer() {
  return owner!=null && level() instanceof ServerLevel server?server.getPlayerByUUID(owner):null;
 }
 public boolean retreating() { return retreating || getHealth()<=6; }
 @Override public boolean canAttack(LivingEntity target) {
  return target instanceof net.minecraft.world.entity.monster.Enemy && settings.combat()!=CompanionSettings.Combat.PASSIVE
   && !retreating() && super.canAttack(target);
 }
 @Override public void setTarget(LivingEntity target) {
  if(target!=null && !canAttack(target))return;
  if(target!=null && getTarget()==null)combatAnchor=blockPosition();
  super.setTarget(target);
 }
 public boolean withinCombatBoundary(LivingEntity target) {
  var player=ownerPlayer();
  if(mode==Mode.FOLLOW && player!=null)return player.distanceToSqr(target)<=24*24;
  return combatAnchor==null || target.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(combatAnchor))<=16*16;
 }
 boolean tryEmergencyTeleport(boolean stuck) {
  if(!(level() instanceof ServerLevel server) || mode!=Mode.FOLLOW || !settings.emergencyTeleport()
    || isPassenger() || isLeashed() || !isAlive() || getTarget()!=null || retreating()
    || server.getGameTime()<teleportAfter)return false;
  var player=ownerPlayer();
  if(player==null || player.isSpectator())return false;
  double distance=distanceToSqr(player);
  if(distance<32*32 && !(stuck && distance>12*12))return false;
  teleportAfter=server.getGameTime()+200;
  if(!SafeTravel.teleportNear(this,player))return false;
  activity="Caught up after losing the path";return true;
 }
 public void openInventory(Player player) {
  if(ownedBy(player))player.openMenu(new net.minecraft.world.SimpleMenuProvider(
   (id,playerInventory,p)->net.minecraft.world.inventory.ChestMenu.threeRows(id,playerInventory,inventory),getDisplayName()));
 }
 @Override protected void customServerAiStep(ServerLevel server) {
  if(getHealth()<=6)retreating=true;
  else if(getHealth()>=12)retreating=false;
  super.customServerAiStep(server);inventory.tickSurvival();
  if(getTarget()!=null && (!canAttack(getTarget()) || distanceToSqr(getTarget())>16*16 || !withinCombatBoundary(getTarget())))setTarget(null);
  if(tickCount%20==0) {
   if(mode==Mode.FOLLOW && ownerPlayer()==null)activity="Waiting for owner in this dimension";
   else if(mode==Mode.HOME && home!=null && distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(home))<=4)activity="At home";
   else if(mode==Mode.FOLLOW)tryEmergencyTeleport(false);
  }
 }
 @Override protected void dropCustomDeathLoot(ServerLevel server,net.minecraft.world.damagesource.DamageSource source,boolean recentlyHit) {
  super.dropCustomDeathLoot(server,source,recentlyHit);
  for(int i=0;i<inventory.getContainerSize();i++) {
   var stack=inventory.removeItemNoUpdate(i);if(!stack.isEmpty())spawnAtLocation(server,stack);
  }
 }
 public boolean ownedBy(Player player) { return owner != null && owner.equals(player.getUUID()); }
 public void claim(Player player) { owner=player.getUUID(); setCustomName(Component.literal("Companion")); setCustomNameVisible(true); }
 public void command(Mode next) { if(next!=Mode.GATHER)resourceTask.cancel(); if(next!=Mode.WORK)workTask.cancel(); mode=next; setTarget(null); getNavigation().stop(); activity=next==Mode.STAY?"Waiting":next==Mode.FOLLOW?"Following":next==Mode.GATHER?"Gathering resources":next==Mode.WORK?"Preparing crafting work":"Returning home"; }
 public void setHome(BlockPos pos) { home=pos.immutable(); }
 public BlockPos home() { return home; }
 public String status() { return (retreating()?"Retreating/recovering":getTarget()!=null?"Fighting":activity) + "; food " + inventory.hunger() + "/20" + (home==null?"; no home assigned":"; home "+home.toShortString()); }
 @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
  if (ownedBy(player) && hand==InteractionHand.MAIN_HAND) {
   if (level() instanceof ServerLevel) {
    if(player.isShiftKeyDown()) { command(Mode.STAY);player.sendOverlayMessage(Component.literal(status())); }
    else if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)CompanionMenuProtocol.open(serverPlayer,this,"What would you like to do?");
   }
   return InteractionResult.SUCCESS;
  }
  return super.mobInteract(player,hand);
 }
 @Override protected void addAdditionalSaveData(ValueOutput out) {
  super.addAdditionalSaveData(out);
  writeInventoryToTag(out);
  settings.save(out.child("CompanionSettings"));
  resourceTask.save(out.child("CompanionResourceTask"));
  workTask.save(out.child("CompanionWorkTask"));
  sharedStorage.save(out.child("CompanionStorage"));
  out.putLong("CompanionTeleportAfter",teleportAfter);
  out.putInt("CompanionHunger",inventory.hunger());
  if(owner!=null)out.putString("CompanionOwner",owner.toString());
  out.putString("CompanionMode",mode.name());
  if(home!=null)out.putLong("CompanionHome",home.asLong());
 }
 @Override protected void readAdditionalSaveData(ValueInput in) {
  super.readAdditionalSaveData(in);
  readInventoryFromTag(in);
  settings.load(in.childOrEmpty("CompanionSettings"));
  resourceTask.load(in.childOrEmpty("CompanionResourceTask"));
  workTask.load(in.childOrEmpty("CompanionWorkTask"));
  sharedStorage.load(in.childOrEmpty("CompanionStorage"));
  teleportAfter=in.getLongOr("CompanionTeleportAfter",0);
  inventory.setHunger(in.getIntOr("CompanionHunger",20));
  try { owner=UUID.fromString(in.getStringOr("CompanionOwner","")); } catch(IllegalArgumentException ignored) { owner=null; }
  try { mode=Mode.valueOf(in.getStringOr("CompanionMode","STAY")); } catch(IllegalArgumentException ignored) { mode=Mode.STAY; }
  if(mode==Mode.GATHER && !resourceTask.active())mode=Mode.STAY;
  if(mode==Mode.WORK && !workTask.active())mode=Mode.STAY;
  home=in.getLong("CompanionHome").map(BlockPos::of).orElse(null);
  activity=mode==Mode.STAY?"Waiting":mode==Mode.FOLLOW?"Following":mode==Mode.GATHER?"Gathering resources":mode==Mode.WORK?"Preparing crafting work":"Returning home";
 }
 private final class TravelGoal extends Goal {
  private int repath, failures;
  private net.minecraft.world.phys.Vec3 previous;
  TravelGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
  private net.minecraft.world.phys.Vec3 destination() {
   if(mode==Mode.HOME && home!=null) return net.minecraft.world.phys.Vec3.atCenterOf(home);
   if(mode==Mode.FOLLOW && owner!=null && level() instanceof ServerLevel server) {
    Player player=server.getPlayerByUUID(owner);
    if(player!=null && player.isAlive() && distanceToSqr(player)<128*128) return player.position();
   }
   return null;
  }
  @Override public boolean canUse() { var p=destination(); return p!=null && distanceToSqr(p)> (mode==Mode.FOLLOW?settings.distance().blocks()*settings.distance().blocks():4); }
  @Override public boolean canContinueToUse() { return canUse(); }
  @Override public void start() { repath=0; failures=0; previous=position(); }
  @Override public void stop() { getNavigation().stop(); }
  @Override public void tick() {
   if(--repath>0)return;
   repath=20;
   var p=destination(); if(p==null)return;
   if(previous!=null && position().distanceToSqr(previous)<0.1)failures++; else failures=0;
   previous=position();
   if(failures>=5) { getNavigation().stop();
    if(!tryEmergencyTeleport(true))activity="Path blocked; retrying";
    repath=100; failures=0; return; }
   boolean found=getNavigation().moveTo(p.x,p.y,p.z,1.0);
   activity=found?(mode==Mode.FOLLOW?"Following":mode==Mode.GATHER?"Gathering resources":mode==Mode.WORK?"Preparing crafting work":"Returning home"):"No reachable path; retrying";
  }
 }
}
