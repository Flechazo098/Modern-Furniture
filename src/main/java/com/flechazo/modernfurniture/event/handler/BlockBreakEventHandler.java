package com.flechazo.modernfurniture.event.handler;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.util.wire.WireConnectable;
import com.flechazo.modernfurniture.util.wire.WireNetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BlockBreakEventHandler {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(event.getPos());

            // 检查是否是可连接的方块实体
            if (blockEntity instanceof WireConnectable) {
                try {
                    WireNetworkManager manager = WireNetworkManager.get(serverLevel);
                    manager.removeAllConnections(serverLevel, event.getPos());
                    ModernFurniture.LOGGER.debug("Removed wire connections for broken block at: {}", event.getPos());
                } catch (Exception e) {
                    ModernFurniture.LOGGER.error("Failed to remove wire connections for broken block at: {}", event.getPos(), e);
                }
            }
        }
    }
}