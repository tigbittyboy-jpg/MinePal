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
   && companion.distanceToSqr(target)<=companion.combatLeashSquared(target) && companion.withinCombatBoundary(target)
   && companion.getSensing().hasLineOfSight(target);
 }
 @Override public boolean canUse() {
  selected=null;
  if(companion.retreating())return false;
  var attacker=companion.getLastHurtByMob();int stamp=companion.getLastHurtByMobTimestamp();
  if(stamp!=selfAttackSeen && companion.tickCount-stamp<=200 && eligible(attacker)) {
   selfAttackSeen=stamp;selected=attacker;return true;
  }
  var owner=companion.ownerPlayer();
  if(owner!=null && companion.distanceToSqr(owner)<32*32) {
   stamp=owner.getLastHurtByMobTimestamp();attacker=owner.getLastHurtByMob();
   if(stamp!=ownerAttackSeen && owner.tickCount-stamp<=200 && eligible(attacker)) {
    ownerAttackSeen=stamp;selected=attacker;return true;
   }
   var chosen=companion.ownerAttackTarget()!=null?companion.ownerAttackTarget():owner.getLastHurtMob();
   if(companion.settings().combat()==CompanionSettings.Combat.AGGRESSIVE && eligible(chosen) && CombatSafety.ownerSelected(companion,chosen)) {
    selected=chosen;return true;
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
  var urgent=companion.getLastHurtByMob();
  if(urgent!=selected && CombatSafety.selfThreat(companion,urgent) && eligible(urgent))return false;
  var owner=companion.ownerPlayer();
  var chosen=companion.ownerAttackTarget()!=null?companion.ownerAttackTarget():owner==null?null:owner.getLastHurtMob();
  if(chosen!=selected && companion.settings().combat()==CompanionSettings.Combat.AGGRESSIVE && eligible(chosen))return false;
  return !companion.retreating() && selected==companion.getTarget() && eligible(selected);
 }
 @Override public void start() { companion.getInventory().equipBestWeapon();companion.setTarget(selected); }
 @Override public void stop() { companion.setTarget(null);selected=null; }
}
