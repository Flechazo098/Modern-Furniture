package com.flechazo.modernfurniture.network.modules;

import com.flechazo.modernfurniture.config.ConfigManager;
import com.flechazo.modernfurniture.network.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigPacket extends PacketHandler.AbstractPacket {
    private final Map<String, Object> configData = new HashMap<>();

    public ConfigPacket() {
    }

    public static ConfigPacket createForSync() {
        ConfigPacket packet = new ConfigPacket();
        ConfigManager.map.forEach((configValue, field) -> {
            packet.configData.put(field.getName(), configValue.get());
        });
        return packet;
    }

    public static ConfigPacket createForUpdate(Map<String, Object> serverConfig) {
        ConfigPacket packet = new ConfigPacket();
        packet.configData.putAll(serverConfig);
        return packet;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(configData.size());

        configData.forEach((key, value) -> {
            buf.writeUtf(key);

            if (value instanceof Integer) {
                buf.writeByte(0); // 类型标记: int
                buf.writeInt((Integer) value);
            } else if (value instanceof Boolean) {
                buf.writeByte(1); // 类型标记: boolean
                buf.writeBoolean((Boolean) value);
            } else if (value instanceof Float) {
                buf.writeByte(2); // 类型标记: float
                buf.writeFloat((Float) value);
            } else if (value instanceof Double) {
                buf.writeByte(3); // 类型标记: double
                buf.writeDouble((Double) value);
            } else if (value instanceof String) {
                buf.writeByte(4); // 类型标记: string
                buf.writeUtf((String) value);
            } else {
                throw new IllegalArgumentException("Unsupported config type: " + value.getClass());
            }
        });
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        int size = buf.readInt();

        configData.clear();

        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            byte type = buf.readByte();

            switch (type) {
                case 0 -> configData.put(key, buf.readInt());
                case 1 -> configData.put(key, buf.readBoolean());
                case 2 -> configData.put(key, buf.readFloat());
                case 3 -> configData.put(key, buf.readDouble());
                case 4 -> configData.put(key, buf.readUtf());
                default -> throw new IllegalArgumentException("Unknown config type: " + type);
            }
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                handleServerSide();
            } else {
                handleClientSide();
            }
        });
        context.get().setPacketHandled(true);
    }

    private void handleServerSide() {
        System.out.println("Received client config: " + configData);
    }

    private void handleClientSide() {
        System.out.println("Received server config update: " + configData);
    }

    public Map<String, Object> getConfigData() {
        return new HashMap<>(configData);
    }
}
