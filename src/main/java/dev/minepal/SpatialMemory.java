package dev.minepal;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Bounded spatial memory, separated by dimension; old cells expire instead of being rescanned every tick. */
public final class SpatialMemory {
 public enum Kind { FOREST,CAVE,TABLE }
 private final LinkedHashMap<String,Long> searched=new LinkedHashMap<>();
 private final LinkedHashMap<String,BlockPos> known=new LinkedHashMap<>();
 private static String cell(String dimension,BlockPos p) { return dimension+"|"+Math.floorDiv(p.getX(),16)+"|"+Math.floorDiv(p.getZ(),16); }
 public boolean recentlySearched(String dimension,BlockPos p,long now) { Long at=searched.get(cell(dimension,p));return at!=null && now-at<2400; }
 public void searched(String dimension,BlockPos p,long now) { searched.put(cell(dimension,p),now);while(searched.size()>512)searched.remove(searched.keySet().iterator().next()); }
 public void remember(String dimension,Kind kind,BlockPos pos) {
  known.put(dimension+"|"+kind+"|"+pos.asLong(),pos.immutable());while(known.size()>128)known.remove(known.keySet().iterator().next());
 }
 public List<BlockPos> locations(String dimension,Kind kind) { return known.entrySet().stream().filter(e->e.getKey().startsWith(dimension+"|"+kind+"|")).map(Map.Entry::getValue).toList(); }
 public int searchedCount() { return searched.size(); }
 public void save(ValueOutput out) {
  out.store("Searched",Codec.STRING.listOf(),searched.entrySet().stream().map(e->e.getKey()+"|"+e.getValue()).toList());
  out.store("Known",Codec.STRING.listOf(),new ArrayList<>(known.keySet()));
 }
 public void load(ValueInput in) {
  searched.clear();known.clear();
  for(String s:in.read("Searched",Codec.STRING.listOf()).orElse(List.of()).stream().limit(512).toList()) {
   int split=s.lastIndexOf('|');if(split<0)continue;try{searched.put(s.substring(0,split),Long.parseLong(s.substring(split+1)));}catch(NumberFormatException ignored){}
  }
  for(String s:in.read("Known",Codec.STRING.listOf()).orElse(List.of()).stream().limit(128).toList()) {
   int split=s.lastIndexOf('|');if(split<0)continue;try{known.put(s,BlockPos.of(Long.parseLong(s.substring(split+1))));}catch(NumberFormatException ignored){}
  }
 }
}
