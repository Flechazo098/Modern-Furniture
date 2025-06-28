package com.flechazo.modernfurniture.block.entity;

import com.flechazo.modernfurniture.block.LaptopBlock;
import com.flechazo.modernfurniture.block.manager.BlockEntityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import software.bernie.geckolib.core.animation.RawAnimation;

public class LaptopBlockEntity extends AbstractAnimatableBlockEntity {
    private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin().thenPlayAndHold("laptop_open");
    private static final RawAnimation OPENED_ANIMATION = RawAnimation.begin().thenPlayAndHold("laptop_opened");
    private static final RawAnimation CLOSE_ANIMATION = RawAnimation.begin().thenPlayAndHold("laptop_close");

    public LaptopBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityManager.LAPTOP_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected RawAnimation getOpenAnimation() {
        return OPEN_ANIMATION;
    }

    @Override
    protected RawAnimation getOpenedAnimation() {
        return OPENED_ANIMATION;
    }

    @Override
    protected RawAnimation getCloseAnimation() {
        return CLOSE_ANIMATION;
    }

    @Override
    protected String getControllerName() {
        return "laptop_controller";
    }

    @Override
    protected BooleanProperty getOpenProperty() {
        return LaptopBlock.OPEN;
    }
}