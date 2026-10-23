package dev.minepal.client;
import dev.minepal.CompanionMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
public class CompanionClient implements ClientModInitializer {
 @Override public void onInitializeClient() { EntityRendererRegistry.register(CompanionMod.TYPE,CompanionRenderer::new); }
}
