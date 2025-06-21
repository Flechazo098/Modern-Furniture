package com.flechazo.modernfurniture.client.renderer;

import com.flechazo.modernfurniture.block.DisplayBlockEntity;
import com.flechazo.modernfurniture.client.model.DisplayBlockModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class DisplayBlockRenderer extends GeoBlockRenderer<DisplayBlockEntity> {
    public DisplayBlockRenderer() {
        super(new DisplayBlockModel());
    }
}