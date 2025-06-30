package com.flechazo.modernfurniture.client.model;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.DisplayBlock;
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
        boolean isPowered = animatable.getBlockState().getValue(DisplayBlock.POWERED);

        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID,
                "textures/block/display_" + ((DisplayBlock) animatable.getBlockState().getBlock()).getColor() + "_" + (isPowered ? "on" : "off") + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(DisplayBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "animations/display.animation.json");
    }
}