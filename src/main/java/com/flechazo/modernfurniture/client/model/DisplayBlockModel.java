package com.flechazo.modernfurniture.client.model;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.BlackDisplayBlock;
import com.flechazo.modernfurniture.block.DisplayBlockEntity;
import com.flechazo.modernfurniture.block.WhiteDisplayBlock;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DisplayBlockModel extends GeoModel<DisplayBlockEntity> {
    @Override
    public ResourceLocation getModelResource(DisplayBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "geo/display_block.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DisplayBlockEntity animatable) {
        boolean isPowered = animatable.getBlockState().getValue(animatable.getBlockState().getBlock() instanceof BlackDisplayBlock ?
            BlackDisplayBlock.POWERED : WhiteDisplayBlock.POWERED);
        
        if (animatable.getBlockState().getBlock() instanceof BlackDisplayBlock) {
            return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID,
                isPowered ? "textures/block/black_on.png" : "textures/block/black_off.png");
        } else {
            return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID,
                isPowered ? "textures/block/white_on.png" : "textures/block/white_off.png");
        }
    }

    @Override
    public ResourceLocation getAnimationResource(DisplayBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "animations/display_block.animation.json");
    }
}