package com.flechazo.modernfurniture.client.renderer;

import com.flechazo.modernfurniture.block.entity.WallMountedAirConditioningBlockEntity;
import com.flechazo.modernfurniture.client.model.WallMountedAirConditioningBlockModel;

public class WallMountedAirConditioningBlockRenderer extends AbstractAirConditioningBlockRenderer<WallMountedAirConditioningBlockEntity> {
    public WallMountedAirConditioningBlockRenderer() {
        super(new WallMountedAirConditioningBlockModel());
    }
}