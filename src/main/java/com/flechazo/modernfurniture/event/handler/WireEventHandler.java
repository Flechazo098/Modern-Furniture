package com.flechazo.modernfurniture.event.handler;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.util.wire.WireNetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WireEventHandler {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 延迟同步，避免在加载时阻塞
            player.getServer().execute(() -> {
                try {
                    ServerLevel level = (ServerLevel) player.level();
                    WireNetworkManager manager = WireNetworkManager.get(level);

                    // 只同步当前维度的连接，而不是所有维度
                    manager.syncToClient(level, player);
                } catch (Exception e) {
                    ModernFurniture.LOGGER.error("Failed to sync wire connections to player: {}", player.getName().getString(), e);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 延迟同步，确保维度切换完成
            player.getServer().execute(() -> {
                try {
                    ServerLevel level = (ServerLevel) player.level();
                    WireNetworkManager manager = WireNetworkManager.get(level);

                    // 同步当前维度的电线连接
                    manager.syncToClient(level, player);
                } catch (Exception e) {
                    ModernFurniture.LOGGER.error("Failed to sync wire connections during dimension change for player: {}", player.getName().getString(), e);
                }
            });
        }
    }
}