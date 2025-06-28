package com.flechazo.modernfurniture.client.renderer;

import com.flechazo.modernfurniture.block.entity.ACOutdoorUnitBlockEntity;
import com.flechazo.modernfurniture.client.model.ACOutdoorUnitBlockModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ACOutdoorUnitBlockRenderer extends GeoBlockRenderer<ACOutdoorUnitBlockEntity> {

    public ACOutdoorUnitBlockRenderer() {
        super(new ACOutdoorUnitBlockModel());
    }
}
