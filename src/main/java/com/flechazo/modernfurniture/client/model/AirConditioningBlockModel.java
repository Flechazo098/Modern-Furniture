package com.flechazo.modernfurniture.client.model;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.AirConditioningBlock;
import com.flechazo.modernfurniture.block.entity.AirConditioningBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AirConditioningBlockModel extends GeoModel<AirConditioningBlockEntity> {
    @Override
    public ResourceLocation getModelResource(AirConditioningBlockEntity animatable) {
        // 使用 BlockState 获取状态，而不是依赖可能延迟加载的 BlockEntity 内部字段
        boolean isOpen = animatable.getBlockState().getValue(AirConditioningBlock.OPEN);
        return isOpen ?
            ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "geo/wall_mounted_air_conditioning_open.geo.json") :
            ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "geo/wall_mounted_air_conditioning_close.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AirConditioningBlockEntity animatable) {
        // 同样使用 BlockState 获取状态
        boolean isOpen = animatable.getBlockState().getValue(AirConditioningBlock.OPEN);
        return isOpen ?
            ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "textures/block/wall_mounted_air_conditioning_on.png") :
            ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "textures/block/wall_mounted_air_conditioning_off.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AirConditioningBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "animations/wall_mounted_air_conditioning.animation.json");
    }
}