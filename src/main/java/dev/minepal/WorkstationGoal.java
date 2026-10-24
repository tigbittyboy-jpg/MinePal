package dev.minepal;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Mutually exclusive work job: search, approach a table, then craft using actual resources. */
public final class WorkstationGoal extends Goal {
 private final CompanionEntity c;
 private boolean active,grid,placeTable;
 private long deadline,nextStep,nextTableSearch;
 private BlockPos table;
 private int operations,failures;
 private Vec3 previous;
 private List<net.minecraft.world.item.ItemStack> gridSnapshot=List.of();
 public WorkstationGoal(CompanionEntity c) { this.c=c;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
 public String prepare(boolean allowPlacement) { return begin(false,allowPlacement); }
 public String grid() { return begin(true,false); }
 private String begin(boolean grid,boolean place) {
  if(!(c.level() instanceof ServerLevel server))return "Crafting requires a server.";
  c.command(CompanionEntity.Mode.WORK);active=true;this.grid=grid;placeTable=place;table=null;
  deadline=server.getGameTime()+1200;nextStep=0;nextTableSearch=0;operations=0;failures=0;previous=c.position();
  if(grid) { var snapshot=new ArrayList<net.minecraft.world.item.ItemStack>();for(int i=0;i<9;i++)snapshot.add(c.getInventory().getItem(i).copy());gridSnapshot=snapshot; }
  c.taskActivity("Looking for a crafting table");
  return grid?"Searching for a reachable crafting table within 16 blocks.":"Preparing useful supplies and tools from my own resources.";
 }
 public boolean active() { return active; }
 public void cancel() { active=false;table=null; }
 @Override public boolean canUse() { return active && c.mode()==CompanionEntity.Mode.WORK && c.getTarget()==null && !c.retreating(); }
 @Override public boolean canContinueToUse() { return canUse(); }
 @Override public void stop() { c.getNavigation().stop(); }
 private void finish(String reason) { cancel();c.command(c.home()!=null?CompanionEntity.Mode.HOME:CompanionEntity.Mode.STAY);c.taskActivity(reason); }
 public static BlockPos nearest(CompanionEntity c,int radius,boolean reachable) {
  var found=new ArrayList<BlockPos>();
  for(var pos:BlockPos.betweenClosed(c.blockPosition().offset(-radius,-4,-radius),c.blockPosition().offset(radius,4,radius))) {
   if(c.level().hasChunkAt(pos) && c.level().getBlockState(pos).is(Blocks.CRAFTING_TABLE)
    && c.distanceToSqr(Vec3.atCenterOf(pos))<=radius*radius)found.add(pos.immutable());
  }
  found.sort(Comparator.comparingDouble(p->c.distanceToSqr(Vec3.atCenterOf(p))));
  for(var pos:found.stream().limit(8).toList()) {
   if(c.distanceToSqr(Vec3.atCenterOf(pos))<=9 && visible(c,pos))return pos;
   if(!reachable)continue;
   if(!c.onGround())return pos; // Navigation becomes available after the first physics tick.
   var path=c.getNavigation().createPath(pos,1);if(path!=null && path.canReach())return pos;
  }
  return null;
 }
 public static boolean visible(CompanionEntity c,BlockPos table) {
  var hit=c.level().clip(new net.minecraft.world.level.ClipContext(c.getEyePosition(),Vec3.atCenterOf(table),
   net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,c));
  return hit.getBlockPos().equals(table);
 }
 private boolean placeOwnedTable(ServerLevel server) {
  var owner=c.ownerPlayer();
  if(!placeTable || owner==null || !owner.mayBuild() || !server.getGameRules().get(GameRules.MOB_GRIEFING))return false;
  int slot=-1;for(int i=0;i<c.getInventory().getContainerSize();i++)if(c.getInventory().getItem(i).is(Items.CRAFTING_TABLE)){slot=i;break;}
  if(slot<0)return false;
  for(var offset:new int[][]{{2,0},{-2,0},{0,2},{0,-2}}) {
   var pos=c.blockPosition().offset(offset[0],0,offset[1]);
   if(!server.hasChunkAt(pos) || !server.getWorldBorder().isWithinBounds(pos) || !server.getBlockState(pos).isAir()
    || !SafeTravel.safeLanding(c,server,pos) || server.getServer().isUnderSpawnProtection(server,pos,owner)
    || owner.blockActionRestricted(server,pos,owner.gameMode()))continue;
   // Use vanilla item placement with the owner's permissions and a temporary hand, restored immediately.
   var item=c.getInventory().getItem(slot);var held=owner.getMainHandItem();
   try {
    owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,item.copyWithCount(1));
    var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos.below()),net.minecraft.core.Direction.UP,pos.below(),false);
    var context=new net.minecraft.world.item.context.UseOnContext(owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
    if(net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(owner,server,net.minecraft.world.InteractionHand.MAIN_HAND,hit)!=net.minecraft.world.InteractionResult.PASS)continue;
    owner.getMainHandItem().useOn(context);
    if(server.getBlockState(pos).is(Blocks.CRAFTING_TABLE)) {
     c.getInventory().removeItem(slot,1);table=pos;return true;
    }
   } finally { owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held); }
  }
  return false;
 }
 @Override public void tick() {
  var server=(ServerLevel)c.level();if(server.getGameTime()<nextStep)return;nextStep=server.getGameTime()+10;
  if(server.getGameTime()>deadline || operations>=32) { finish("Crafting job reached its limit");return; }
  if(c.ownerPlayer()==null) { finish("Crafting paused: owner is not present");return; }
  if(table!=null && (!server.hasChunkAt(table) || !server.getBlockState(table).is(Blocks.CRAFTING_TABLE)))table=null;
  if(table==null && server.getGameTime()>=nextTableSearch){table=nearest(c,16,true);nextTableSearch=server.getGameTime()+100;}
  if(table!=null && (c.distanceToSqr(Vec3.atCenterOf(table))>9 || !visible(c,table))) {
   if(previous!=null && c.position().distanceToSqr(previous)<0.1)failures++;else failures=0;
   previous=c.position();if(failures>=12){finish("Cannot reach the crafting table");return;}
   c.getNavigation().moveTo(c.getNavigation().createPath(table,1),1);c.taskActivity("Walking to the nearest crafting table");return;
  }
  c.getNavigation().stop();
  if(grid) {
   if(table==null){finish("No reachable crafting table within 16 blocks");return;}
   if(gridSnapshot.size()!=9){finish("Recipe changed; please choose it again");return;}
   for(int i=0;i<9;i++)if(!net.minecraft.world.item.ItemStack.matches(gridSnapshot.get(i),c.getInventory().getItem(i))) {
    finish("Inventory recipe changed; nothing crafted");return;
   }
   finish(CompanionCrafting.craftGrid(c));return;
  }
  var step=BasicCrafting.step(c,table!=null);operations++;
  switch(step) {
   case CRAFTED -> c.taskActivity("Crafting useful supplies from owned materials");
   case NEED_TABLE -> { if(!placeOwnedTable(server))finish("I made supplies, but need a reachable table. Use Craft basic tools to allow placing my owned bench."); }
   case FINISHED -> { c.getInventory().equipEmptySlots();finish("Basic tools and supplies are ready"); }
   case MISSING_RESOURCES -> finish("Need more wood or tool materials; kept my remaining supplies");
   case FULL -> finish("Crafting stopped: no room or no matching recipe; nothing lost");
  }
 }
 public void save(ValueOutput out) {
  out.putBoolean("Active",active);out.putBoolean("Grid",grid);out.putBoolean("PlaceTable",placeTable);out.putLong("Deadline",deadline);out.putInt("Operations",operations);
  if(table!=null)out.putLong("Table",table.asLong());
  if(grid && !gridSnapshot.isEmpty())out.store("GridSnapshot",net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.listOf(),gridSnapshot);
 }
 public void load(ValueInput in) {
  active=in.getBooleanOr("Active",false);grid=in.getBooleanOr("Grid",false);placeTable=in.getBooleanOr("PlaceTable",false);
  deadline=in.getLongOr("Deadline",0);operations=Math.clamp(in.getIntOr("Operations",0),0,32);table=in.getLong("Table").map(BlockPos::of).orElse(null);
  gridSnapshot=in.read("GridSnapshot",net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()).stream().limit(9).toList();
 }
}
