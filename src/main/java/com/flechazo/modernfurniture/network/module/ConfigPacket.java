package com.flechazo.modernfurniture.network.module;

import com.flechazo.modernfurniture.client.gui.ConfigScreen;
import com.flechazo.modernfurniture.config.ConfigManager;
import com.flechazo.modernfurniture.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigPacket extends PacketHandler.AbstractPacket {
    private final Map<String, Object> configData = new HashMap<>();
    private Type packetType;
    public ConfigPacket(Type type) {
        this.packetType = type;
    }

    public static ConfigPacket createForSync() {
        ConfigPacket packet = new ConfigPacket(Type.SYNC_TO_CLIENT);
        packet.configData.putAll(ConfigManager.getCurrentConfigValues());
        return packet;
    }

    public static ConfigPacket createForUpdate(Map<String, Object> serverConfig) {
        ConfigPacket packet = new ConfigPacket(Type.UPDATE_CONFIG);
        packet.configData.putAll(serverConfig);
        return packet;
    }

    public static ConfigPacket createForReset() {
        return new ConfigPacket(Type.RESET_TO_DEFAULTS);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(packetType.getId());
        buf.writeInt(configData.size());

        configData.forEach((key, value) -> {
            buf.writeUtf(key);
            encodeValue(buf, value);
        });
    }

    private void encodeValue(FriendlyByteBuf buf, Object value) {
        if (value instanceof Boolean) {
            buf.writeByte(0);
            buf.writeBoolean((Boolean) value);
        } else if (value instanceof Integer) {
            buf.writeByte(1);
            buf.writeInt((Integer) value);
        } else if (value instanceof Long) {
            buf.writeByte(2);
            buf.writeLong((Long) value);
        } else if (value instanceof Float) {
            buf.writeByte(3);
            buf.writeFloat((Float) value);
        } else if (value instanceof Double) {
            buf.writeByte(4);
            buf.writeDouble((Double) value);
        } else if (value instanceof String) {
            buf.writeByte(5);
            buf.writeUtf((String) value);
        } else {
            throw new IllegalArgumentException("Unsupported config value type: " + value.getClass());
        }
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        packetType = Type.fromId(buf.readInt());
        int size = buf.readInt();

        configData.clear();

        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            Object value = decodeValue(buf);
            configData.put(key, value);
        }
    }

    private Object decodeValue(FriendlyByteBuf buf) {
        byte typeId = buf.readByte();
        return switch (typeId) {
            case 0 -> buf.readBoolean();
            case 1 -> buf.readInt();
            case 2 -> buf.readLong();
            case 3 -> buf.readFloat();
            case 4 -> buf.readDouble();
            case 5 -> buf.readUtf();
            default -> throw new IllegalArgumentException("Unknown config type: " + typeId);
        };
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            switch (packetType) {
                case SYNC_TO_CLIENT -> handleClientSide();
                case UPDATE_CONFIG -> handleServerSide(context);
                case RESET_TO_DEFAULTS -> handleResetToDefaults(context);
            }
        });
        context.get().setPacketHandled(true);
    }

    private void handleServerSide(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) {
            return;
        }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("You don't have permission to update the config"));
            return;
        }

        if (!ConfigManager.isConfigLoaded()) {
            player.sendSystemMessage(Component.literal("Config has not been loaded yet, please try again later"));
            return;
        }

        try {
            ConfigManager.syncValue(configData, true);
            player.sendSystemMessage(Component.literal("Config has been reset to default and saved to file"));
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("Config reset failed: " + e.getMessage()));
        }
    }

    private void handleResetToDefaults(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) {
            return;
        }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("You don't have permission to reset the config"));
            return;
        }

        if (!ConfigManager.isConfigLoaded()) {
            player.sendSystemMessage(Component.literal("Config has not been loaded yet, please try again later"));
            return;
        }

        try {
            ConfigManager.resetToDefaults();
            player.sendSystemMessage(Component.literal("Config has been reset to default and saved to file"));
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("Config reset failed: " + e.getMessage()));
        }
    }

    private void handleClientSide() {
        Minecraft.getInstance().setScreen(new ConfigScreen(configData));
    }

    public enum Type {
        SYNC_TO_CLIENT(1),
        UPDATE_CONFIG(2),
        RESET_TO_DEFAULTS(3);

        private final int id;

        Type(int id) {
            this.id = id;
        }

        public static Type fromId(int id) {
            for (Type type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown packet type: " + id);
        }

        public int getId() {
            return id;
        }
    }
}