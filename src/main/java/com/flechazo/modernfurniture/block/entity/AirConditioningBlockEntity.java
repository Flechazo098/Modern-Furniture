package com.flechazo.modernfurniture.block.entity;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.AirConditioningBlock;
import com.flechazo.modernfurniture.config.modules.SnowGenerationConfig;
import com.flechazo.modernfurniture.init.ModBlockEntities;
import com.flechazo.modernfurniture.util.RoomDetector;
import com.flechazo.modernfurniture.util.snow.SnowManager;
import com.flechazo.modernfurniture.util.snow.SnowStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AirConditioningBlockEntity extends AbstractAnimatableBlockEntity {
    private static final long PERFORMANCE_LOG_INTERVAL = 60000;
    private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin().thenPlayAndHold("open");
    private static final RawAnimation OPENED_ANIMATION = RawAnimation.begin().thenPlayAndHold("opened");
    private static final RawAnimation CLOSE_ANIMATION = RawAnimation.begin().thenPlayAndHold("close");

    private CompletableFuture<Void> roomDetectionFuture;
    private boolean isCooling = false;
    private long coolingStartTime = 0;
    private Set<BlockPos> roomBlocks = null;
    private SnowManager snowManager = null;
    private long lastPerformanceLog = 0;

    public AirConditioningBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIR_CONDITIONING_BLOCK_ENTITY.get(), pos, state);
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
        return "ac_controller";
    }

    @Override
    protected BooleanProperty getOpenProperty() {
        return AirConditioningBlock.OPEN;
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
    public void setRemoved() {
        super.setRemoved();

        if (roomDetectionFuture != null && !roomDetectionFuture.isDone()) {
            roomDetectionFuture.cancel(true);
        }

        if (snowManager != null) {
            try {
                snowManager.shutdown();
            } catch (Exception e) {
                // 忽略关闭异常
            }
            snowManager = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsCooling", isCooling);
        tag.putLong("CoolingStartTime", coolingStartTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isCooling = tag.getBoolean("IsCooling");
        coolingStartTime = tag.getLong("CoolingStartTime");

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
                    .exceptionally(throwable -> null);
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
                    ModernFurniture.LOGGER.warn("Failed to perform snow block entity", e);
                }
            }
        }
    }

    private void logPerformanceStats(long currentTime) {
        if (currentTime - lastPerformanceLog > PERFORMANCE_LOG_INTERVAL) {
            lastPerformanceLog = currentTime;
                SnowStats stats = getSnowStats();
                ModernFurniture.LOGGER.debug("降雪性能统计: {}", stats);
        }
    }

    // 调试方法
    public SnowStats getSnowStats() {
        return snowManager != null ? snowManager.getSnowStats() : null;
    }

    public void refreshSnowManager() {
        if (snowManager != null && roomBlocks != null && !roomBlocks.isEmpty()) {
            snowManager.shutdown();
            snowManager = new SnowManager((ServerLevel) this.level, roomBlocks);
        }
    }
}