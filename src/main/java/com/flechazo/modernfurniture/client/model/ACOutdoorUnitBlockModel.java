package com.flechazo.modernfurniture.client.model;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.entity.ACOutdoorUnitBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ACOutdoorUnitBlockModel extends GeoModel<ACOutdoorUnitBlockEntity> {

    @Override
    public ResourceLocation getModelResource(ACOutdoorUnitBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "geo/ac_outdoor_unit.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ACOutdoorUnitBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "textures/block/ac_outdoor_unit.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ACOutdoorUnitBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ModernFurniture.MODID, "animations/ac_outdoor_unit.animation.json");
    }
}
