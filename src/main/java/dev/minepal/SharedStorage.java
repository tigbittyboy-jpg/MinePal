package dev.minepal;

import java.util.UUID;
import java.util.ArrayList;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** An explicit capability for one exact chest/barrel instance, never a proximity grant. */
public final class SharedStorage {
 public static final AttachmentType<String> IDENTITY=AttachmentRegistry.createPersistent(
  Identifier.fromNamespaceAndPath("companion","storage_identity"),Codec.STRING);
 private final CompanionEntity companion;
 private BlockPos position;
 private String dimension="", identity="";
 public SharedStorage(CompanionEntity companion) { this.companion=companion; }
 private RandomizableContainerBlockEntity containerAt(BlockPos pos) {
  if(!companion.level().hasChunkAt(pos))return null;
  var block=companion.level().getBlockEntity(pos);
  if(!(block instanceof ChestBlockEntity) && !(block instanceof BarrelBlockEntity))return null;
  var container=(RandomizableContainerBlockEntity)block;
  // Do not bypass locks or generate unopened loot on the companion's behalf.
  return container.isLocked() || container.getLootTable()!=null?null:container;
 }
 public String designate(Player player,BlockPos pos) {
  if(!companion.ownedBy(player) || player.level()!=companion.level()
   || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>6*6)return "Look at your chest or barrel within six blocks.";
  var block=containerAt(pos);
  if(block==null)return "Only an unlocked, already-opened chest or barrel can be shared.";
  String token=((AttachmentTarget)block).getAttachedOrCreate(IDENTITY,()->UUID.randomUUID().toString());
  block.setChanged();position=pos.immutable();identity=token;dimension=companion.level().dimension().identifier().toString();
  return "Storage designated. Enable shared storage before transferring. A double chest grants only the selected half.";
 }
 public void revoke() { position=null;identity="";dimension=""; }
 public boolean permitted(BlockPos pos) {
  if(!companion.settings().sharedStorage() || position==null || !position.equals(pos)
   || !dimension.equals(companion.level().dimension().identifier().toString()))return false;
  var block=containerAt(pos);
  return block!=null && !identity.isEmpty() && identity.equals(((AttachmentTarget)block).getAttached(IDENTITY));
 }
 public String transfer(boolean deposit,int slot) {
  if(position==null || !permitted(position))return "Shared storage is disabled, missing, locked, replaced, or not designated.";
  if(companion.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(position))>4*4)return "I must be within four blocks of the shared container.";
  var storage=containerAt(position);
  Container source=deposit?companion.getInventory():storage;
  Container target=deposit?storage:companion.getInventory();
  if(slot<0 || slot>=source.getContainerSize())return "Invalid source slot.";
  var original=source.getItem(slot);
  if(original.isEmpty())return "That source slot is empty.";
  if(!source.canTakeItem(target,slot,original))return "That item cannot be transferred.";
  var remainder=original.copy();var planned=new ArrayList<ItemStack>();
  for(int i=0;i<target.getContainerSize();i++)planned.add(target.getItem(i).copy());
  // Prefer matching stacks, then empty slots; all capacity is checked before committing.
  for(int pass=0;pass<2;pass++)for(int i=0;i<planned.size() && !remainder.isEmpty();i++) {
   var stack=planned.get(i);
   if((pass==0 && stack.isEmpty()) || (pass==1 && !stack.isEmpty()))continue;
   if(!target.canPlaceItem(i,remainder) || (!stack.isEmpty() && !ItemStack.isSameItemSameComponents(stack,remainder)))continue;
   int limit=Math.min(target.getMaxStackSize(remainder),remainder.getMaxStackSize());
   int moved=Math.min(limit-stack.getCount(),remainder.getCount());
   if(moved<=0)continue;
   if(stack.isEmpty())planned.set(i,remainder.copyWithCount(moved));else stack.grow(moved);
   remainder.shrink(moved);
  }
  if(!remainder.isEmpty())return "Not enough room; nothing moved.";
  int count=original.getCount();String name=original.getHoverName().getString();
  source.setItem(slot,ItemStack.EMPTY);
  for(int i=0;i<planned.size();i++)target.setItem(i,planned.get(i));
  source.setChanged();target.setChanged();
  return (deposit?"Deposited ":"Withdrew ")+count+" "+name+".";
 }
 public void save(ValueOutput out) {
  if(position==null)return;
  out.putLong("Position",position.asLong());out.putString("Dimension",dimension);out.putString("Identity",identity);
 }
 public void load(ValueInput in) {
  position=in.getLong("Position").map(BlockPos::of).orElse(null);
  dimension=in.getStringOr("Dimension","");identity=in.getStringOr("Identity","");
 }
}
