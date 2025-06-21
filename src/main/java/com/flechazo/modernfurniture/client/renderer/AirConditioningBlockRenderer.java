package com.flechazo.modernfurniture.client.renderer;

import com.flechazo.modernfurniture.block.entity.AirConditioningBlockEntity;
import com.flechazo.modernfurniture.client.model.AirConditioningBlockModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AirConditioningBlockRenderer extends GeoBlockRenderer<AirConditioningBlockEntity> {
    public AirConditioningBlockRenderer() {
        super(new AirConditioningBlockModel());
    }
}