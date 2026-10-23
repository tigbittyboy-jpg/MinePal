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
 private Combat combat=Combat.DEFENSIVE;
 private FollowDistance distance=FollowDistance.NORMAL;
 private boolean emergencyTeleport=true;
 private boolean useGiftedEquipment=true;
 private boolean sharedStorage=false;
 private boolean gathering=true, mining=true;
 public boolean gathering() { return gathering; }
 public boolean mining() { return mining; }
 public void gathering(boolean value) { gathering=value; }
 public void mining(boolean value) { mining=value; }
 public Combat combat() { return combat; }
 public FollowDistance distance() { return distance; }
 public boolean emergencyTeleport() { return emergencyTeleport; }
 public boolean useGiftedEquipment() { return useGiftedEquipment; }
 public boolean sharedStorage() { return sharedStorage; }
 public void combat(Combat value) { combat=java.util.Objects.requireNonNull(value); }
 public void distance(FollowDistance value) { distance=java.util.Objects.requireNonNull(value); }
 public void emergencyTeleport(boolean value) { emergencyTeleport=value; }
 public void useGiftedEquipment(boolean value) { useGiftedEquipment=value; }
 public void sharedStorage(boolean value) { sharedStorage=value; }
 public void save(ValueOutput out) {
  out.putBoolean("Gathering",gathering);out.putBoolean("Mining",mining);
  out.putString("Combat",combat.name());out.putString("FollowDistance",distance.name());
  out.putBoolean("EmergencyTeleport",emergencyTeleport);out.putBoolean("UseGiftedEquipment",useGiftedEquipment);
  out.putBoolean("SharedStorage",sharedStorage);
 }
 public void load(ValueInput in) {
  gathering=in.getBooleanOr("Gathering",true);mining=in.getBooleanOr("Mining",true);
  try { combat=Combat.valueOf(in.getStringOr("Combat","DEFENSIVE")); } catch(IllegalArgumentException ignored) { combat=Combat.DEFENSIVE; }
  try { distance=FollowDistance.valueOf(in.getStringOr("FollowDistance","NORMAL")); } catch(IllegalArgumentException ignored) { distance=FollowDistance.NORMAL; }
  emergencyTeleport=in.getBooleanOr("EmergencyTeleport",true);
  useGiftedEquipment=in.getBooleanOr("UseGiftedEquipment",true);
  sharedStorage=in.getBooleanOr("SharedStorage",false);
 }
 public String summary() {
  return "Combat: "+combat+", follow: "+distance+", teleport: "+emergencyTeleport+
   ", gathering: "+gathering+", mining: "+mining+", gifted equipment: "+useGiftedEquipment+", shared storage: "+sharedStorage;
 }
}
