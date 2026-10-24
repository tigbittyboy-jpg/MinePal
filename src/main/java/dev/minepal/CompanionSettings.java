package dev.minepal;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Per-companion settings. Invalid or old optional data falls back safely. */
public final class CompanionSettings {
 public enum Combat { PASSIVE, DEFENSIVE, AGGRESSIVE }
 public enum FollowDistance {
  CLOSE(2), NORMAL(4), RELAXED(6);
  private final int blocks;
  FollowDistance(int blocks) { this.blocks=blocks; }
  public int blocks() { return blocks; }
 }
 private Combat combat=Combat.AGGRESSIVE;
 private FollowDistance distance=FollowDistance.NORMAL;
 private boolean emergencyTeleport=false;
 private boolean useGiftedEquipment=true;
 private boolean sharedStorage=false,pvpAssist=false;
 private boolean gathering=true, mining=true;
 private boolean basicCrafting=true, clearLeaves=true;
 public boolean pvpAssist() { return pvpAssist; }
 public void pvpAssist(boolean value) { pvpAssist=value; }
 public boolean basicCrafting() { return basicCrafting; }
 public boolean clearLeaves() { return clearLeaves; }
 public void basicCrafting(boolean v) { basicCrafting=v; }
 public void clearLeaves(boolean v) { clearLeaves=v; }
 public boolean gathering() { return gathering; }
 public boolean mining() { return mining; }
 public void gathering(boolean value) { gathering=value; }
 public void mining(boolean value) { mining=value; }
 public Combat combat() { return combat; }
 public FollowDistance distance() { return distance; }
 public boolean emergencyTeleport() { return false; }
 public boolean useGiftedEquipment() { return useGiftedEquipment; }
 public boolean sharedStorage() { return sharedStorage; }
 public void combat(Combat value) { combat=java.util.Objects.requireNonNull(value); }
 public void distance(FollowDistance value) { distance=java.util.Objects.requireNonNull(value); }
 public void emergencyTeleport(boolean value) { emergencyTeleport=false; }
 public void useGiftedEquipment(boolean value) { useGiftedEquipment=value; }
 public void sharedStorage(boolean value) { sharedStorage=value; }
 public void save(ValueOutput out) {
  out.putBoolean("PvpAssist",pvpAssist);
  out.putBoolean("BasicCrafting",basicCrafting);out.putBoolean("ClearLeaves",clearLeaves);
  out.putBoolean("Gathering",gathering);out.putBoolean("Mining",mining);
  out.putString("Combat",combat.name());out.putString("FollowDistance",distance.name());
  out.putBoolean("EmergencyTeleport",emergencyTeleport);out.putBoolean("UseGiftedEquipment",useGiftedEquipment);
  out.putBoolean("SharedStorage",sharedStorage);
 }
 public void load(ValueInput in) {
  pvpAssist=in.getBooleanOr("PvpAssist",false);
  basicCrafting=in.getBooleanOr("BasicCrafting",true);clearLeaves=in.getBooleanOr("ClearLeaves",true);
  gathering=in.getBooleanOr("Gathering",true);mining=in.getBooleanOr("Mining",true);
  try { combat=Combat.valueOf(in.getStringOr("Combat","AGGRESSIVE")); } catch(IllegalArgumentException ignored) { combat=Combat.AGGRESSIVE; }
  try { distance=FollowDistance.valueOf(in.getStringOr("FollowDistance","NORMAL")); } catch(IllegalArgumentException ignored) { distance=FollowDistance.NORMAL; }
  emergencyTeleport=false; // Legacy save key retained, but navigation teleportation is no longer permitted.
  useGiftedEquipment=in.getBooleanOr("UseGiftedEquipment",true);
  sharedStorage=in.getBooleanOr("SharedStorage",false);
 }
 public String summary() {
  return "Combat: "+combat+", follow: "+distance+", teleport: "+emergencyTeleport+
   ", gathering: "+gathering+", mining: "+mining+", gifted equipment: "+useGiftedEquipment+", shared storage: "+sharedStorage;
 }
}
