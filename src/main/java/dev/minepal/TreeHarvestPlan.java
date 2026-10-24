package dev.minepal;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Connected-log inspection with natural-soil/canopy checks; unresolved crowns are never reported complete. */
public final class TreeHarvestPlan {
 private final LinkedHashSet<BlockPos> remaining=new LinkedHashSet<>();
 private boolean incomplete,oversized;
 private String family="";
 private int inspected;
 private final LinkedHashSet<BlockPos> frontier=new LinkedHashSet<>();
 private static String family(net.minecraft.world.level.block.state.BlockState state){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().replace("stripped_","").replace("_log","").replace("_wood","");}
 public void clear(){remaining.clear();frontier.clear();incomplete=oversized=false;inspected=0;family="";}
 public boolean detect(ServerLevel server,BlockPos seed) {
  remaining.clear();frontier.clear();incomplete=oversized=false;inspected=0;family=family(server.getBlockState(seed));
  var queue=new ArrayDeque<BlockPos>();queue.add(seed);var visited=new HashSet<BlockPos>();boolean canopy=false;
  int minimumY=Integer.MAX_VALUE;
  while(!queue.isEmpty()) {
   var p=queue.removeFirst();if(!visited.add(p))continue;
   if(!server.hasChunkAt(p)){incomplete=true;frontier.add(p);continue;}
   if(!server.getBlockState(p).is(BlockTags.LOGS) || !family(server.getBlockState(p)).equals(family))continue;
   remaining.add(p.immutable());inspected++;minimumY=Math.min(minimumY,p.getY());
   if(remaining.size()>384){remaining.clear();incomplete=true;return false;}
   for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
    if(x==0&&y==0&&z==0)continue;var q=p.offset(x,y,z);
    if(!server.hasChunkAt(q)){incomplete=true;if(frontier.size()<2048)frontier.add(q);else oversized=true;continue;}
    var state=server.getBlockState(q);
    if(state.is(BlockTags.LOGS) && family(state).equals(family))queue.add(q);
    if(state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT))canopy=true;
   }
  }
  int baseLogs=0,soil=0;
  for(var p:remaining)if(p.getY()==minimumY) {
   baseLogs++;var below=p.below();if(server.hasChunkAt(below) && server.getBlockState(below).is(BlockTags.DIRT))soil++;
  }
  if(!canopy || soil==0 || baseLogs>4){remaining.clear();return false;}
  return !remaining.isEmpty();
 }
 public void reconcile(ServerLevel server) {
  remaining.removeIf(p->server.hasChunkAt(p) && !server.getBlockState(p).is(BlockTags.LOGS));
  var queue=new ArrayDeque<BlockPos>();for(var p:new ArrayList<>(frontier))if(server.hasChunkAt(p)){frontier.remove(p);queue.add(p);}
  var seen=new HashSet<BlockPos>();
  while(!queue.isEmpty()) {
   var p=queue.removeFirst();if(!seen.add(p) || remaining.contains(p))continue;
   if(!server.hasChunkAt(p)){if(frontier.size()<2048)frontier.add(p);else oversized=true;continue;}
   var state=server.getBlockState(p);if(!state.is(BlockTags.LOGS) || !family(state).equals(family))continue;
   if(inspected>=384){oversized=true;break;}inspected++;remaining.add(p);
   for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(x!=0||y!=0||z!=0)queue.add(p.offset(x,y,z));
  }
  incomplete=!frontier.isEmpty() || oversized;
 }
 public boolean contains(BlockPos p){return remaining.contains(p);}
 public boolean active(){return !remaining.isEmpty();}
 public int remaining(){return remaining.size();}
 public boolean incomplete(){return incomplete;}
 public List<BlockPos> nearest(CompanionEntity c) { return remaining.stream().sorted(Comparator.comparingDouble(p->c.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p)))).toList(); }
 public void removed(BlockPos p){remaining.remove(p);}
 public void save(ValueOutput out){out.store("Logs",Codec.LONG.listOf(),remaining.stream().map(BlockPos::asLong).toList());out.putBoolean("Incomplete",incomplete);out.putBoolean("Oversized",oversized);out.putString("Family",family);out.putInt("Inspected",inspected);out.store("Frontier",Codec.LONG.listOf(),frontier.stream().map(BlockPos::asLong).toList());}
 public void load(ValueInput in){remaining.clear();frontier.clear();family=in.getStringOr("Family","");inspected=Math.clamp(in.getIntOr("Inspected",0),0,384);oversized=in.getBooleanOr("Oversized",false);for(long p:in.read("Frontier",Codec.LONG.listOf()).orElse(List.of()).stream().limit(2048).toList())frontier.add(BlockPos.of(p));incomplete=in.getBooleanOr("Incomplete",false);for(long p:in.read("Logs",Codec.LONG.listOf()).orElse(List.of()).stream().limit(384).toList())remaining.add(BlockPos.of(p));}
}
