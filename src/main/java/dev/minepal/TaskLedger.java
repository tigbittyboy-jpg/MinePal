package dev.minepal;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persistent objective/progress; interruption overlays never discard the underlying assignment. */
public final class TaskLedger {
 public enum Phase { PLANNING,SEARCHING,TRAVELING,EXCAVATING,CLEARING_OBSTRUCTIONS,WORKING,COLLECTING,FIGHTING,RETREATING,RETURNING,COMPLETED,BLOCKED,CANCELED }
 private String objective="None",detail="Ready";
 private int requested,collected;
 private Phase phase=Phase.COMPLETED,previous=Phase.COMPLETED;
 public void begin(String objective,int requested) { this.objective=objective;this.requested=Math.clamp(requested,0,4096);collected=0;phase=previous=Phase.PLANNING;detail="Planning"; }
 public void state(Phase next,String reason) { phase=next;detail=reason; }
 public void add(int amount) { collected=Math.min(1_000_000,collected+Math.max(0,amount)); }
 public boolean quotaMet() { return requested>0 && collected>=requested; }
 public int collected() { return collected; }
 public int requested() { return requested; }
 public String objective() { return objective; }
 public String detail() { return detail; }
 public Phase phase() { return phase; }
 public void interrupt(boolean fleeing,boolean fighting) {
  if(fleeing || fighting) {
   if(phase!=Phase.RETREATING && phase!=Phase.FIGHTING)previous=phase;
   phase=fleeing?Phase.RETREATING:Phase.FIGHTING;
  } else if(phase==Phase.RETREATING || phase==Phase.FIGHTING)phase=previous;
 }
 public String progress() { return collected+"/"+(requested==0?"open-ended":requested); }
 public void save(ValueOutput out) {
  out.putString("Objective",objective);out.putString("Detail",detail);out.putInt("Requested",requested);out.putInt("Collected",collected);
  out.putString("Phase",phase.name());out.putString("Previous",previous.name());
 }
 public void load(ValueInput in) {
  objective=in.getStringOr("Objective","None");detail=in.getStringOr("Detail","Ready");requested=Math.clamp(in.getIntOr("Requested",0),0,4096);collected=Math.clamp(in.getIntOr("Collected",0),0,1_000_000);
  try{phase=Phase.valueOf(in.getStringOr("Phase","COMPLETED"));}catch(IllegalArgumentException ignored){phase=Phase.BLOCKED;}
  try{previous=Phase.valueOf(in.getStringOr("Previous","SEARCHING"));}catch(IllegalArgumentException ignored){previous=Phase.SEARCHING;}
  if(phase==Phase.FIGHTING || phase==Phase.RETREATING)phase=previous;
 }
}
