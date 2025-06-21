package com.flechazo.modernfurniture.client.model;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.LaptopBlock;
import com.flechazo.modernfurniture.block.entity.LaptopBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LaptopBlockModel extends GeoModel<LaptopBlockEntity> {
    @Override
    public ResourceLocation getModelResource(LaptopBlockEntity animatable) {
        return animatable.getBlockState().getValue(LaptopBlock.OPEN) ?
            ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "geo/laptop_open.geo.json") :
            ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "geo/laptop_closed.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LaptopBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "textures/block/laptop.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LaptopBlockEntity animatable) {
        // 使用包含所有动画的单一文件
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "animations/laptop.animation.json");
    }
}