package dev.minepal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.Comparator;

/** Target arbitration, separate from vanilla attack movement/cooldowns. No PvP targets. */
public final class CompanionCombatGoal extends Goal {
 private final CompanionEntity companion;
 private LivingEntity selected;
 private long nextScan;
 private int selfAttackSeen=-1, ownerAttackSeen=-1;
 public CompanionCombatGoal(CompanionEntity companion) {
  this.companion=companion;setFlags(EnumSet.of(Flag.TARGET));
 }
 private boolean eligible(LivingEntity target) {
  return target!=null && target.isAlive() && companion.canAttack(target)
   && companion.distanceToSqr(target)<=16*16 && companion.withinCombatBoundary(target)
   && companion.getSensing().hasLineOfSight(target);
 }
 @Override public boolean canUse() {
  selected=null;
  if(companion.retreating() || companion.settings().combat()==CompanionSettings.Combat.PASSIVE)return false;
  var attacker=companion.getLastHurtByMob();int stamp=companion.getLastHurtByMobTimestamp();
  if(stamp!=selfAttackSeen && companion.tickCount-stamp<=200 && eligible(attacker)) {
   selfAttackSeen=stamp;selected=attacker;return true;
  }
  var owner=companion.ownerPlayer();
  if(owner!=null && companion.distanceToSqr(owner)<24*24) {
   stamp=owner.getLastHurtByMobTimestamp();attacker=owner.getLastHurtByMob();
   if(stamp!=ownerAttackSeen && owner.tickCount-stamp<=200 && eligible(attacker)) {
    ownerAttackSeen=stamp;selected=attacker;return true;
   }
  }
  if(companion.settings().combat()==CompanionSettings.Combat.AGGRESSIVE && companion.level().getGameTime()>=nextScan) {
   nextScan=companion.level().getGameTime()+10;
   selected=companion.level().getEntitiesOfClass(LivingEntity.class,companion.getBoundingBox().inflate(12),
    target->target instanceof Enemy && eligible(target)).stream()
    .min(Comparator.comparingDouble(companion::distanceToSqr)).orElse(null);
  }
  return selected!=null;
 }
 @Override public boolean canContinueToUse() {
  return !companion.retreating() && companion.settings().combat()!=CompanionSettings.Combat.PASSIVE
   && selected==companion.getTarget() && eligible(selected);
 }
 @Override public void start() { companion.setTarget(selected); }
 @Override public void stop() { companion.setTarget(null);selected=null; }
}
