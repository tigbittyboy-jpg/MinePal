package dev.minepal;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.phys.AABB;

public class CompanionClientGameTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext context) {
  try(var world=context.worldBuilder().create()) {
   world.getConnection().waitForChunksDownload();
   world.getServer().runOnServer(server->{
    var player=world.getConnection().getServerPlayer();var level=player.level();
    // Small controlled platform ensures the actor and camera cannot fall.
    var base=player.blockPosition();
    for(int x=-4;x<=4;x++)for(int z=-4;z<=6;z++) {
     level.setBlockAndUpdate(base.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
     for(int y=0;y<4;y++)level.setBlockAndUpdate(base.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }
    player.teleportTo(base.getX()+0.5,base.getY(),base.getZ()+0.5);player.setYRot(0);player.setXRot(0);
    var c=new CompanionEntity(CompanionMod.TYPE,level);c.claim(player);
    c.snapTo(base.getX()+0.5,base.getY(),base.getZ()+4.5,180,0);
    c.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BOOTS));
    c.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
    level.addFreshEntity(c);
    var chestPos=base.offset(2,0,2);level.setBlockAndUpdate(chestPos,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
    ((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chestPos)).setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,3));
   });
   world.getConnection().waitForClientboundEntityUpdates(CompanionMod.TYPE);
   context.waitFor(client->client.level.getEntitiesOfClass(CompanionEntity.class,client.player.getBoundingBox().inflate(12)).size()==1);
   context.waitTicks(40);
   context.runOnClient(client->{
    var c=client.level.getEntitiesOfClass(CompanionEntity.class,client.player.getBoundingBox().inflate(12)).getFirst();
    c.setYRot(180);c.yRotO=180;c.setYHeadRot(180);c.yHeadRotO=180;c.yBodyRot=180;c.yBodyRotO=180;
   });
   System.out.println("COMPANION_TEST_RENDERING_SCREENSHOT="+context.takeScreenshot("companion-rendering"));
   world.getServer().runOnServer(server->{
    var player=world.getConnection().getServerPlayer();
    var c=player.level().getEntitiesOfClass(CompanionEntity.class,player.getBoundingBox().inflate(12)).getFirst();
    c.interact(player,net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO);
   });
   context.waitForScreen(dev.minepal.client.CompanionScreen.class);
   System.out.println("COMPANION_MENU_ACTIONS_SCREENSHOT="+context.takeScreenshot("companion-menu-actions"));
   context.clickScreenButton("Follow me");
   world.getServer().waitFor(server->world.getConnection().getServerLevel().getEntitiesOfClass(CompanionEntity.class,
    world.getConnection().getServerPlayer().getBoundingBox().inflate(12)).getFirst().mode()==CompanionEntity.Mode.FOLLOW);
   context.waitTicks(5);context.clickScreenButton("Stay here");
   world.getServer().waitFor(server->world.getConnection().getServerLevel().getEntitiesOfClass(CompanionEntity.class,
    world.getConnection().getServerPlayer().getBoundingBox().inflate(12)).getFirst().mode()==CompanionEntity.Mode.STAY);
   context.waitTicks(5);context.clickScreenButton(">");
   context.clickScreenButton("Combat: Defensive");context.waitTicks(5);
   context.clickScreenButton("Emergency teleport: On");context.waitTicks(5);
   world.getServer().runOnServer(server->{
    var player=world.getConnection().getServerPlayer();
    var c=player.level().getEntitiesOfClass(CompanionEntity.class,player.getBoundingBox().inflate(12)).getFirst();
    if(c.settings().combat()!=CompanionSettings.Combat.AGGRESSIVE || c.settings().emergencyTeleport())throw new AssertionError("UI setting buttons did not mutate actual server state");
   });
   System.out.println("COMPANION_MENU_SETTINGS_SCREENSHOT="+context.takeScreenshot("companion-menu-settings"));
   context.clickScreenButton(">");
   context.runOnClient(client->{
    var edit=(net.minecraft.client.gui.components.EditBox)client.gui.screen().children().stream()
     .filter(child->child instanceof net.minecraft.client.gui.components.EditBox).findFirst().orElseThrow();edit.setValue("Rowan");
   });
   context.clickScreenButton("Save name");context.waitTicks(5);
   world.getServer().runOnServer(server->{
    var player=world.getConnection().getServerPlayer();
    if(!player.level().getEntitiesOfClass(CompanionEntity.class,player.getBoundingBox().inflate(12)).getFirst().getName().getString().equals("Rowan"))throw new AssertionError("UI rename must persist on the server");
   });
   context.clickScreenButton(">");
   context.clickScreenButton("Designate nearest chest");context.waitTicks(5);
   context.clickScreenButton("Shared access: Off");context.waitTicks(5);
   context.clickScreenButton("Withdraw slot");context.waitTicks(5);
   world.getServer().runOnServer(server->{
    var player=world.getConnection().getServerPlayer();var c=player.level().getEntitiesOfClass(CompanionEntity.class,player.getBoundingBox().inflate(12)).getFirst();
    if(c.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND)!=3)throw new AssertionError("UI withdrawal must transfer the actual chest stack");
   });
   context.clickScreenButton("Deposit slot");context.waitTicks(5);
   world.getServer().runOnServer(server->{
    var player=world.getConnection().getServerPlayer();var c=player.level().getEntitiesOfClass(CompanionEntity.class,player.getBoundingBox().inflate(12)).getFirst();
    var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)player.level().getBlockEntity(player.blockPosition().offset(2,0,2));
    if(c.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND)!=0 || chest.countItem(net.minecraft.world.item.Items.DIAMOND)!=3)throw new AssertionError("UI deposit must preserve total item count");
   });
   System.out.println("COMPANION_MENU_STORAGE_SCREENSHOT="+context.takeScreenshot("companion-menu-storage"));
   context.clickScreenButton("Done");context.waitTicks(10);
   context.runOnClient(client->{if(client.gui.screen() instanceof dev.minepal.client.CompanionScreen)throw new AssertionError("A delayed refresh must not reopen a closed menu");});
   System.out.println("COMPANION_MENU_TEST_PASSED: right-click screen, follow/stay actions, authoritative settings, rename, real shared withdrawal/deposit, no reopen after closing");
   world.getServer().runOnServer(server->{
    var player=world.getConnection().getServerPlayer();
    var companions=player.level().getEntitiesOfClass(CompanionEntity.class,player.getBoundingBox().inflate(12));
    var c=companions.getFirst();
    try {
     var dispatcher=server.getCommands().getDispatcher();var source=player.createCommandSourceStack();
     dispatcher.execute("companion combat passive",source);dispatcher.execute("companion distance relaxed",source);
     dispatcher.execute("companion teleport off",source);
     if(c.settings().combat()!=CompanionSettings.Combat.PASSIVE || c.settings().distance()!=CompanionSettings.FollowDistance.RELAXED
      || c.settings().emergencyTeleport())throw new AssertionError("Integrated command settings did not apply");
     dispatcher.execute("companion inventory",source);
    } catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex) { throw new AssertionError(ex); }
   });
   context.waitForScreen(net.minecraft.client.gui.screens.inventory.ContainerScreen.class);
   System.out.println("COMPANION_TEST_INVENTORY_SCREENSHOT="+context.takeScreenshot("companion-inventory"));
   System.out.println("COMPANION_CLIENT_TEST_PASSED: entity synchronization, equipped rendering, owner inventory screen, integrated server commands");
  }
 }
}
