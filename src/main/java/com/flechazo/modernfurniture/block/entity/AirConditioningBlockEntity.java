package com.flechazo.modernfurniture.block.entity;

import com.flechazo.modernfurniture.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
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

public class AirConditioningBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // 添加动画状态跟踪
    private boolean isAnimating = false;
    private String currentAnimation = "";

    // 动画定义
    private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin().thenPlayAndHold("open");
    private static final RawAnimation CLOSE_ANIMATION = RawAnimation.begin().thenPlayAndHold("close");

    public AirConditioningBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIR_CONDITIONING_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "ac_controller", 0, this::predicate)
            .triggerableAnim("open", OPEN_ANIMATION)
            .triggerableAnim("close", CLOSE_ANIMATION));
    }

    private PlayState predicate(AnimationState<AirConditioningBlockEntity> animationState) {
        if (isAnimating) {
            // 如果正在播放触发动画，让其继续播放
            if (animationState.isCurrentAnimation(OPEN_ANIMATION) ||
                    animationState.isCurrentAnimation(CLOSE_ANIMATION)) {
                return PlayState.CONTINUE;
            } else {
                // 动画完成，重置状态
                isAnimating = false;
                currentAnimation = "";
                return PlayState.STOP;
            }
        }
        return PlayState.STOP;
    }

    public void triggerAnimation(boolean opening) {
        if (this.level instanceof ServerLevel) {
            // 防止重复触发相同动画
            String targetAnimation = opening ? "open" : "close";
            if (isAnimating && currentAnimation.equals(targetAnimation)) {
                return;
            }
            
            isAnimating = true;
            currentAnimation = targetAnimation;
            
            if (opening) {
                triggerAnim("ac_controller", "open");
            } else {
                triggerAnim("ac_controller", "close");
            }
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}