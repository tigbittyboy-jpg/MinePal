package dev.minepal;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Incremental flood of available chunks, capped work per call, followed by ordinary navigation. */
public final class ExplorationPlanner {
 private final ArrayDeque<BlockPos> frontier=new ArrayDeque<>();
 private final Set<Long> queued=new HashSet<>();
 private BlockPos cell,waypoint;
 private int cursor,levelY;
 private long nextMove,nextRevisit;
 String diagnostics(){return "cell="+cell+" frontier="+frontier.size()+" cursor="+cursor+" y="+levelY;}
 public void reset(BlockPos start){frontier.clear();queued.clear();cell=null;waypoint=null;cursor=0;enqueue(start);}
 private static BlockPos cellOf(BlockPos p){return new BlockPos(Math.floorDiv(p.getX(),16)*16,p.getY(),Math.floorDiv(p.getZ(),16)*16);}
 private void enqueue(BlockPos pos){var p=cellOf(pos);long key=new BlockPos(p.getX(),0,p.getZ()).asLong();if(frontier.size()<2048 && queued.add(key))frontier.add(p);}
 public BlockPos scan(CompanionEntity c,String purpose,java.util.function.Predicate<BlockPos> predicate) {
  var server=(ServerLevel)c.level();String scope=server.dimension().identifier()+"#"+purpose;
  if(cell==null) {
   int tries=Math.min(32,frontier.size());
   while(tries-->0) {
    var next=frontier.removeFirst();
    if(!server.hasChunkAt(next)){frontier.addLast(next);continue;}
    if(c.spatialMemory().recentlySearched(scope,next,server.getGameTime()))continue;
    cell=next;levelY=c.blockPosition().getY();cursor=0;break;
   }
   if(cell==null){
    if(server.getGameTime()>=nextRevisit){
     nextRevisit=server.getGameTime()+200;var here=cellOf(c.blockPosition());queued.remove(new BlockPos(here.getX(),0,here.getZ()).asLong());enqueue(here);
    }
    return null;
   }
  }
  for(int budget=0;budget<768 && cursor<16*16*20;budget++,cursor++) {
   int i=cursor;var pos=new BlockPos(cell.getX()+i%16,levelY-4+(i/256),cell.getZ()+(i/16)%16);
   if(server.hasChunkAt(pos) && predicate.test(pos)){cursor++;return pos;}
  }
  if(cursor>=5120) {
   c.spatialMemory().searched(scope,cell,server.getGameTime());
   for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL)enqueue(cell.relative(d,16));cell=null;
  }
  return null;
 }
 public boolean scanning(){return cell!=null;}
 public boolean hasLoadedFrontier(CompanionEntity c){return frontier.stream().anyMatch(c.level()::hasChunkAt);}
 public boolean travel(CompanionEntity c) {
  var server=(ServerLevel)c.level();if(server.getGameTime()<nextMove)return waypoint!=null;
  nextMove=server.getGameTime()+20;
  BlockPos destination=cell;
  if(destination==null)destination=frontier.stream().filter(server::hasChunkAt)
   .min(Comparator.comparingDouble(p->c.distanceToSqr(Vec3.atCenterOf(p)))).orElse(null);
  if(destination==null){waypoint=null;return false;}
  double dx=destination.getX()+8-c.getX(),dz=destination.getZ()+8-c.getZ(),length=Math.sqrt(dx*dx+dz*dz);
  if(length<3)return false;
  for(int distance:new int[]{10,6,3})for(int y:new int[]{0,1,-1,2,-2}) {
   var p=c.blockPosition().offset((int)Math.round(dx/length*distance),y,(int)Math.round(dz/length*distance));
   if(!SafeTravel.safeLanding(c,server,p))continue;var path=c.getNavigation().createPath(p,0);
   if(path!=null && path.canReach()){waypoint=p;c.getNavigation().moveTo(path,1);return true;}
  }
  waypoint=null;return false;
 }
 public void save(ValueOutput out){out.store("Frontier",Codec.LONG.listOf(),frontier.stream().map(BlockPos::asLong).toList());if(cell!=null)out.putLong("Cell",cell.asLong());out.putInt("Cursor",cursor);out.putInt("LevelY",levelY);}
 public void load(ValueInput in){frontier.clear();queued.clear();for(long p:in.read("Frontier",Codec.LONG.listOf()).orElse(List.of()).stream().limit(2048).toList())enqueue(BlockPos.of(p));cell=in.getLong("Cell").map(BlockPos::of).orElse(null);cursor=Math.clamp(in.getIntOr("Cursor",0),0,5120);levelY=in.getIntOr("LevelY",64);}
}
