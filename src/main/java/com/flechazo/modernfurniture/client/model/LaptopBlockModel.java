package com.flechazo.modernfurniture.client.model;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.LaptopBlock;
import com.flechazo.modernfurniture.block.entity.LaptopBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.model.GeoModel;

public class LaptopBlockModel extends GeoModel<LaptopBlockEntity> {
    @Override
    public ResourceLocation getModelResource(LaptopBlockEntity animatable) {
        // 直接从 BlockState 获取状态，而不是从 BlockEntity
        if (animatable.getLevel() != null) {
            BlockState state = animatable.getLevel().getBlockState(animatable.getBlockPos());
            if (state.hasProperty(LaptopBlock.OPEN) && state.getValue(LaptopBlock.OPEN)) {
                return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "geo/laptop_open.geo.json");
            }
        }
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "geo/laptop_close.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LaptopBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "textures/block/laptop.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LaptopBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "animations/laptop.animation.json");
    }
}