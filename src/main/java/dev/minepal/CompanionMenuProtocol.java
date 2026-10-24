package dev.minepal;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

/** Small, validated server-authoritative menu messages, also used in dedicated multiplayer. */
public final class CompanionMenuProtocol {
 private CompanionMenuProtocol() { }
 public record Action(int entityId,String action,String value) implements CustomPacketPayload {
  public static final Type<Action> TYPE=new Type<>(Identifier.fromNamespaceAndPath("companion","menu_action"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of(
   (b,p)->{b.writeVarInt(p.entityId);b.writeUtf(p.action,40);b.writeUtf(p.value,64);},
   b->new Action(b.readVarInt(),b.readUtf(40),b.readUtf(64)));
  @Override public Type<Action> type() { return TYPE; }
 }
 public record State(int entityId,String name,String status,String response,int flags,int combat,int distance,boolean initial) implements CustomPacketPayload {
  public static final Type<State> TYPE=new Type<>(Identifier.fromNamespaceAndPath("companion","menu_state"));
  public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of(
   (b,p)->{b.writeVarInt(p.entityId);b.writeUtf(p.name,128);b.writeUtf(p.status,512);b.writeUtf(p.response,512);b.writeVarInt(p.flags);b.writeVarInt(p.combat);b.writeVarInt(p.distance);b.writeBoolean(p.initial);},
   b->new State(b.readVarInt(),b.readUtf(128),b.readUtf(512),b.readUtf(512),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean()));
  @Override public Type<State> type() { return TYPE; }
 }
 public static void register() {
  PayloadTypeRegistry.serverboundPlay().register(Action.TYPE,Action.CODEC);
  PayloadTypeRegistry.clientboundPlay().register(State.TYPE,State.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(Action.TYPE,(packet,context)->context.server().execute(()->handle(context.player(),packet)));
 }
 public static boolean permitted(ServerPlayer player,CompanionEntity c) {
  return c.isAlive() && c.ownedBy(player) && c.level()==player.level() && player.distanceToSqr(c)<=16*16;
 }
 public static void open(ServerPlayer player,CompanionEntity c,String response) { sendState(player,c,response,true); }
 private static void sendState(ServerPlayer player,CompanionEntity c,String response,boolean initial) {
  if(!permitted(player,c) || !ServerPlayNetworking.canSend(player,State.TYPE))return;
  var s=c.settings();int flags=(s.emergencyTeleport()?1:0)|(s.useGiftedEquipment()?2:0)|(s.sharedStorage()?4:0)
   |(s.gathering()?8:0)|(s.mining()?16:0)|(s.basicCrafting()?32:0)|(s.clearLeaves()?64:0);
  ServerPlayNetworking.send(player,new State(c.getId(),c.getName().getString(),c.status(),response,flags,s.combat().ordinal(),s.distance().ordinal(),initial));
 }
 public static String apply(ServerPlayer player,CompanionEntity c,String action,String value) {
  if(!permitted(player,c))return "This companion is not yours or is too far away.";
  switch(action) {
   case "refresh": return "";
   case "follow": c.command(CompanionEntity.Mode.FOLLOW);return "Coming with you.";
   case "stay", "stop": c.command(CompanionEntity.Mode.STAY);return "I'll wait here.";
   case "home":
    if(c.home()==null)return "Assign a bed first.";
    c.command(CompanionEntity.Mode.HOME);return "Heading home.";
   case "inventory": c.openInventory(player);return "";
   case "gather": return c.resourceTask().begin(null);
   case "mine": return c.resourceTask().beginNearestMine();
   case "craft_grid": return CompanionCrafting.craft(c);
   case "prepare": return c.workTask().prepare(true);
   case "equip": c.getInventory().equipEmptySlots();return "Checked my equipment.";
   case "unequip":
    if(!c.getInventory().unequipAll())return "Not enough room; nothing moved.";
    c.settings().useGiftedEquipment(false);return "Equipment stored; automatic re-equipping disabled.";
   case "name":
    if(value.isBlank() || value.length()>32)return "Use a name of 1–32 characters.";
    c.setCustomName(Component.literal(value));return "Name saved.";
   case "set_home":
    BlockPos nearest=null;double best=Double.MAX_VALUE;
    for(var p:BlockPos.betweenClosed(c.blockPosition().offset(-8,-3,-8),c.blockPosition().offset(8,3,8))) {
     if(!player.level().hasChunkAt(p) || !(player.level().getBlockState(p).getBlock() instanceof BedBlock))continue;
     double d=c.distanceToSqr(Vec3.atCenterOf(p));if(d<=64 && d<best){best=d;nearest=p.immutable();}
    }
    if(nearest==null)return "No loaded bed found within eight blocks.";
    c.setHome(nearest);return "Assigned the nearest bed as home.";
   case "combat": c.settings().combat(CompanionSettings.Combat.values()[(c.settings().combat().ordinal()+1)%3]);c.setTarget(null);break;
   case "distance": c.settings().distance(CompanionSettings.FollowDistance.values()[(c.settings().distance().ordinal()+1)%3]);break;
   case "teleport": c.settings().emergencyTeleport(!c.settings().emergencyTeleport());break;
   case "gifted": c.settings().useGiftedEquipment(!c.settings().useGiftedEquipment());break;
   case "shared": c.settings().sharedStorage(!c.settings().sharedStorage());break;
   case "gathering": c.settings().gathering(!c.settings().gathering());break;
   case "mining": c.settings().mining(!c.settings().mining());break;
   case "basic_crafting": c.settings().basicCrafting(!c.settings().basicCrafting());break;
   case "leaves": c.settings().clearLeaves(!c.settings().clearLeaves());break;
   case "share_nearest":
    BlockPos storage=null;double storageDistance=Double.MAX_VALUE;
    for(var p:BlockPos.betweenClosed(c.blockPosition().offset(-6,-2,-6),c.blockPosition().offset(6,2,6))) {
     if(!player.level().hasChunkAt(p))continue;var be=player.level().getBlockEntity(p);
     if(!(be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity) && !(be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity))continue;
     double d=player.distanceToSqr(Vec3.atCenterOf(p));if(d<storageDistance){storageDistance=d;storage=p.immutable();}
    }
    return storage==null?"No nearby chest/barrel found.":c.sharedStorage().designate(player,storage);
   case "deposit", "withdraw":
    try { int slot=Integer.parseInt(value);if(slot<1 || slot>27)return "Choose a slot from 1 to 27.";
     return c.sharedStorage().transfer(action.equals("deposit"),slot-1);
    } catch(NumberFormatException invalid) { return "Choose a slot from 1 to 27."; }
   case "unshare": c.sharedStorage().revoke();return "Shared storage permission revoked.";
   default: return "Unknown menu action.";
  }
  return "Setting saved.";
 }
 private static void handle(ServerPlayer player,Action packet) {
  if(!(player.level().getEntity(packet.entityId) instanceof CompanionEntity c) || !permitted(player,c))return;
  if(!c.allowMenuAction(packet.action.equals("refresh")))return;
  String response=apply(player,c,packet.action,packet.value);
  // Inventory uses vanilla container synchronization; don't replace it with another screen.
  if(!packet.action.equals("inventory"))sendState(player,c,response,false);
 }
}
