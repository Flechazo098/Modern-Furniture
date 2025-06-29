package com.flechazo.modernfurniture.event.handler;

import com.flechazo.modernfurniture.config.ConfigManager;
import com.flechazo.modernfurniture.config.module.GlobalConfig;
import com.flechazo.modernfurniture.network.NetworkHandler;
import com.flechazo.modernfurniture.network.module.ConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ConfigSyncEventHandler {
    @SubscribeEvent
    public static void syncData(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) {
            ConfigPacket packet = ConfigPacket.syncRequest(GlobalConfig.syncDataFromServer);
            NetworkHandler.sendToServer(packet);
        } else if (GlobalConfig.enforceServerConfigDataSync && event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.sendToClient(ConfigPacket.syncResponse(ConfigManager.createSyncData(false)), player);
        }
    }


    @SubscribeEvent
    public static void syncData(PlayerEvent.PlayerLoggedOutEvent event) {
        ConfigManager.load();
    }
}
