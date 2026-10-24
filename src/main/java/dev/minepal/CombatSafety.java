package dev.minepal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;

public final class CombatSafety {
 private CombatSafety() { }
 public static boolean valid(CompanionEntity c,LivingEntity target) {
  // PvP remains unavailable in this release, including when an owner punches another player.
  return target!=null && target!=c && target.isAlive() && (!(target instanceof Player p) || (c.settings().pvpAssist() && !c.ownedBy(p) && !p.isSpectator()))
   && !(target instanceof CompanionEntity) && !(target instanceof TamableAnimal);
 }
 public static boolean selfThreat(CompanionEntity c,LivingEntity target) {
  return target==c.getLastHurtByMob() && c.tickCount-c.getLastHurtByMobTimestamp()<=200;
 }
 public static boolean ownerThreat(CompanionEntity c,LivingEntity target) {
  var owner=c.ownerPlayer();return owner!=null && target==owner.getLastHurtByMob() && owner.tickCount-owner.getLastHurtByMobTimestamp()<=200;
 }
 public static boolean ownerSelected(CompanionEntity c,LivingEntity target) {
  var marked=c.ownerAttackTarget();if(target!=null && marked==target)return true;
  var owner=c.ownerPlayer();return owner!=null && target==owner.getLastHurtMob() && (owner.tickCount-owner.getLastHurtMobTimestamp()<=200 || c.getTarget()==target);
 }
}
