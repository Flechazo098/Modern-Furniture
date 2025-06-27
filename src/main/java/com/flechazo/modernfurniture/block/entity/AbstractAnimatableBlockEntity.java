package com.flechazo.modernfurniture.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class AbstractAnimatableBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected boolean isOpen = false;
    protected boolean isAnimating = false;
    protected String currentAnimation = "";
    protected boolean needsAnimationSync = false;
    protected boolean init = true;

    public AbstractAnimatableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract RawAnimation getOpenAnimation();

    protected abstract RawAnimation getOpenedAnimation();

    protected abstract RawAnimation getCloseAnimation();

    protected abstract String getControllerName();

    protected abstract BooleanProperty getOpenProperty();

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, getControllerName(), 0, this::animationPredicate)
                .triggerableAnim("open", getOpenAnimation())
                .triggerableAnim("opened", getOpenedAnimation())
                .triggerableAnim("close", getCloseAnimation()));
    }

    private PlayState animationPredicate(AnimationState<AbstractAnimatableBlockEntity> animationState) {
        if (isAnimating) {
            if (animationState.isCurrentAnimation(getOpenAnimation()) ||
                    animationState.isCurrentAnimation(getCloseAnimation())) {
                return PlayState.CONTINUE;
            } else {
                isAnimating = false;
                currentAnimation = "";
                return PlayState.STOP;
            }
        }
        return PlayState.STOP;
    }

    public void triggerAnimation(boolean opening) {
        if (this.level instanceof ServerLevel) {
            String targetAnimation = opening ? "open" : "close";
            if (isAnimating && currentAnimation.equals(targetAnimation)) {
                return;
            }

            isOpen = opening;
            isAnimating = true;
            currentAnimation = targetAnimation;

            updateBlockState(opening);

            triggerAnim(getControllerName(), targetAnimation);

            setChanged();
        }
    }

    protected void updateBlockState(boolean opening) {
        BlockState currentState = level.getBlockState(worldPosition);
        BooleanProperty openProperty = getOpenProperty();
        if (currentState.hasProperty(openProperty)) {
            level.setBlock(worldPosition, currentState.setValue(openProperty, opening), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveAnimationData(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadAnimationData(tag);
        needsAnimationSync = true;
    }

    protected void saveAnimationData(CompoundTag tag) {
        tag.putBoolean("IsOpen", isOpen);
        tag.putBoolean("IsAnimating", isAnimating);
        tag.putString("CurrentAnimation", currentAnimation);
    }

    protected void loadAnimationData(CompoundTag tag) {
        isOpen = tag.getBoolean("IsOpen");
        isAnimating = tag.getBoolean("IsAnimating");
        currentAnimation = tag.getString("CurrentAnimation");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAnimationData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        loadAnimationData(tag);
        needsAnimationSync = true;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void clientTick() {
        if (this.needsAnimationSync) {
            this.needsAnimationSync = false;
            this.isAnimating = true;

            if (this.isOpen) {
                this.currentAnimation = "open";
                if (this.init) {
                    this.triggerAnim(getControllerName(), "opened");
                } else {
                    this.triggerAnim(getControllerName(), "open");
                }
            }

            if (this.init) this.init = false;
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public boolean isOpen() {
        return isOpen;
    }
}