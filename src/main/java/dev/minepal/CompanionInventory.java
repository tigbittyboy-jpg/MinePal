package dev.minepal;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;

/** Personal storage only. Never searches or opens world containers. */
public final class CompanionInventory extends SimpleContainer {
 private final CompanionEntity companion;
 private int hunger=20;
 private int clock;
 public CompanionInventory(CompanionEntity companion) { super(27);this.companion=companion; }
 @Override public boolean stillValid(Player player) {
  return companion.isAlive() && companion.ownedBy(player) && companion.distanceToSqr(player)<=64;
 }
 public int hunger() { return hunger; }
 public void setHunger(int value) { hunger=Math.clamp(value,0,20); }
 public void tickSurvival() {
  clock++;
  if(clock%600==0)hunger=Math.max(0,hunger-1);
  if(clock%40==0 && hunger<=14) {
   for(int i=0;i<getContainerSize();i++) {
    var stack=getItem(i);var food=stack.get(DataComponents.FOOD);
    if(food==null || food.nutrition()<=0 || !stack.has(DataComponents.CONSUMABLE))continue;
    // Vanilla item consumption applies effects and returns bowl/bottle remainders.
    int nutrition=food.nutrition();
    ItemStack remainder=stack.finishUsingItem(companion.level(),companion);
    setItem(i,remainder);hunger=Math.min(20,hunger+nutrition);break;
   }
  }
  if(clock%80==0 && hunger>=18 && companion.getHealth()<companion.getMaxHealth()) {
   companion.heal(1);hunger--;
  }
  if(clock%200==0)equipEmptySlots();
 }
 public boolean unequipAll() {
  var planned=new SimpleContainer(getContainerSize());
  for(int i=0;i<getContainerSize();i++)planned.setItem(i,getItem(i).copy());
  for(var slot:EquipmentSlot.values()) {
   var item=companion.getItemBySlot(slot);
   if(!item.isEmpty() && !planned.addItem(item.copy()).isEmpty())return false;
  }
  for(var slot:EquipmentSlot.values())companion.setItemSlot(slot,ItemStack.EMPTY);
  for(int i=0;i<getContainerSize();i++)setItem(i,planned.getItem(i));
  return true;
 }
 public void equipEmptySlots() {
  if(!companion.settings().useGiftedEquipment())return;
  for(int i=0;i<getContainerSize();i++) {
   var stack=getItem(i);if(stack.isEmpty())continue;
   var equippable=stack.get(DataComponents.EQUIPPABLE);
   EquipmentSlot slot=equippable==null?null:equippable.slot();
   if(slot==null && (stack.is(ItemTags.SWORDS)||stack.is(ItemTags.AXES)||stack.is(ItemTags.PICKAXES)))slot=EquipmentSlot.MAINHAND;
   if(slot==null || !companion.isEquippableInSlot(stack,slot))continue;
   var equipped=companion.getItemBySlot(slot);
   if(!equipped.isEmpty()) {
    // Upgrade common tools, or replace a near-broken item with a healthier copy.
    // Preserve the old equipment in the freed storage slot, including its enchantments.
    boolean upgrade=BasicCrafting.toolCategory(stack)>=0 && BasicCrafting.toolCategory(stack)==BasicCrafting.toolCategory(equipped)
     && BasicCrafting.toolRank(stack)>BasicCrafting.toolRank(equipped)
     && stack.getMaxDamage()-stack.getDamageValue()>Math.max(1,stack.getMaxDamage()/10);
    boolean repair=equipped.isDamageableItem() && ItemStack.isSameItem(stack,equipped)
     && equipped.getMaxDamage()-equipped.getDamageValue()<=Math.max(1,equipped.getMaxDamage()/10)
     && stack.getDamageValue()<equipped.getDamageValue();
    if(stack.getCount()!=1 || (!upgrade && !repair))continue;
    setItem(i,equipped);companion.setItemSlot(slot,stack);companion.setGuaranteedDrop(slot);
   } else {
    if(stack.isDamageableItem() && stack.nextDamageWillBreak())continue;
    companion.setItemSlot(slot,removeItem(i,1));companion.setGuaranteedDrop(slot);
   }
  }
 }
}
