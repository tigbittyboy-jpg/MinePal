package dev.minepal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Walk to the safest reachable sampled escape point. Never teleports or grants invulnerability. */
public final class RetreatGoal extends Goal {
 private final CompanionEntity c;
 private long nextPlan;
 public RetreatGoal(CompanionEntity c){this.c=c;setFlags(EnumSet.of(Flag.MOVE));}
 @Override public boolean canUse(){return c.retreating() && !ThreatAssessment.threats(c).isEmpty();}
 @Override public boolean canContinueToUse(){return canUse();}
 @Override public void start(){c.setTarget(null);nextPlan=0;}
 @Override public void stop(){c.getNavigation().stop();}
 @Override public void tick(){
  var server=(ServerLevel)c.level();if(server.getGameTime()<nextPlan)return;nextPlan=server.getGameTime()+20;
  var threats=ThreatAssessment.threats(c);Path bestPath=null;double best=-Double.MAX_VALUE;
  for(int radius:new int[]{4,8})for(int y:new int[]{0,1,-1})for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}){
   BlockPos pos=c.blockPosition().offset(d[0]*radius,y,d[1]*radius);
   if(!SafeTravel.safeLanding(c,server,pos))continue;
   var path=c.getNavigation().createPath(pos,0);if(path==null || !path.canReach())continue;
   double nearest=threats.stream().mapToDouble(e->e.distanceToSqr(Vec3.atBottomCenterOf(pos))).min().orElse(0);
   double score=nearest-radius*0.2-Math.abs(y)*2;
   if(score>best){best=score;bestPath=path;}
  }
  if(bestPath!=null){c.getNavigation().moveTo(bestPath,1.4);c.taskActivity("Retreating along a safe escape route");}
  else {c.getNavigation().stop();c.taskActivity("No safe escape route; need help");}
 }
}
