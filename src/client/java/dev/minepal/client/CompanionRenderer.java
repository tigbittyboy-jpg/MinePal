package dev.minepal.client;
import dev.minepal.CompanionEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
public class CompanionRenderer extends HumanoidMobRenderer<CompanionEntity,HumanoidRenderState,HumanoidModel<HumanoidRenderState>> {
 private static final Identifier TEXTURE=Identifier.fromNamespaceAndPath("companion","textures/entity/companion.png");
 public CompanionRenderer(EntityRendererProvider.Context context) {
  super(context,new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),0.5F);
  addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
   ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR,context.getModelSet(),HumanoidModel<HumanoidRenderState>::new),context.getEquipmentRenderer()));
 }
 @Override public HumanoidRenderState createRenderState() { return new HumanoidRenderState(); }
 @Override public Identifier getTextureLocation(HumanoidRenderState state) { return TEXTURE; }
}
