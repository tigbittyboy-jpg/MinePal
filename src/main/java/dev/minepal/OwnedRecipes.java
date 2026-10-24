package dev.minepal;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.server.level.ServerLevel;

/** Resolve a real recipe, then atomically debit owned ingredients and retain all outputs/remainders. */
public final class OwnedRecipes {
 private OwnedRecipes() { }
 public static boolean craft(CompanionEntity c,int width,int height,List<ItemStack> grid,boolean workbench) {
  if(!(c.level() instanceof ServerLevel server) || (workbench && !CompanionCrafting.hasTableNearby(c,3)))return false;
  var input=CraftingInput.of(width,height,grid);
  var match=server.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,server);
  if(match.isEmpty())return false;
  var recipe=match.get().value();var output=recipe.assemble(input);
  if(output.isEmpty())return false;
  var inv=c.getInventory();var plan=new SimpleContainer(inv.getContainerSize());
  for(int i=0;i<inv.getContainerSize();i++)plan.setItem(i,inv.getItem(i).copy());
  for(var ingredient:grid) {
   if(ingredient.isEmpty())continue;boolean found=false;
   for(int i=0;i<plan.getContainerSize();i++) {
    if(ItemStack.isSameItemSameComponents(plan.getItem(i),ingredient) && plan.getItem(i).getCount()>0) {
     plan.removeItem(i,1);found=true;break;
    }
   }
   if(!found)return false;
  }
  for(var remainder:recipe.getRemainingItems(input))if(!remainder.isEmpty() && !plan.addItem(remainder.copy()).isEmpty())return false;
  if(!plan.addItem(output.copy()).isEmpty())return false;
  for(int i=0;i<inv.getContainerSize();i++)inv.setItem(i,plan.getItem(i));
  return true;
 }
}
