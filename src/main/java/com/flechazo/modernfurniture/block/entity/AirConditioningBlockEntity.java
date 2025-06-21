package com.flechazo.modernfurniture.block.entity;

import com.flechazo.modernfurniture.block.AirConditioningBlock;
import com.flechazo.modernfurniture.init.ModBlockEntities;
import com.flechazo.modernfurniture.util.RoomDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private CompletableFuture<Void> roomDetectionFuture;

    private boolean isAnimating = false;
    private String currentAnimation = "";
    public boolean isOpen = false;
    private boolean needsAnimationSync = false;

    private boolean isCooling = false;
    private long coolingStartTime = 0;
    private Set<BlockPos> roomBlocks = null;
    private boolean hasSnowed = false;

    private static final long SNOW_DELAY_TICKS = 6000;

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
                this.hasSnowed = false;
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
    }

    public void stopCooling() {
        isCooling = false;
        coolingStartTime = 0;
        roomBlocks = null;
        hasSnowed = false;
        setChanged();
    }

    private void createSnowInRoom() {
        if (roomBlocks == null || roomBlocks.isEmpty()) {
            return;
        }

        for (BlockPos pos : roomBlocks) {
            BlockState currentState = this.level.getBlockState(pos);
            if (currentState.isAir()) {
                if (this.level.getRandom().nextFloat() < 0.1f) {
                    this.level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
                }
            }

            BlockPos belowPos = pos.below();
            BlockState belowState = this.level.getBlockState(belowPos);
            if (!belowState.isAir() && this.level.getBlockState(pos).isAir()) {
                if (this.level.getRandom().nextFloat() < 0.3f) {
                    this.level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
                }
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsCooling", isCooling);
        tag.putLong("CoolingStartTime", coolingStartTime);
        tag.putBoolean("HasSnowed", hasSnowed);
        tag.putBoolean("IsOpen", isOpen);
        tag.putBoolean("IsAnimating", isAnimating);
        tag.putString("CurrentAnimation", currentAnimation);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isCooling = tag.getBoolean("IsCooling");
        coolingStartTime = tag.getLong("CoolingStartTime");
        hasSnowed = tag.getBoolean("HasSnowed");
        isOpen = tag.getBoolean("IsOpen");
        isAnimating = tag.getBoolean("IsAnimating");
        currentAnimation = tag.getString("CurrentAnimation");

        // 标记需要同步动画
        needsAnimationSync = true;
    
        if (isCooling && this.level instanceof ServerLevel) {
            roomDetectionFuture = RoomDetector.findRoomAsync(this.level, this.worldPosition)
                    .thenAccept(blocks -> {
                        if (this.level != null && !this.isRemoved()) {
                            this.roomBlocks = blocks;
                            this.setChanged();
                        }
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
        
        // 在客户端触发动画同步
        needsAnimationSync = true;
    }

    public void clientTick () {
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
        if (this.level instanceof ServerLevel && this.isCooling && !this.hasSnowed) {
            long currentTime = this.level.getGameTime();
            long elapsedTime = currentTime - this.coolingStartTime;

            if (elapsedTime >= SNOW_DELAY_TICKS) {
                this.createSnowInRoom();
                this.hasSnowed = true;
                this.setChanged();
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
}