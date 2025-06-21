package com.flechazo.modernfurniture.client.renderer;

import com.flechazo.modernfurniture.block.AbstractDisplayBlock;
import com.flechazo.modernfurniture.block.BlackDisplayBlock;
import com.flechazo.modernfurniture.block.DisplayBlockEntity;
import com.flechazo.modernfurniture.client.model.DisplayBlockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class DisplayBlockRenderer extends GeoBlockRenderer<DisplayBlockEntity> {
    public DisplayBlockRenderer() {
        super(new DisplayBlockModel());
    }
    
    @Override
    public void renderRecursively(PoseStack poseStack, DisplayBlockEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean isPowered = animatable.getBlockState().getValue(AbstractDisplayBlock.POWERED);

        if (bone.getName().equals("main") && isPowered) {
            packedLight = 15728880;
        }
        
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}