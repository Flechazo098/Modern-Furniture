package com.flechazo.modernfurniture.client.renderer;

import com.flechazo.modernfurniture.block.LaptopBlock;
import com.flechazo.modernfurniture.block.LaptopBlockEntity;
import com.flechazo.modernfurniture.client.model.LaptopBlockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class LaptopBlockRenderer extends GeoBlockRenderer<LaptopBlockEntity> {
    public LaptopBlockRenderer() {
        super(new LaptopBlockModel());
    }
    
    @Override
    public void renderRecursively(PoseStack poseStack, LaptopBlockEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean isOpen = animatable.getBlockState().getValue(LaptopBlock.OPEN);

        if (bone.getName().equals("screen") && isOpen) {
            packedLight = Math.max(packedLight, 12000);
        }
        
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}