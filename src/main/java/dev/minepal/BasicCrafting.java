package dev.minepal;

import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.tags.ItemTags;

/** A small resource-driven planner. Every step is validated by the world's recipe manager. */
public final class BasicCrafting {
 private BasicCrafting() { }
 public enum Step { CRAFTED, NEED_TABLE, FINISHED, MISSING_RESOURCES, FULL }
 public static int count(CompanionEntity c,Item item) { return c.getInventory().countItem(item); }
 private static List<ItemStack> supplies(CompanionEntity c,java.util.function.Predicate<ItemStack> filter,int amount) {
  var result=new ArrayList<ItemStack>();
  for(var stack:c.getInventory())if(filter.test(stack))for(int n=0;n<stack.getCount() && result.size()<amount;n++)result.add(stack.copyWithCount(1));
  return result;
 }
 private static boolean owns(CompanionEntity c,Item item) {
  if(count(c,item)>0)return true;
  for(var slot:net.minecraft.world.entity.EquipmentSlot.values())if(c.getItemBySlot(slot).is(item))return true;
  return false;
 }
 private static final Item[][] TOOLS={
  {Items.WOODEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.NETHERITE_PICKAXE},
  {Items.WOODEN_AXE,Items.STONE_AXE,Items.IRON_AXE,Items.DIAMOND_AXE,Items.NETHERITE_AXE},
  {Items.WOODEN_SHOVEL,Items.STONE_SHOVEL,Items.IRON_SHOVEL,Items.DIAMOND_SHOVEL,Items.NETHERITE_SHOVEL}
 };
 private static final Item[] MATERIALS={null,Items.COBBLESTONE,Items.IRON_INGOT,Items.DIAMOND};
 public static int toolCategory(ItemStack stack) {
  for(int type=0;type<TOOLS.length;type++)for(var item:TOOLS[type])if(stack.is(item))return type;return -1;
 }
 public static int toolRank(ItemStack stack) {
  for(var row:TOOLS)for(int tier=0;tier<row.length;tier++)if(stack.is(row[tier]))return tier;return -1;
 }
 private static ItemStack e() { return ItemStack.EMPTY; }
 private static Step planks(CompanionEntity c) {
  var logs=supplies(c,s->s.is(ItemTags.LOGS),1);
  if(logs.isEmpty())return Step.MISSING_RESOURCES;
  // Only accept a vanilla/datapack recipe that actually produces planks.
  var server=(net.minecraft.server.level.ServerLevel)c.level();var input=net.minecraft.world.item.crafting.CraftingInput.of(1,1,logs);
  var recipe=server.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,server);
  if(recipe.isEmpty() || !recipe.get().value().assemble(input).is(ItemTags.PLANKS))return Step.MISSING_RESOURCES;
  return OwnedRecipes.craft(c,1,1,logs,false)?Step.CRAFTED:Step.FULL;
 }
 private static Step ensurePlanks(CompanionEntity c,int count) {
  return supplies(c,s->s.is(ItemTags.PLANKS),count).size()>=count?Step.FINISHED:planks(c);
 }
 public static Step step(CompanionEntity c,boolean tableAvailable) {
  if(!tableAvailable && !owns(c,Items.CRAFTING_TABLE)) {
   var enough=ensurePlanks(c,4);if(enough!=Step.FINISHED)return enough;
   return OwnedRecipes.craft(c,2,2,supplies(c,s->s.is(ItemTags.PLANKS),4),false)?Step.CRAFTED:Step.FULL;
  }
  int selected=-1,tier=-1,required=0;
  for(int tool=0;tool<TOOLS.length;tool++) {
   int current=-1;for(int t=0;t<TOOLS[tool].length;t++)if(owns(c,TOOLS[tool][t]))current=t;
   int wanted=current<0?0:current;
   int amount=tool==2?1:3;
   for(int t=1;t<4;t++)if(count(c,MATERIALS[t])>=amount && t>wanted)wanted=t;
   if(current<wanted || current<0) { selected=tool;tier=wanted;required=amount;break; }
  }
  if(selected<0)return Step.FINISHED;
  if(count(c,Items.STICK)<2) {
   var enough=ensurePlanks(c,2);if(enough!=Step.FINISHED)return enough;
   return OwnedRecipes.craft(c,1,2,supplies(c,s->s.is(ItemTags.PLANKS),2),false)?Step.CRAFTED:Step.FULL;
  }
  List<ItemStack> materials;
  if(tier==0) {
   var enough=ensurePlanks(c,required);if(enough!=Step.FINISHED)return enough;
   materials=supplies(c,s->s.is(ItemTags.PLANKS),required);
  } else { Item item=MATERIALS[tier];materials=supplies(c,s->s.is(item),required); }
  if(materials.size()<required)return Step.MISSING_RESOURCES;
  if(!tableAvailable)return Step.NEED_TABLE;
  var sticks=supplies(c,s->s.is(Items.STICK),2);List<ItemStack> grid;
  if(selected==0)grid=List.of(materials.get(0),materials.get(1),materials.get(2),e(),sticks.get(0),e(),e(),sticks.get(1),e());
  else if(selected==1)grid=List.of(materials.get(0),materials.get(1),e(),materials.get(2),sticks.get(0),e(),e(),sticks.get(1),e());
  else grid=List.of(materials.get(0),e(),e(),sticks.get(0),e(),e(),sticks.get(1),e(),e());
  return OwnedRecipes.craft(c,3,3,grid,true)?Step.CRAFTED:Step.FULL;
 }
}
