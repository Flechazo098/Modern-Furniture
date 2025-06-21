package com.flechazo.modernfurniture.block.entity;

import com.flechazo.modernfurniture.block.LaptopBlock;
import com.flechazo.modernfurniture.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LaptopBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean isAnimating = false;
    private String currentAnimation = "";
    private boolean isOpen = false;
    private boolean needsAnimationSync = false;

    private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin().thenPlayAndHold("laptop_open");
    private static final RawAnimation CLOSE_ANIMATION = RawAnimation.begin().thenPlayAndHold("laptop_close");

    public LaptopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAPTOP_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "laptop_controller", 0, this::predicate)
            .triggerableAnim("open", OPEN_ANIMATION)
            .triggerableAnim("close", CLOSE_ANIMATION));
    }

    private PlayState predicate(AnimationState<LaptopBlockEntity> animationState) {
        if (isAnimating) {
            if (animationState.isCurrentAnimation(OPEN_ANIMATION) ||
                    animationState.isCurrentAnimation(CLOSE_ANIMATION)) {
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

            // 同步更新 BlockState
            BlockState currentState = level.getBlockState(worldPosition);
            if (currentState.hasProperty(LaptopBlock.OPEN)) {
                level.setBlock(worldPosition, currentState.setValue(LaptopBlock.OPEN, opening), 3);
            }

            if (opening) {
                triggerAnim("laptop_controller", "open");
            } else {
                triggerAnim("laptop_controller", "close");
            }

            setChanged();
        }
    }
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("IsOpen", isOpen);
        tag.putBoolean("IsAnimating", isAnimating);
        tag.putString("CurrentAnimation", currentAnimation);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        isOpen = tag.getBoolean("IsOpen");
        isAnimating = tag.getBoolean("IsAnimating");
        currentAnimation = tag.getString("CurrentAnimation");

        // 在客户端触发动画同步
        needsAnimationSync = true;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional (CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putBoolean("IsOpen", isOpen);
        tag.putBoolean("IsAnimating", isAnimating);
        tag.putString("CurrentAnimation", currentAnimation);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        isOpen = tag.getBoolean("IsOpen");
        isAnimating = tag.getBoolean("IsAnimating");
        currentAnimation = tag.getString("CurrentAnimation");

        needsAnimationSync = true;
    }

    public void clientTick() {
        if (this.needsAnimationSync) {
            this.needsAnimationSync = false;
            this.isAnimating = true;

            if (this.isOpen) {
                this.currentAnimation = "open";
                this.triggerAnim("laptop_controller", "open");
            }
        }
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}