package com.flechazo.modernfurniture.client.model;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.AbstractAirConditioningBlock;
import com.flechazo.modernfurniture.block.entity.AbstractAirConditioningBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;


public abstract class AbstractAirConditioningBlockModel<T extends AbstractAirConditioningBlockEntity> extends GeoModel<T> {

    protected abstract String getModelPrefix();

    protected abstract String getTexturePrefix();

    protected abstract String getAnimationPath();

    @Override
    public ResourceLocation getModelResource(T animatable) {
        boolean isOpen = animatable.getBlockState().getValue(AbstractAirConditioningBlock.OPEN);
        String suffix = isOpen ? "_open.geo.json" : "_close.geo.json";
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "geo/" + getModelPrefix() + suffix);
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        boolean isOpen = animatable.getBlockState().getValue(AbstractAirConditioningBlock.OPEN);
        String suffix = isOpen ? "_on.png" : "_off.png";
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "textures/block/" + getTexturePrefix() + suffix);
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "animations/" + getAnimationPath() + ".animation.json");
    }
}