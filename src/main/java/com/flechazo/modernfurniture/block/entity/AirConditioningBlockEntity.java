package com.flechazo.modernfurniture.block.entity;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.AirConditioningBlock;
import com.flechazo.modernfurniture.config.modules.SnowGenerationConfig;
import com.flechazo.modernfurniture.init.ModBlockEntities;
import com.flechazo.modernfurniture.util.RoomDetector;
import com.flechazo.modernfurniture.util.SnowManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AirConditioningBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final long PERFORMANCE_LOG_INTERVAL = 60000;
    private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin().thenPlayAndHold("open");
    private static final RawAnimation CLOSE_ANIMATION = RawAnimation.begin().thenPlayAndHold("close");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public boolean isOpen = false;
    private CompletableFuture<Void> roomDetectionFuture;
    private boolean isAnimating = false;
    private String currentAnimation = "";
    private boolean needsAnimationSync = false;
    private boolean isCooling = false;
    private long coolingStartTime = 0;
    private Set<BlockPos> roomBlocks = null;
    private SnowManager snowManager = null;
    private long lastPerformanceLog = 0;

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

            BlockState currentState = level.getBlockState(worldPosition);
            if (currentState.hasProperty(AirConditioningBlock.OPEN)) {
                level.setBlock(worldPosition, currentState.setValue(AirConditioningBlock.OPEN, opening), 3);
            }

            if (opening) {
                triggerAnim("ac_controller", "open");
            } else {
                triggerAnim("ac_controller", "close");
            }

            setChanged();
        }
    }

    public void startCooling() {
        if (this.level instanceof ServerLevel) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            Direction facing = state.getValue(AirConditioningBlock.FACING);

            BlockPos startPos = this.worldPosition.relative(facing);
            if (!RoomDetector.isPassable(this.level, startPos)) {
                startPos = this.worldPosition.above();
            }

            Set<BlockPos> detectedRoom = RoomDetector.findRoom(this.level, startPos);

            if (detectedRoom != null && !detectedRoom.isEmpty()) {
                this.roomBlocks = detectedRoom;
                this.isCooling = true;
                this.coolingStartTime = this.level.getGameTime();

                if (SnowGenerationConfig.enableSnow) {
                    this.snowManager = new SnowManager((ServerLevel) this.level, detectedRoom);
                }

                this.setChanged();
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (roomDetectionFuture != null && !roomDetectionFuture.isDone()) {
            roomDetectionFuture.cancel(true);
        }

        if (snowManager != null) {
            try {
                snowManager.shutdown();
            } catch (Exception e) {
            }
            snowManager = null;
        }
    }

    public void stopCooling() {
        isCooling = false;
        coolingStartTime = 0;
        roomBlocks = null;

        if (snowManager != null) {
            try {
                snowManager.shutdown();
            } catch (Exception e) {
                if (level instanceof ServerLevel) {
                    ModernFurniture.LOGGER.warn("Failed to stop room blocks snow manager", e);
                }
            }
            snowManager = null;
        }

        setChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsCooling", isCooling);
        tag.putLong("CoolingStartTime", coolingStartTime);
        tag.putBoolean("IsOpen", isOpen);
        tag.putBoolean("IsAnimating", isAnimating);
        tag.putString("CurrentAnimation", currentAnimation);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isCooling = tag.getBoolean("IsCooling");
        coolingStartTime = tag.getLong("CoolingStartTime");
        isOpen = tag.getBoolean("IsOpen");
        isAnimating = tag.getBoolean("IsAnimating");
        currentAnimation = tag.getString("CurrentAnimation");

        needsAnimationSync = true;

        if (isCooling && this.level instanceof ServerLevel) {
            roomDetectionFuture = RoomDetector.findRoomAsync(this.level, this.worldPosition)
                    .thenAccept(blocks -> {
                        if (this.level != null && !this.isRemoved()) {
                            this.roomBlocks = blocks;

                            if (SnowGenerationConfig.enableSnow && blocks != null && !blocks.isEmpty()) {
                                this.snowManager = new SnowManager((ServerLevel) this.level, blocks);
                            }

                            this.setChanged();
                        }
                    })
                    .exceptionally(throwable -> {
                        return null;
                    });
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
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
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
                this.triggerAnim("ac_controller", "open");
            }
        }
    }

    public void serverTick() {
        if (this.level instanceof ServerLevel && this.isCooling && SnowGenerationConfig.enableSnow) {
            if (snowManager != null) {
                long currentTime = this.level.getGameTime();

                try {
                    if (snowManager.performSnowingAsync(currentTime)) {
                        this.setChanged();
                    }

                    logPerformanceStats(currentTime);

                } catch (Exception e) {
                    // 降雪处理异常，记录但不影响主要功能
                    // 可以考虑重置降雪管理器或采取其他恢复措施
                }
            }
        }
    }

    private void logPerformanceStats(long currentTime) {
        if (currentTime - lastPerformanceLog > PERFORMANCE_LOG_INTERVAL) {
            lastPerformanceLog = currentTime;

            if (snowManager != null) {
                SnowManager.SnowStats stats = snowManager.getSnowStats();
                ModernFurniture.LOGGER.debug("降雪性能统计: {}", stats);
            }
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public boolean isOpen() {
        return isOpen;
    }

    /**
     * 获取降雪统计信息（用于调试和监控）
     */
    public SnowManager.SnowStats getSnowStats() {
        return snowManager != null ? snowManager.getSnowStats() : null;
    }

    /**
     * 强制刷新降雪管理器（用于调试）
     */
    public void refreshSnowManager() {
        if (snowManager != null && roomBlocks != null && !roomBlocks.isEmpty()) {
            snowManager.shutdown();
            snowManager = new SnowManager((ServerLevel) this.level, roomBlocks);
        }
    }
}