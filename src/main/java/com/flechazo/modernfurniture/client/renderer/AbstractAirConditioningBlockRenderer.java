package com.flechazo.modernfurniture.client.renderer;

import com.flechazo.modernfurniture.block.entity.AbstractAirConditioningBlockEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public abstract class AbstractAirConditioningBlockRenderer<T extends AbstractAirConditioningBlockEntity> extends GeoBlockRenderer<T> {

    public AbstractAirConditioningBlockRenderer(GeoModel<T> model) {
        super(model);
    }
}