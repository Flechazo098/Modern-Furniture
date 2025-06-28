package com.flechazo.modernfurniture.util.wire;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.network.NetworkHandler;
import com.flechazo.modernfurniture.network.modules.WireSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局管理所有连接关系和激活状态
 * 按维度分区，维护连接的激活状态及加载情况
 */
public class WireNetworkManager extends SavedData {
    private static final String DATA_NAME = ModernFurniture.MODID + "_wire_network";

    // 所有连接关系 (维度 -> 连接集合)
    private final Map<ResourceLocation, Set<WireConnection>> connections = new ConcurrentHashMap<>();

    // 当前激活的连接 (维度 -> 连接集合)
    private final Map<ResourceLocation, Set<WireConnection>> activeConnections = new ConcurrentHashMap<>();

    // 位置到连接的映射，用于快速查找 (维度 -> 位置 -> 连接集合)
    private final Map<ResourceLocation, Map<BlockPos, Set<WireConnection>>> positionIndex = new ConcurrentHashMap<>();

    public static WireNetworkManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WireNetworkManager::load,
                WireNetworkManager::new,
                DATA_NAME
        );
    }

    public static WireNetworkManager load(CompoundTag tag) {
        WireNetworkManager manager = new WireNetworkManager();

        ListTag dimensionsList = tag.getList("dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensionsList.size(); i++) {
            CompoundTag dimensionTag = dimensionsList.getCompound(i);
            ResourceLocation dimension = ResourceLocation.parse(dimensionTag.getString("dimension"));

            // 修复：使用线程安全的Set
            Set<WireConnection> dimConnections = ConcurrentHashMap.newKeySet();
            ListTag connectionsList = dimensionTag.getList("connections", Tag.TAG_COMPOUND);
            for (int j = 0; j < connectionsList.size(); j++) {
                WireConnection connection = WireConnection.load(connectionsList.getCompound(j));
                dimConnections.add(connection);
                manager.updatePositionIndex(dimension, connection, true);
            }

            manager.connections.put(dimension, dimConnections);
        }

        return manager;
    }

    /**
     * 添加连接
     */
    public boolean addConnection(Level level, BlockPos pos1, BlockPos pos2) {
        if (level.isClientSide) return false;

        ResourceLocation dimension = level.dimension().location();
        WireConnection connection = new WireConnection(pos1, pos2, dimension);

        // 检查连接是否已存在
        // 修复：使用线程安全的Set
        Set<WireConnection> dimConnections = connections.computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet());
        if (dimConnections.contains(connection)) {
            return false;
        }

        // 验证连接的有效性
        if (!validateConnection(level, pos1, pos2)) {
            return false;
        }

        // 添加连接
        dimConnections.add(connection);

        // 更新位置索引
        updatePositionIndex(dimension, connection, true);

        // 尝试激活连接
        tryActivateConnection(level, connection);

        // 同步到所有在线玩家
        if (level instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                if (isInSameDimension(serverLevel, player)) {
                    syncToClient(serverLevel, player);
                }
            }
        }

        setDirty();
        ModernFurniture.LOGGER.info("Added wire connection: {}", connection);
        return true;
    }

    public static boolean isInSameDimension(Level level, ServerPlayer player) {
        return player.level().dimension().equals(level.dimension());
    }

    /**
     * 移除连接
     */
    public boolean removeConnection(Level level, BlockPos pos1, BlockPos pos2) {
        if (level.isClientSide) return false;

        ResourceLocation dimension = level.dimension().location();
        WireConnection connection = new WireConnection(pos1, pos2, dimension);

        Set<WireConnection> dimConnections = connections.get(dimension);
        if (dimConnections == null || !dimConnections.contains(connection)) {
            return false;
        }

        // 停用连接
        deactivateConnection(level, connection);

        // 移除连接
        dimConnections.remove(connection);

        // 更新位置索引
        updatePositionIndex(dimension, connection, false);

        setDirty();
        ModernFurniture.LOGGER.info("Removed wire connection: {}", connection);
        return true;
    }

    /**
     * 移除指定位置的所有连接
     */
    public void removeAllConnections(Level level, BlockPos pos) {
        if (level.isClientSide) return;

        ResourceLocation dimension = level.dimension().location();
        Set<WireConnection> posConnections = getConnectionsAt(dimension, pos);

        for (WireConnection connection : Set.copyOf(posConnections)) {
            BlockPos otherPos = connection.getOtherEnd(pos);
            if (otherPos != null) {
                removeConnection(level, pos, otherPos);
            }
        }
    }

    /**
     * 尝试激活连接
     */
    public void tryActivateConnection(Level level, WireConnection connection) {
        if (level.isClientSide) return;

        ResourceLocation dimension = level.dimension().location();

        // 检查两端区块是否都已加载
        if (!level.isLoaded(connection.pos1()) || !level.isLoaded(connection.pos2())) {
            return;
        }

        // 检查两端设备是否存在且可连接
        BlockEntity be1 = level.getBlockEntity(connection.pos1());
        BlockEntity be2 = level.getBlockEntity(connection.pos2());

        if (!(be1 instanceof WireConnectable device1) || !(be2 instanceof WireConnectable device2)) {
            return;
        }

        // 检查连接类型兼容性
        if (!device1.getConnectionType().canConnectTo(device2.getConnectionType())) {
            return;
        }

        // 激活连接
        Set<WireConnection> activeSet = activeConnections.computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet());
        if (activeSet.add(connection)) {
            device1.onConnectionActivated(level, connection.pos1(), connection.pos2());
            device2.onConnectionActivated(level, connection.pos2(), connection.pos1());
            ModernFurniture.LOGGER.debug("Activated wire connection: {}", connection);
        }
    }

    /**
     * 停用连接
     */
    public void deactivateConnection(Level level, WireConnection connection) {
        if (level.isClientSide) return;

        ResourceLocation dimension = level.dimension().location();
        Set<WireConnection> activeSet = activeConnections.get(dimension);

        if (activeSet != null && activeSet.remove(connection)) {
            // 通知设备连接已停用
            BlockEntity be1 = level.getBlockEntity(connection.pos1());
            BlockEntity be2 = level.getBlockEntity(connection.pos2());

            if (be1 instanceof WireConnectable) {
                ((WireConnectable) be1).onConnectionDeactivated(level, connection.pos1(), connection.pos2());
            }
            if (be2 instanceof WireConnectable) {
                ((WireConnectable) be2).onConnectionDeactivated(level, connection.pos2(), connection.pos1());
            }

            ModernFurniture.LOGGER.debug("Deactivated wire connection: {}", connection);
        }
    }

    /**
     * 当区块加载时调用
     */
    public void onChunkLoaded(Level level, BlockPos chunkPos) {
        if (level.isClientSide) return;

        ResourceLocation dimension = level.dimension().location();
        Set<WireConnection> dimConnections = connections.get(dimension);
        if (dimConnections == null) return;

        // 查找涉及该区块的连接
        for (WireConnection connection : dimConnections) {
            if (isInChunk(connection.pos1(), chunkPos) || isInChunk(connection.pos2(), chunkPos)) {
                tryActivateConnection(level, connection);
            }
        }
    }

    /**
     * 当区块卸载时调用
     */
    public void onChunkUnloaded(Level level, BlockPos chunkPos) {
        if (level.isClientSide) return;

        ResourceLocation dimension = level.dimension().location();
        Set<WireConnection> activeSet = activeConnections.get(dimension);
        if (activeSet == null) return;

        // 停用涉及该区块的连接
        for (WireConnection connection : new HashSet<>(activeSet)) {
            if (isInChunk(connection.pos1(), chunkPos) || isInChunk(connection.pos2(), chunkPos)) {
                deactivateConnection(level, connection);
            }
        }
    }

    /**
     * 获取指定位置的所有连接
     */
    public Set<WireConnection> getConnectionsAt(ResourceLocation dimension, BlockPos pos) {
        Map<BlockPos, Set<WireConnection>> dimIndex = positionIndex.get(dimension);
        if (dimIndex == null) return Collections.emptySet();

        Set<WireConnection> result = dimIndex.get(pos);
        return result != null ? new HashSet<>(result) : Collections.emptySet();
    }

    /**
     * 检查连接是否激活
     */
    public boolean isConnectionActive(ResourceLocation dimension, WireConnection connection) {
        Set<WireConnection> activeSet = activeConnections.get(dimension);
        return activeSet != null && activeSet.contains(connection);
    }


    /**
     * 获取所有连接（用于渲染）
     */
    public Set<WireConnection> getAllConnections(ResourceLocation dimension) {
        Set<WireConnection> dimConnections = connections.get(dimension);
        return dimConnections != null ? new HashSet<>(dimConnections) : Collections.emptySet();
    }

    private boolean validateConnection(Level level, BlockPos pos1, BlockPos pos2) {
        BlockEntity be1 = level.getBlockEntity(pos1);
        BlockEntity be2 = level.getBlockEntity(pos2);

        if (!(be1 instanceof WireConnectable device1) || !(be2 instanceof WireConnectable device2)) {
            return false;
        }

        return device1.canConnectTo(level, pos1, pos2) &&
                device2.canConnectTo(level, pos2, pos1) &&
                device1.getConnectionType().canConnectTo(device2.getConnectionType());
    }

    private void updatePositionIndex(ResourceLocation dimension, WireConnection connection, boolean add) {
        Map<BlockPos, Set<WireConnection>> dimIndex = positionIndex.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());

        if (add) {
            dimIndex.computeIfAbsent(connection.pos1(), k -> ConcurrentHashMap.newKeySet()).add(connection);
            dimIndex.computeIfAbsent(connection.pos2(), k -> ConcurrentHashMap.newKeySet()).add(connection);
        } else {
            Set<WireConnection> set1 = dimIndex.get(connection.pos1());
            if (set1 != null) {
                set1.remove(connection);
                if (set1.isEmpty()) dimIndex.remove(connection.pos1());
            }

            Set<WireConnection> set2 = dimIndex.get(connection.pos2());
            if (set2 != null) {
                set2.remove(connection);
                if (set2.isEmpty()) dimIndex.remove(connection.pos2());
            }
        }
    }

    // 数据持久化

    private boolean isInChunk(BlockPos blockPos, BlockPos chunkPos) {
        int chunkX = chunkPos.getX() >> 4;
        int chunkZ = chunkPos.getZ() >> 4;
        int blockChunkX = blockPos.getX() >> 4;
        int blockChunkZ = blockPos.getZ() >> 4;
        return chunkX == blockChunkX && chunkZ == blockChunkZ;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag dimensionsList = new ListTag();

        for (Map.Entry<ResourceLocation, Set<WireConnection>> entry : connections.entrySet()) {
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("dimension", entry.getKey().toString());

            ListTag connectionsList = new ListTag();
            for (WireConnection connection : entry.getValue()) {
                connectionsList.add(connection.save());
            }
            dimensionTag.put("connections", connectionsList);

            dimensionsList.add(dimensionTag);
        }

        tag.put("dimensions", dimensionsList);
        return tag;
    }

    /**
     * 同步连接到客户端
     */
    public void syncToClient(ServerLevel level, Player player) {
        ResourceLocation dimension = level.dimension().location();
        Set<WireConnection> connections = getAllConnections(dimension);

        NetworkHandler.NETWORK.sendToClient(
                new WireSyncPacket(dimension, connections),
                (ServerPlayer) player
        );
    }

    /**
     * 当玩家加入世界时同步所有连接
     */
    public void syncAllToClient(ServerLevel level, Player player) {
        for (Map.Entry<ResourceLocation, Set<WireConnection>> entry : connections.entrySet()) {
            NetworkHandler.NETWORK.sendToClient(
                    new WireSyncPacket(entry.getKey(), entry.getValue()),
                    (ServerPlayer) player
            );
        }
    }
}