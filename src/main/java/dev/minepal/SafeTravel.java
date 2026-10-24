package dev.minepal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Bounded, loaded-chunk-only teleport fallback; never changes blocks or loads terrain. */
public final class SafeTravel {
 private SafeTravel() { }
 public static boolean safeLanding(CompanionEntity companion,ServerLevel server,BlockPos pos) {
  if(!server.getWorldBorder().isWithinBounds(pos))return false;
  for(BlockPos nearby:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1))) {
   if(!server.hasChunkAt(nearby))return false;
   BlockState state=server.getBlockState(nearby);
   if(!state.getFluidState().isEmpty() || hazardous(state))return false;
  }
  if(!server.getBlockState(pos.below()).isFaceSturdy(server,pos.below(),Direction.UP))return false;
  var landing=Vec3.atBottomCenterOf(pos);
  var bounds=companion.getBoundingBox().move(landing.subtract(companion.position()));
  return server.getWorldBorder().isWithinBounds(bounds) && server.noCollision(companion,bounds);
 }
 private static boolean hazardous(BlockState s) {
  return s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)||s.is(Blocks.MAGMA_BLOCK)||s.is(Blocks.CACTUS)
   ||s.is(Blocks.CAMPFIRE)||s.is(Blocks.SOUL_CAMPFIRE)||s.is(Blocks.POWDER_SNOW)
   ||s.is(Blocks.SWEET_BERRY_BUSH)||s.is(Blocks.WITHER_ROSE)||s.is(Blocks.POINTED_DRIPSTONE)
   ||s.is(Blocks.NETHER_PORTAL)||s.is(Blocks.END_PORTAL);
 }
 static boolean teleportNear(CompanionEntity companion,Player owner) { return false; }
}
