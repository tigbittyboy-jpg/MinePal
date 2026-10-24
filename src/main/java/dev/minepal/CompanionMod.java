package dev.minepal;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.brigadier.arguments.StringArgumentType;

public class CompanionMod implements ModInitializer {
 public static final Identifier ID=Identifier.fromNamespaceAndPath("companion","companion");
 public static final EntityType<CompanionEntity> TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,ID,
  EntityType.Builder.of(CompanionEntity::new,MobCategory.CREATURE).sized(0.6F,1.8F).eyeHeight(1.62F)
   .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE,ID)));
 @Override public void onInitialize() {
  CompanionMenuProtocol.register();OwnerAttackHooks.register();
  FabricDefaultAttributeRegistry.register(TYPE,CompanionEntity.attributes());
  CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)-> {
   var root=Commands.literal("companion");
   root.then(Commands.literal("spawn").executes(ctx->{
    var s=ctx.getSource();var player=s.getPlayerOrException();
    if(!s.getLevel().getEntitiesOfClass(CompanionEntity.class,player.getBoundingBox().inflate(128),c->c.ownedBy(player)).isEmpty()) {
     s.sendFailure(Component.literal("You already have a loaded companion nearby."));return 0;
    }
    var c=new CompanionEntity(TYPE,s.getLevel());c.snapTo(player.position());c.claim(player);
    if(!s.getLevel().addFreshEntity(c))return 0;
    s.sendSuccess(()->Component.literal("Companion spawned. Right-click to follow; sneak-right-click to stay."),false);return 1;
   }));
   for(var action:new String[]{"follow","stay","return","home","status","inventory","equip","unequip","craft","settings","share","unshare","stop","recall","gather","mine","menu","prepare","mine_nearby"}) {
    root.then(Commands.literal(action).executes(ctx->control(ctx.getSource(),action,null)));
   }
   root.then(Commands.literal("name").then(Commands.argument("name",StringArgumentType.greedyString())
    .executes(ctx->control(ctx.getSource(),"name",StringArgumentType.getString(ctx,"name")))));
   for(String command:new String[]{"combat","distance","teleport","gifted","shared","gathering","mining"}) {
    var branch=Commands.literal(command);
    String[] values=switch(command) {
     case "combat" -> new String[]{"passive","defensive","aggressive"};
     case "distance" -> new String[]{"close","normal","relaxed"};
     default -> new String[]{"on","off"};
    };
    for(String value:values)branch.then(Commands.literal(value).executes(ctx->control(ctx.getSource(),command,value)));
    root.then(branch);
   }
   for(String action:new String[]{"deposit","withdraw"}) {
    root.then(Commands.literal(action).then(Commands.argument("slot",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,27))
     .executes(ctx->control(ctx.getSource(),action,Integer.toString(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"slot"))))));
   }
   dispatcher.register(root);
  });
 }
 private static int control(CommandSourceStack s,String action,String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
  var player=s.getPlayerOrException();
  var list=new java.util.ArrayList<CompanionEntity>();
  if(action.equals("menu") || action.equals("recall") || action.equals("stop")) {
   for(var entity:s.getLevel().getAllEntities())if(entity instanceof CompanionEntity companion && companion.ownedBy(player))list.add(companion);
  } else list.addAll(s.getLevel().getEntitiesOfClass(CompanionEntity.class,player.getBoundingBox().inflate(64),c->c.ownedBy(player)));
  var c=list.stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
  if(c==null){s.sendFailure(Component.literal("No owned companion loaded within 64 blocks."));return 0;}
  switch(action) {
   case "menu" -> { CompanionMenuProtocol.open(player,c,"What would you like to do?");return 1; }
   case "prepare" -> { String result=c.workTask().prepare(true);s.sendSuccess(()->Component.literal(result),false);return 1; }
   case "mine_nearby" -> { String result=c.resourceTask().assign(ResourceTaskGoal.Objective.ORE,64);s.sendSuccess(()->Component.literal(result),false);return 1; }
   case "gathering" -> c.settings().gathering("on".equals(name));
   case "mining" -> c.settings().mining("on".equals(name));
   case "gather" -> { String result=c.resourceTask().assign(ResourceTaskGoal.Objective.WOOD,64);s.sendSuccess(()->Component.literal(result),false);return 1; }
   case "mine" -> {
    var hit=player.pick(6,0,false);
    if(!(hit instanceof BlockHitResult block) || hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK) {
     s.sendFailure(Component.literal("Look at a stone or ore block within six blocks."));return 0;
    }
    String result=c.resourceTask().begin(block.getBlockPos());s.sendSuccess(()->Component.literal(result),false);return 1;
   }
   case "combat" -> { c.settings().combat(CompanionSettings.Combat.valueOf(name.toUpperCase(java.util.Locale.ROOT)));c.setTarget(null);c.getNavigation().stop(); }
   case "distance" -> c.settings().distance(CompanionSettings.FollowDistance.valueOf(name.toUpperCase(java.util.Locale.ROOT)));
   case "teleport" -> c.settings().emergencyTeleport("on".equals(name));
   case "gifted" -> c.settings().useGiftedEquipment("on".equals(name));
   case "shared" -> c.settings().sharedStorage("on".equals(name));
   case "settings" -> { s.sendSuccess(()->Component.literal(c.settings().summary()),false);return 1; }
   case "share" -> {
    var hit=player.pick(6,0,false);
    if(!(hit instanceof BlockHitResult block) || hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK) {
     s.sendFailure(Component.literal("Look at a chest or barrel within six blocks."));return 0;
    }
    String result=c.sharedStorage().designate(player,block.getBlockPos());s.sendSuccess(()->Component.literal(result),false);return 1;
   }
   case "unshare" -> c.sharedStorage().revoke();
   case "deposit", "withdraw" -> {
    String result=c.sharedStorage().transfer(action.equals("deposit"),Integer.parseInt(name)-1);
    s.sendSuccess(()->Component.literal(result),false);return 1;
   }
   case "unequip" -> {
    if(!c.getInventory().unequipAll()){s.sendFailure(Component.literal("Not enough personal inventory space; nothing moved."));return 0;}
    s.sendSuccess(()->Component.literal("Equipment moved to personal inventory. Use /companion gifted off to prevent automatic re-equipping."),false);return 1;
   }
   case "craft" -> { String result=CompanionCrafting.craft(c);s.sendSuccess(()->Component.literal(result),false);return 1; }
   case "inventory" -> c.openInventory(player);
   case "equip" -> c.getInventory().equipEmptySlots();
   case "follow" -> c.command(CompanionEntity.Mode.FOLLOW);
   case "stay", "stop" -> c.command(CompanionEntity.Mode.STAY);
   case "return", "recall" -> { if(c.home()==null){s.sendFailure(Component.literal("Assign a bed first with /companion home."));return 0;} c.command(CompanionEntity.Mode.HOME); }
   case "home" -> {
    var hit=player.pick(6,0,false);
    if(!(hit instanceof BlockHitResult block) || !(s.getLevel().getBlockState(block.getBlockPos()).getBlock() instanceof BedBlock)) {
     s.sendFailure(Component.literal("Look at a bed within six blocks."));return 0;
    }
    c.setHome(block.getBlockPos());
   }
   case "name" -> { if(name==null || name.isBlank() || name.length()>32){s.sendFailure(Component.literal("Use a name of 1–32 characters."));return 0;}c.setCustomName(Component.literal(name)); }
  }
  s.sendSuccess(()->Component.literal(c.status() + " | " + c.settings().summary()),false);return 1;
 }
}
