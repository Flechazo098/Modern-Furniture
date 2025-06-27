package com.flechazo.modernfurniture.client.model;

import com.flechazo.modernfurniture.block.entity.WallMountedAirConditioningBlockEntity;

public class WallMountedAirConditioningBlockModel extends AbstractAirConditioningBlockModel<WallMountedAirConditioningBlockEntity> {

    @Override
    protected String getModelPrefix() {
        return "wall_mounted_air_conditioning";
    }

    @Override
    protected String getTexturePrefix() {
        return "wall_mounted_air_conditioning";
    }

    @Override
    protected String getAnimationPath() {
        return "wall_mounted_air_conditioning";
    }
}