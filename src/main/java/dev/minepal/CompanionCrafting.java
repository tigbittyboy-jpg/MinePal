package dev.minepal;

import java.util.ArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;

/** One atomic, recipe-manager-backed craft using the first nine personal slots. */
public final class CompanionCrafting {
 private CompanionCrafting() { }
 public static String craft(CompanionEntity companion) {
  if(!(companion.level() instanceof ServerLevel server))return "Crafting requires a server.";
  if(!hasTableNearby(companion,3))return companion.workTask().grid();
  return craftGrid(companion);
 }
 public static boolean hasTableNearby(CompanionEntity c,int radius) {
  return WorkstationGoal.nearest(c,radius,false)!=null;
 }
 public static String craftGrid(CompanionEntity companion) {
  if(!(companion.level() instanceof ServerLevel server) || !hasTableNearby(companion,3))return "I need a reachable crafting table.";
  var inventory=companion.getInventory();var grid=new ArrayList<ItemStack>();
  for(int i=0;i<9;i++)grid.add(inventory.getItem(i).copy());
  var positioned=CraftingInput.ofPositioned(3,3,grid);var input=positioned.input();
  var match=server.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,server);
  if(match.isEmpty())return "The first nine slots do not match a crafting recipe.";
  var recipe=match.get().value();var result=recipe.assemble(input);
  if(result.isEmpty())return "That recipe has no output.";
  var planned=new SimpleContainer(inventory.getContainerSize());
  for(int i=0;i<inventory.getContainerSize();i++)planned.setItem(i,inventory.getItem(i).copy());
  var remainders=recipe.getRemainingItems(input);
  for(int y=0;y<input.height();y++)for(int x=0;x<input.width();x++) {
   int gridSlot=(y+positioned.top())*3+x+positioned.left();
   int recipeSlot=y*input.width()+x;
   if(!input.getItem(recipeSlot).isEmpty())planned.removeItem(gridSlot,1);
  }
  // Simulate capacity before committing any inventory mutation.
  for(var remainder:remainders)if(!remainder.isEmpty() && !planned.addItem(remainder.copy()).isEmpty())return "No room for crafting remainders.";
  if(!planned.addItem(result.copy()).isEmpty())return "No room for the crafted item.";
  for(int i=0;i<inventory.getContainerSize();i++)inventory.setItem(i,planned.getItem(i));
  return "Crafted "+result.getCount()+" "+result.getHoverName().getString()+".";
 }
}
