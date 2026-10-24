package dev.minepal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.List;

/** Relative danger, not a blanket fear of every living entity. */
public final class ThreatAssessment {
 private ThreatAssessment() { }
 public static boolean dangerousTo(CompanionEntity c,LivingEntity e) {
  return e.isAlive() && e!=c && (e instanceof Enemy || e==c.getLastHurtByMob()
   || (e instanceof Mob mob && (mob.getTarget()==c || (c.ownerPlayer()!=null && mob.getTarget()==c.ownerPlayer()))));
 }
 public static double power(LivingEntity e) {
  String id=BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
  if(id.contains("warden"))return 32;
  if(id.contains("wither") || id.contains("ender_dragon"))return 24;
  if(id.contains("iron_golem"))return 16;
  if(id.contains("ravager") || id.contains("piglin_brute"))return 11;
  if(id.contains("creeper"))return 8;
  if(e instanceof Enemy)return 5;
  if(e.getMaxHealth()>30)return 7;
  return e.getMaxHealth()<=6?0.4:2;
 }
 public static List<LivingEntity> threats(CompanionEntity c) {
  return c.level().getEntitiesOfClass(LivingEntity.class,c.getBoundingBox().inflate(12),e->dangerousTo(c,e));
 }
 public static double score(CompanionEntity c) {
  double danger=0;
  for(var e:threats(c))danger+=power(e)*Math.max(0.2,1-Math.sqrt(c.distanceToSqr(e))/16);
  double damage=c.recentDamage();
  double armor=c.getArmorValue()*0.32;
  double healthRatio=c.getHealth()/Math.max(1,c.getMaxHealth());
  double weapon=c.getMainHandItem().isEmpty()?0:1.5;
  double needs=(1-healthRatio)*8+damage*0.6+(c.getNavigation().isStuck()?2:0);
  if(c.isOnFire() || c.isUnderWater())needs+=3;
  return Math.max(0,danger+needs-armor-weapon);
 }
 public static boolean shouldRetreat(CompanionEntity c) {
  if(c.getHealth()<=6 && !threats(c).isEmpty())return true;
  return !threats(c).isEmpty() && score(c)>Math.max(8,c.getHealth()*0.8);
 }
}
