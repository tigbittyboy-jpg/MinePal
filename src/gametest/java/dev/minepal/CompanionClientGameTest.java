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
