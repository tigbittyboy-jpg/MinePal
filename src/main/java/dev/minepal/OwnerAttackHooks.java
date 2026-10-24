package dev.minepal;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

/** Registers attack intent server-side; successful vanilla-hit memory remains a fallback. */
public final class OwnerAttackHooks {
 private OwnerAttackHooks() { }
 public static void register(){
  AttackEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
   if(world instanceof ServerLevel server && entity instanceof LivingEntity target && !player.isSpectator()) {
    for(var c:server.getEntitiesOfClass(CompanionEntity.class,player.getBoundingBox().inflate(32),npc->npc.ownedBy(player)))
     c.registerOwnerAttack(target);
   }
   return InteractionResult.PASS;
  });
 }
}
