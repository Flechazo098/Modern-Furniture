package com.flechazo.modernfurniture.network;

import com.flechazo.modernfurniture.network.module.ConfigPacket;
import com.flechazo.modernfurniture.network.module.WireSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class NetworkHandler {
    public static void register(IEventBus bus) {
        bus.addListener(NetworkHandler::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        NETWORK.registerPackets();
    }

    private static final PacketHandler NETWORK = new PacketHandler("1.0.0") {
        @Override
        public void registerPackets() {
            registerPacket(ConfigPacket.class, ConfigPacket::encode, buf -> {
                        ConfigPacket packet = new ConfigPacket(-1);
                        packet.decode(buf);
                        return packet;
                    },
                    ConfigPacket::handle);
            registerPacket(WireSyncPacket.class, WireSyncPacket::encode, buf -> {
                        WireSyncPacket packet = new WireSyncPacket();
                        packet.decode(buf);
                        return packet;
                    },
                    WireSyncPacket::handle);
        }
    };

    public static void sendToServer(PacketHandler.AbstractPacket packet) {
        NETWORK.sendToServer(packet);
    }

    public static void sendToClient(PacketHandler.AbstractPacket packet, ServerPlayer player) {
        NETWORK.sendToClient(packet, player);
    }


}
