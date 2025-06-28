package com.flechazo.modernfurniture.network;

import com.flechazo.modernfurniture.network.modules.ConfigPacket;
import com.flechazo.modernfurniture.network.modules.WireSyncPacket;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class NetworkHandler {
    public static final PacketHandler NETWORK = new PacketHandler("1.0.0") {
        @Override
        public void registerPackets() {
            registerPacket(ConfigPacket.class, ConfigPacket::encode, buf -> {
                        ConfigPacket packet = new ConfigPacket();
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

    public static void register(IEventBus bus) {
        bus.addListener(NetworkHandler::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        NETWORK.registerPackets();
    }
}
