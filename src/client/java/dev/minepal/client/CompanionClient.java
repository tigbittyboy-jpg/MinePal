package dev.minepal.client;
import dev.minepal.CompanionMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
public class CompanionClient implements ClientModInitializer {
 @Override public void onInitializeClient() { EntityRendererRegistry.register(CompanionMod.TYPE,CompanionRenderer::new);
 net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.minepal.CompanionMenuProtocol.State.TYPE,
  (state,context)->context.client().execute(()->{
   if(context.client().gui.screen() instanceof CompanionScreen screen && screen.companionUuid().equals(state.companionUuid()))screen.update(state);
   else if(state.initial())context.client().gui.setScreen(new CompanionScreen(state));
  })); }
}
