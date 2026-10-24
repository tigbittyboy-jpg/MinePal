package dev.minepal;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Short inspectable staircase segments. Never removes the block directly supporting the actor. */
public final class ExcavationPlanner {
 private final ArrayDeque<BlockPos> cuts=new ArrayDeque<>();
 private BlockPos landing;
 private Direction direction;
 public boolean active(){return landing!=null;}
 public boolean cutting(){return !cuts.isEmpty();}
 public BlockPos next(){return cuts.peekFirst();}
 public boolean contains(BlockPos p){return cuts.contains(p);}
 public void removed(BlockPos p){cuts.remove(p);}
 public BlockPos landing(){return landing;}
 public void arrived(){cuts.clear();landing=null;}
 public boolean plan(CompanionEntity c) {
  var server=(ServerLevel)c.level();
  var choices=new ArrayList<Direction>();if(direction!=null)choices.add(direction);
  for(var d:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST})if(!choices.contains(d))choices.add(d);
  for(var d:choices) {
   BlockPos front=c.blockPosition().relative(d),floor=front.below(),foundation=floor.below();
   if(!server.hasChunkAt(foundation) || !server.getWorldBorder().isWithinBounds(front)
    || !server.getBlockState(foundation).isFaceSturdy(server,foundation,Direction.UP))continue;
   var planned=new ArrayDeque<BlockPos>();boolean safe=true;
   for(var pos:List.of(front.above(),front,floor)) {
    if(!safeCut(c,server,pos)){safe=false;break;}
    if(!server.getBlockState(pos).isAir())planned.add(pos);
   }
   if(!safe || planned.isEmpty())continue;
   cuts.clear();cuts.addAll(planned);landing=floor;direction=d;return true;
  }
  return false;
 }
 public static boolean safeCut(CompanionEntity c,ServerLevel server,BlockPos p) {
  if(!server.hasChunkAt(p) || p.equals(c.blockPosition().below()) || server.getBlockEntity(p)!=null)return false;
  var state=server.getBlockState(p);
  if(!state.isAir() && !(state.is(BlockTags.DIRT)||state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE)))return false;
  if(!c.getInventory().hasToolFor(state))return false;
  for(var d:Direction.values()) {
   var q=p.relative(d);if(!server.hasChunkAt(q) || !server.getFluidState(q).isEmpty())return false;
   if(d==Direction.UP && server.getBlockState(q).getBlock() instanceof FallingBlock)return false;
  }
  return state.isAir() || state.getDestroySpeed(server,p)>0;
 }
 public void save(ValueOutput out){out.store("Cuts",Codec.LONG.listOf(),cuts.stream().map(BlockPos::asLong).toList());if(landing!=null)out.putLong("Landing",landing.asLong());if(direction!=null)out.putString("Direction",direction.name());}
 public void load(ValueInput in){cuts.clear();for(long p:in.read("Cuts",Codec.LONG.listOf()).orElse(List.of()).stream().limit(3).toList())cuts.add(BlockPos.of(p));landing=in.getLong("Landing").map(BlockPos::of).orElse(null);try{direction=Direction.valueOf(in.getStringOr("Direction","NORTH"));}catch(IllegalArgumentException ignored){direction=Direction.NORTH;}}
}
