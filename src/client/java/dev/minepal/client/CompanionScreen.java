package dev.minepal.client;

import dev.minepal.CompanionMenuProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Vanilla widgets, paged to fit small Minecraft windows, no chat-command typing. */
public final class CompanionScreen extends Screen {
 private CompanionMenuProtocol.State state;
 private int page;
 private EditBox name;
 private int refreshTicks;
 public CompanionScreen(CompanionMenuProtocol.State state) { super(Component.literal("Companion"));this.state=state; }
 public int entityId() { return state.entityId(); }
 public void update(CompanionMenuProtocol.State next) {
  boolean changed=state.flags()!=next.flags() || state.combat()!=next.combat() || state.distance()!=next.distance() || !state.name().equals(next.name());
  if(next.response().isEmpty())next=new CompanionMenuProtocol.State(next.entityId(),next.name(),next.status(),state.response(),next.flags(),next.combat(),next.distance(),next.initial());
  String editing=(page==2 || page==3) && name!=null?name.getValue():null;
  state=next;if(changed){rebuildWidgets();if(editing!=null && name!=null)name.setValue(editing);}
 }
 @Override public void tick() {
  if(minecraft.level==null || minecraft.player==null || !(minecraft.level.getEntity(state.entityId()) instanceof dev.minepal.CompanionEntity c)
   || !c.isAlive() || c.distanceToSqr(minecraft.player)>256) { onClose();return; }
  if(++refreshTicks>=40){refreshTicks=0;send("refresh","");}
 }
 public void send(String action,String value) { ClientPlayNetworking.send(new CompanionMenuProtocol.Action(state.entityId(),action,value)); }
 private void button(String label,String action,int column,int row,String tooltip) {
  int panel=Math.min(380,width-16),x=(width-panel)/2;
  var b=Button.builder(Component.literal(label),ignored->send(action,""))
   .bounds(x+column*(panel/2+2),62+row*23,panel/2-3,20).build();
  if(tooltip!=null)b.setTooltip(Tooltip.create(Component.literal(tooltip)));
  addRenderableWidget(b);
 }
 private String on(int bit) { return (state.flags()&bit)!=0?"On":"Off"; }
 @Override protected void init() {
  int panel=Math.min(380,width-16),x=(width-panel)/2;
  if(page==0) {
   button("Follow me","follow",0,0,null);button("Stay here","stay",1,0,null);
   button("Return home","home",0,1,null);button("Stop task","stop",1,1,null);
   button("Gather wood","gather",0,2,"Permits nearby log harvesting and limited leaf clearing; avoid wooden builds.");
   button("Mine nearby","mine",1,2,"Search loaded, exposed stone/ores; requires a suitable owned tool.");
   button("Craft basic tools","prepare",0,3,"Use real logs, planks and sticks. May place one owned workbench on a safe empty spot.");
   button("Craft inventory recipe","craft_grid",1,3,"Use the first nine inventory slots as a 3x3 recipe. Walk to the nearest reachable table.");
   button("Trade / inventory","inventory",0,4,null);button("Set nearest bed","set_home",1,4,"Assign a loaded bed within eight blocks.");
  } else if(page==1) {
   button("Combat: "+new String[]{"Passive","Defensive","Aggressive"}[Math.clamp(state.combat(),0,2)],"combat",0,0,null);
   button("Distance: "+new String[]{"Close","Normal","Relaxed"}[Math.clamp(state.distance(),0,2)],"distance",1,0,null);
   button("Emergency teleport: "+on(1),"teleport",0,1,null);button("Gifted gear: "+on(2),"gifted",1,1,null);
   button("Gathering: "+on(8),"gathering",0,2,null);button("Mining: "+on(16),"mining",1,2,null);
   button("Basic crafting: "+on(32),"basic_crafting",0,3,"After resource work, turn owned wood into useful supplies and tools when a table is available.");
   button("Clear leaves: "+on(64),"leaves",1,3,"Only leaves obstructing an assigned resource target, within reach and task limits.");
   button("Shared access: "+on(4),"shared",0,4,null);button("Revoke sharing","unshare",1,4,null);
  } else if(page==2) {
   name=addRenderableWidget(new EditBox(font,x,62,panel,20,Component.literal("Companion name")));
   name.setMaxLength(32);name.setValue(state.name());
   addRenderableWidget(Button.builder(Component.literal("Save name"),b->send("name",name.getValue())).bounds(x,86,panel,20).build());
   button("Equip gear","equip",0,3,null);button("Recover equipment","unequip",1,3,null);
   button("Trade / inventory","inventory",0,4,null);button("Set nearest bed","set_home",1,4,null);
  }
  if(page==3) {
   button("Designate nearest chest","share_nearest",0,0,"Grants only the chosen chest/barrel, not unknown nearby storage.");
   button("Shared access: "+on(4),"shared",1,0,null);
   name=addRenderableWidget(new EditBox(font,x,99,panel,20,Component.literal("Storage slot (1-27)")));
   name.setMaxLength(2);name.setValue("1");
   addRenderableWidget(Button.builder(Component.literal("Deposit slot"),b->send("deposit",name.getValue())).bounds(x,124,panel/2-3,20).build());
   addRenderableWidget(Button.builder(Component.literal("Withdraw slot"),b->send("withdraw",name.getValue())).bounds(x+panel/2+2,124,panel/2-3,20).build());
   button("Revoke sharing","unshare",0,4,null);button("Trade / inventory","inventory",1,4,null);
  }
  int navY=height-27;
  addRenderableWidget(Button.builder(Component.literal("<"),b->{page=(page+3)%4;rebuildWidgets();}).bounds(x,navY,32,20).build());
  addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2-44,navY,88,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->{page=(page+1)%4;rebuildWidgets();}).bounds(x+panel-32,navY,32,20).build());
 }
 @Override public void extractRenderState(GuiGraphicsExtractor gui,int mouseX,int mouseY,float partial) {
  gui.fill(0,0,width,height,0xCF101820);
  super.extractRenderState(gui,mouseX,mouseY,partial);
  gui.centeredText(font,state.name()+" — "+new String[]{"Actions","Settings","Equipment & name","Storage"}[page],width/2,12,0xFFFFFFFF);
  int x=(width-Math.min(380,width-16))/2;
  gui.textWithWordWrap(font,Component.literal(state.status()),x,28,Math.min(380,width-16),0xFFCCCCCC);
  if(page==3)gui.text(font,"Source slot (1–27)",x,86,0xFFCCCCCC);
  if(!state.response().isEmpty()) {
   var lines=font.split(Component.literal(state.response()),Math.min(380,width-16));
   for(int i=0;i<Math.min(2,lines.size());i++)gui.text(font,lines.get(i),x,181+i*10,0xFFFFDD88);
  }
 }
 @Override public boolean isPauseScreen() { return false; }
}
