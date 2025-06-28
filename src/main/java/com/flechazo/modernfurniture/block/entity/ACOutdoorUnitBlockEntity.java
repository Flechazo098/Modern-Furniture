package com.flechazo.modernfurniture.block.entity;

import com.flechazo.modernfurniture.block.ACOutdoorUnitBlock;
import com.flechazo.modernfurniture.block.manager.BlockEntityManager;
import com.flechazo.modernfurniture.util.wire.WireConnectable;
import com.flechazo.modernfurniture.util.wire.WireConnection;
import com.flechazo.modernfurniture.util.wire.WireConnectionType;
import com.flechazo.modernfurniture.util.wire.WireNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.Set;

public class ACOutdoorUnitBlockEntity extends AbstractAnimatableBlockEntity implements WireConnectable {
    private static final RawAnimation RUNNING_ANIMATION = RawAnimation.begin().thenLoop("animation.model.new");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.model.idle");

    private boolean hasValidConnection = false;

    public ACOutdoorUnitBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityManager.AC_OUTDOOR_UNIT_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected RawAnimation getOpenAnimation() {
        return RUNNING_ANIMATION;
    }

    @Override
    protected RawAnimation getOpenedAnimation() {
        return RUNNING_ANIMATION;
    }

    @Override
    protected RawAnimation getCloseAnimation() {
        return IDLE_ANIMATION;
    }

    @Override
    protected String getControllerName() {
        return "fan_controller";
    }

    @Override
    protected BooleanProperty getOpenProperty() {
        return ACOutdoorUnitBlock.RUNNING;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            checkInitialConnection(serverLevel);
        }
    }

    private void checkInitialConnection(ServerLevel serverLevel) {
        WireNetworkManager manager = WireNetworkManager.get(serverLevel);
        Set<WireConnection> connections = manager.getConnectionsAt(serverLevel.dimension().location(), worldPosition);
        if (!connections.isEmpty()) {
            WireConnection connection = connections.iterator().next();
            if (manager.isConnectionActive(serverLevel.dimension().location(), connection)) {
                onConnectionActivated(serverLevel, worldPosition, connection.getOtherEnd(worldPosition));
            }
        }
    }

    public void startRunning() {
        if (hasValidConnection && !isOpen()) {
            triggerAnimation(true);
        }
    }

    public void stopRunning() {
        if (isOpen()) {
            triggerAnimation(false);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // 清理所有连接
        if (level instanceof ServerLevel serverLevel) {
            WireNetworkManager manager = WireNetworkManager.get(serverLevel);
            manager.removeAllConnections(level, worldPosition);
        }
    }

    @Override
    public void onConnectionActivated(Level level, BlockPos pos, BlockPos connectedPos) {
        hasValidConnection = true;
        setChanged();
        // 确保状态同步后再启动
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        startRunning();
    }

    @Override
    public void onConnectionDeactivated(Level level, BlockPos pos, BlockPos connectedPos) {
        hasValidConnection = false;
        // 连接断开时停止运行
        stopRunning();
        setChanged();
    }

    @Override
    public boolean canConnectTo(Level level, BlockPos pos, BlockPos targetPos) {
        BlockEntity targetEntity = level.getBlockEntity(targetPos);
        return targetEntity instanceof WireConnectable connectable &&
                connectable.getConnectionType() == WireConnectionType.AIR_CONDITIONING_INDOOR;
    }

    @Override
    public WireConnectionType getConnectionType() {
        return WireConnectionType.AIR_CONDITIONING_OUTDOOR;
    }

    @Override
    public boolean isWorking(Level level, BlockPos pos) {
        return isOpen() && hasValidConnection;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("HasValidConnection", hasValidConnection);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        hasValidConnection = tag.getBoolean("HasValidConnection");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("HasValidConnection", hasValidConnection);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        hasValidConnection = tag.getBoolean("HasValidConnection");
    }

    public boolean isRunning() {
        return isOpen() && hasValidConnection;
    }

    public boolean hasValidConnection() {
        return hasValidConnection;
    }
}