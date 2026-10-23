package dev.minepal;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;

public class CompanionStorageTests {
 @GameTest public void private_and_disabled_storage_remain_untouched(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));var owner=h.makeMockServerPlayerInLevel();owner.snapTo(c.position());c.claim(owner);
  h.setBlock(3,2,2,Blocks.CHEST);var pos=h.absolutePos(new BlockPos(3,2,2));var chest=h.getBlockEntity(new BlockPos(3,2,2),ChestBlockEntity.class);
  chest.setItem(0,new ItemStack(Items.DIAMOND,8));
  h.assertFalse(c.sharedStorage().permitted(pos),"Unknown chest must be private");
  c.sharedStorage().transfer(false,0);h.assertTrue(chest.getItem(0).getCount()==8,"Unknown chest must not be touched");
  c.sharedStorage().designate(owner,pos);
  h.assertFalse(c.sharedStorage().permitted(pos),"Designation alone must not enable access");
  c.sharedStorage().transfer(false,0);h.assertTrue(chest.getItem(0).getCount()==8,"Disabled shared access must leave contents unchanged");h.succeed();
 }
 @GameTest public void shared_transfer_and_replacement_are_safe(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));var owner=h.makeMockServerPlayerInLevel();owner.snapTo(c.position());c.claim(owner);
  h.setBlock(3,2,2,Blocks.CHEST);var pos=h.absolutePos(new BlockPos(3,2,2));var chest=h.getBlockEntity(new BlockPos(3,2,2),ChestBlockEntity.class);
  chest.setItem(0,new ItemStack(Items.IRON_INGOT,12));c.sharedStorage().designate(owner,pos);c.settings().sharedStorage(true);
  String result=c.sharedStorage().transfer(false,0);
  h.assertTrue(chest.getItem(0).isEmpty() && c.getInventory().countItem(Items.IRON_INGOT)==12,"Withdraw must move actual resources: "+result);
  c.sharedStorage().transfer(true,0);
  h.assertTrue(c.getInventory().countItem(Items.IRON_INGOT)==0 && chest.countItem(Items.IRON_INGOT)==12,"Deposit must not duplicate");
  h.setBlock(3,2,2,Blocks.AIR);h.setBlock(3,2,2,Blocks.CHEST);
  h.assertFalse(c.sharedStorage().permitted(pos),"Replacement chest must default to private");h.succeed();
 }
 @GameTest public void full_inventory_transfer_is_atomic(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));var owner=h.makeMockServerPlayerInLevel();owner.snapTo(c.position());c.claim(owner);
  h.setBlock(3,2,2,Blocks.BARREL);var pos=h.absolutePos(new BlockPos(3,2,2));var chest=h.getBlockEntity(new BlockPos(3,2,2),BarrelBlockEntity.class);
  chest.setItem(0,new ItemStack(Items.DIAMOND,12));
  for(int i=0;i<27;i++)c.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
  c.getInventory().setItem(0,new ItemStack(Items.DIAMOND,60));c.sharedStorage().designate(owner,pos);c.settings().sharedStorage(true);
  c.sharedStorage().transfer(false,0);
  h.assertTrue(chest.getItem(0).getCount()==12 && c.getInventory().getItem(0).getCount()==60,"Insufficient capacity must not partially move a stack");h.succeed();
 }
 @GameTest public void permission_survives_both_serialization_paths(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));var owner=h.makeMockServerPlayerInLevel();owner.snapTo(c.position());c.claim(owner);
  h.setBlock(3,2,2,Blocks.CHEST);var pos=h.absolutePos(new BlockPos(3,2,2));var chest=h.getBlockEntity(new BlockPos(3,2,2),ChestBlockEntity.class);
  c.sharedStorage().designate(owner,pos);c.settings().sharedStorage(true);
  var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());c.saveWithoutId(out);
  var copy=new CompanionEntity(CompanionMod.TYPE,h.getLevel());copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
  h.assertTrue(copy.sharedStorage().permitted(pos),"Companion capability must persist");
  var saved=chest.saveWithFullMetadata(h.getLevel().registryAccess());
  var restored=BlockEntity.loadStatic(pos,chest.getBlockState(),saved,h.getLevel().registryAccess());
  var before=((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)chest).getAttached(SharedStorage.IDENTITY);
  var after=((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)restored).getAttached(SharedStorage.IDENTITY);
  h.assertTrue(before!=null && before.equals(after),"Exact container identity attachment must survive save/load");h.succeed();
 }
 @GameTest public void gifted_toggle_and_durable_replacement_preserve_items(GameTestHelper h) {
  var c=h.spawn(CompanionMod.TYPE,new BlockPos(2,2,2));
  var old=new ItemStack(Items.IRON_PICKAXE);old.setDamageValue(old.getMaxDamage()-2);
  c.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,old);
  c.getInventory().setItem(0,new ItemStack(Items.IRON_PICKAXE));c.settings().useGiftedEquipment(false);c.getInventory().equipEmptySlots();
  h.assertTrue(c.getMainHandItem()==old,"Disabled gifted use must prevent replacement");
  c.settings().useGiftedEquipment(true);c.getInventory().equipEmptySlots();
  h.assertTrue(c.getMainHandItem().getDamageValue()==0 && c.getInventory().getItem(0)==old,"Replace worn tool without discarding the old one");
  c.settings().useGiftedEquipment(false);h.assertTrue(c.getInventory().unequipAll(),"Must be able to recover equipped items");
  h.assertTrue(c.getMainHandItem().isEmpty() && c.getInventory().countItem(Items.IRON_PICKAXE)==2,"Unequip must preserve both tools");h.succeed();
 }
}
