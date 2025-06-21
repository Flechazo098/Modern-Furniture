package com.flechazo.modernfurniture.client.model;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.AbstractDisplayBlock;
import com.flechazo.modernfurniture.block.BlackDisplayBlock;
import com.flechazo.modernfurniture.block.entity.DisplayBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DisplayBlockModel extends GeoModel<DisplayBlockEntity> {
    @Override
    public ResourceLocation getModelResource(DisplayBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "geo/display.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DisplayBlockEntity animatable) {
        boolean isPowered = animatable.getBlockState().getValue(AbstractDisplayBlock.POWERED);
        
        if (animatable.getBlockState().getBlock() instanceof BlackDisplayBlock) {
            return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID,
                isPowered ? "textures/block/display_black_on.png" : "textures/block/display_black_off.png");
        } else {
            return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID,
                isPowered ? "textures/block/display_white_on.png" : "textures/block/display_white_off.png");
        }
    }

    @Override
    public ResourceLocation getAnimationResource(DisplayBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "animations/display.animation.json");
    }
}