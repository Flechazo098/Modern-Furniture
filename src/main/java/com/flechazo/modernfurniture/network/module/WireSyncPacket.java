package com.flechazo.modernfurniture.network.module;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.client.ClientWireManager;
import com.flechazo.modernfurniture.network.PacketHandler;
import com.flechazo.modernfurniture.util.wire.WireConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class WireSyncPacket extends PacketHandler.AbstractPacket {
    private ResourceLocation dimension;
    private Set<WireConnection> connections;

    public WireSyncPacket() {
        this.connections = new HashSet<>();
    }

    public WireSyncPacket(ResourceLocation dimension, Set<WireConnection> connections) {
        this.dimension = dimension;
        this.connections = connections;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeInt(connections.size());

        for (WireConnection connection : connections) {
            buffer.writeBlockPos(connection.pos1());
            buffer.writeBlockPos(connection.pos2());
            buffer.writeResourceLocation(connection.dimension());
        }
    }

    @Override
    public void decode(FriendlyByteBuf buffer) {
        dimension = buffer.readResourceLocation();
        int size = buffer.readInt();

        connections = new HashSet<>();
        for (int i = 0; i < size; i++) {
            BlockPos pos1 = buffer.readBlockPos();
            BlockPos pos2 = buffer.readBlockPos();
            ResourceLocation connDim = buffer.readResourceLocation();
            connections.add(new WireConnection(pos1, pos2, connDim));
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                if (context.getDirection().getReceptionSide().isClient()) {
                    ClientWireManager.updateConnections(dimension, connections);
                }
            } catch (Exception e) {
                ModernFurniture.LOGGER.error("Failed to handle wire sync packet: {}", e.getMessage());
            }
        });
        context.setPacketHandled(true);
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    public Set<WireConnection> getConnections() {
        return connections;
    }
}
