package dev.minepal.client;

import dev.minepal.CompanionMenuProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Six primary choices; advanced controls remain available in named secondary menus. */
public final class CompanionScreen extends Screen {
 private CompanionMenuProtocol.State state;
 // 0 primary, 1 resources, 2 more, 3 settings, 4 equipment/name, 5 storage, 6 work/home.
 private int page,refreshTicks;
 private EditBox name,quantity;
 public CompanionScreen(CompanionMenuProtocol.State state){super(Component.literal("MinePal"));this.state=state;}
 public int entityId(){return state.entityId();}
 public String companionUuid(){return state.companionUuid();}
 public void update(CompanionMenuProtocol.State next){
  boolean changed=state.flags()!=next.flags() || state.combat()!=next.combat() || state.distance()!=next.distance() || !state.name().equals(next.name()) || state.available()!=next.available();
  String editing=name==null?null:name.getValue(),amount=quantity==null?null:quantity.getValue();state=next;
  if(changed){rebuildWidgets();if(name!=null && editing!=null)name.setValue(editing);if(quantity!=null && amount!=null)quantity.setValue(amount);}
 }
 @Override public void tick(){
  if(minecraft.level==null || minecraft.player==null){onClose();return;}
  if(++refreshTicks>=40){refreshTicks=0;send("refresh","");}
 }
 public void send(String action,String value){ClientPlayNetworking.send(new CompanionMenuProtocol.Action(state.entityId(),action,value,state.companionUuid()));}
 private void page(int next){page=next;name=null;quantity=null;rebuildWidgets();}
 private int panel(){return Math.min(380,width-16);}
 private int left(){return (width-panel())/2;}
 private Button widget(String label,int col,int row,boolean primary,Runnable callback,String tooltip){
  var b=Button.builder(Component.literal(label),ignored->callback.run()).bounds(left()+col*(panel()/2+2),(primary?108:62)+row*23,panel()/2-3,20).build();
  if(tooltip!=null)b.setTooltip(Tooltip.create(Component.literal(tooltip)));b.active=state.available();return addRenderableWidget(b);
 }
 private void button(String label,String action,int col,int row,boolean primary,String tooltip){widget(label,col,row,primary,()->send(action,""),tooltip);}
 private void link(String label,int target,int col,int row,boolean primary){widget(label,col,row,primary,()->page(target),null);}
 private String on(int bit){return (state.flags()&bit)!=0?"On":"Off";}
 @Override protected void init(){
  name=null;quantity=null;int x=left(),w=panel();
  if(page==0){
   button("Follow Me","follow",0,0,true,null);button("Stay Here","stay",1,0,true,null);
   link("Gather Resources",1,0,1,true);button("Craft Equipment","prepare",1,1,true,"Uses owned resources; may place one owned bench on safe empty ground.");
   button("Inventory","inventory",0,2,true,null);link("More Options",2,1,2,true);
  }else if(page==1){
   quantity=addRenderableWidget(new EditBox(font,x,78,w,20,Component.literal("Requested quantity")));quantity.setMaxLength(4);quantity.setValue("64");
   widget("Gather Wood",0,0,true,()->send("gather",quantity.getValue()),"Natural-tree search and harvesting. No navigation teleports.");
   widget("Mine Stone",1,0,true,()->send("mine_stone",quantity.getValue()),"Permits safe staircase cuts through dirt/stone; avoids unsupported drops and liquids.");
   widget("Mine Exposed Ore",0,1,true,()->send("mine",quantity.getValue()),"Search available loaded terrain; requires a suitable owned tool.");
   button("Cancel Task","stop",1,1,true,null);
  }else if(page==2){
   link("Combat & Settings",3,0,0,false);link("Equipment & Name",4,1,0,false);
   link("Storage",5,0,1,false);link("Workstations & Home",6,1,1,false);
   button("Return Home","home",0,2,false,null);button("Cancel Task","stop",1,2,false,null);
   button("Inventory","inventory",0,3,false,null);link("Gather Resources",1,1,3,false);
  }else if(page==3){
   button("Combat: "+new String[]{"Passive","Defensive","Aggressive"}[Math.clamp(state.combat(),0,2)],"combat",0,0,false,null);
   button("Distance: "+new String[]{"Close","Normal","Relaxed"}[Math.clamp(state.distance(),0,2)],"distance",1,0,false,null);
   button("Gifted gear: "+on(2),"gifted",0,1,false,null);button("Basic crafting: "+on(32),"basic_crafting",1,1,false,null);
   button("Gathering: "+on(8),"gathering",0,2,false,null);button("Mining: "+on(16),"mining",1,2,false,null);
   button("Clear leaves: "+on(64),"leaves",0,3,false,null);button("PvP assist: "+on(128),"pvp",1,3,false,"Off by default; never attacks the owner, companions, or tame pets.");
   button("Shared access: "+on(4),"shared",0,4,false,null);button("Revoke sharing","unshare",1,4,false,null);
  }else if(page==4){
   name=addRenderableWidget(new EditBox(font,x,62,w,20,Component.literal("Companion name")));name.setMaxLength(32);name.setValue(state.name());
   addRenderableWidget(Button.builder(Component.literal("Save name"),b->send("name",name.getValue())).bounds(x,86,w,20).build());
   button("Equip gear","equip",0,3,false,null);button("Recover equipment","unequip",1,3,false,null);
   button("Inventory","inventory",0,4,false,null);button("Set nearest bed","set_home",1,4,false,null);
  }else if(page==5){
   button("Designate nearest chest","share_nearest",0,0,false,"Only the explicitly selected container is shared.");button("Shared access: "+on(4),"shared",1,0,false,null);
   name=addRenderableWidget(new EditBox(font,x,99,w,20,Component.literal("Storage slot")));name.setMaxLength(2);name.setValue("1");
   addRenderableWidget(Button.builder(Component.literal("Deposit slot"),b->send("deposit",name.getValue())).bounds(x,124,w/2-3,20).build());
   addRenderableWidget(Button.builder(Component.literal("Withdraw slot"),b->send("withdraw",name.getValue())).bounds(x+w/2+2,124,w/2-3,20).build());
   button("Revoke sharing","unshare",0,4,false,null);button("Inventory","inventory",1,4,false,null);
  }else{
   button("Craft Equipment","prepare",0,0,false,"May place an owned workbench if efficient and safe.");button("Craft inventory recipe","craft_grid",1,0,false,"Uses the first nine personal slots; searches remembered/loaded tables.");
   button("Set nearest bed","set_home",0,1,false,null);button("Return Home","home",1,1,false,null);
   button("Equip gear","equip",0,2,false,null);button("Inventory","inventory",1,2,false,null);
  }
  if(page!=0)addRenderableWidget(Button.builder(Component.literal("Back"),b->page(page==3||page==4||page==5||page==6?2:0)).bounds(x,height-27,72,20).build());
  addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2-44,height-27,88,20).build());
 }
 private void wrapped(GuiGraphicsExtractor gui,String text,int y,int maxLines,int color){
  var lines=font.split(Component.literal(text),panel());for(int i=0;i<Math.min(maxLines,lines.size());i++)gui.text(font,lines.get(i),left(),y+i*10,color);
 }
 @Override public void extractRenderState(GuiGraphicsExtractor gui,int mouseX,int mouseY,float partial){
  gui.fill(0,0,width,height,0xE0101820);super.extractRenderState(gui,mouseX,mouseY,partial);
  String heading=new String[]{"MinePal","Resources","More Options","Combat & Settings","Equipment & Name","Storage","Workstations & Home"}[page];
  gui.centeredText(font,page==0?state.name():state.name()+" — "+heading,width/2,12,0xFFFFFFFF);
  String health=state.available()?String.format(java.util.Locale.ROOT,"Health: %.0f/%.0f   Hunger: %d/20   Combat: %s",state.health(),state.maxHealth(),state.hunger(),new String[]{"Passive","Defensive","Aggressive"}[Math.clamp(state.combat(),0,2)]):"Status unknown: companion not loaded";
  wrapped(gui,health,28,1,0xFFDDDDDD);
  if(page==0){
   wrapped(gui,"Current Task: "+(state.objective().equals("None")?"Idle":state.objective()),42,1,0xFFFFFFFF);
   wrapped(gui,"Progress: "+(state.objective().equals("None")?"—":state.collected()+"/"+(state.requested()==0?"open-ended":state.requested()))+"   State: "+state.phase().replace('_',' '),54,1,0xFFCCCCCC);
   wrapped(gui,"Status: "+state.detail(),70,3,0xFFCCCCCC);
  }else{
   wrapped(gui,"Task: "+state.objective()+"  "+state.collected()+"/"+(state.requested()==0?"∞":state.requested()),42,1,0xFFCCCCCC);
  }
  if(page==1)gui.text(font,"Quantity (0 = until canceled)",left(),64,0xFFCCCCCC);
  if(page==5)gui.text(font,"Source slot (1–27)",left(),86,0xFFCCCCCC);
  if(!state.response().isEmpty())wrapped(gui,state.response(),181,2,0xFFFFDD88);
 }
 @Override public boolean isPauseScreen(){return false;}
}
