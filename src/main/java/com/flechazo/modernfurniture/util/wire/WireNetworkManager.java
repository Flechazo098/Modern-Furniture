package com.flechazo.modernfurniture.util.wire;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.network.NetworkHandler;
import com.flechazo.modernfurniture.network.module.WireSyncPacket;
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

    // 各维度所有连接（未激活和激活）
    private final Map<ResourceLocation, Set<WireConnection>> connections = new ConcurrentHashMap<>();
    // 各维度当前激活的连接
    private final Map<ResourceLocation, Set<WireConnection>> activeConnections = new ConcurrentHashMap<>();
    // 各维度中每个位置对应的连接索引
    private final Map<ResourceLocation, Map<BlockPos, Set<WireConnection>>> positionIndex = new ConcurrentHashMap<>();

    // 获取当前维度的连接数据实例
    public static WireNetworkManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WireNetworkManager::load, WireNetworkManager::new, DATA_NAME);
    }

    // 从NBT加载数据
    public static WireNetworkManager load(CompoundTag tag) {
        WireNetworkManager manager = new WireNetworkManager();
        ModernFurniture.LOGGER.info("Loading WireNetworkManager from NBT, tag size: {}", tag.size());

        for (Tag baseTag : tag.getList("dimensions", Tag.TAG_COMPOUND)) {
            CompoundTag dimensionTag = (CompoundTag) baseTag;
            ResourceLocation dimension = ResourceLocation.parse(dimensionTag.getString("dimension"));
            Set<WireConnection> dimConnections = ConcurrentHashMap.newKeySet();
            Set<WireConnection> activeSet = ConcurrentHashMap.newKeySet();

            loadConnectionsList(dimensionTag.getList("connections", Tag.TAG_COMPOUND), dimConnections, manager, dimension);
            loadConnectionsList(dimensionTag.getList("activeConnections", Tag.TAG_COMPOUND), activeSet, null, null);

            manager.connections.put(dimension, dimConnections);
            manager.activeConnections.put(dimension, activeSet);
        }

        ModernFurniture.LOGGER.info("WireNetworkManager loaded successfully");
        return manager;
    }

    // 批量加载连接数据
    private static void loadConnectionsList(ListTag list, Set<WireConnection> targetSet, WireNetworkManager manager, ResourceLocation dimension) {
        for (int j = 0; j < list.size(); j++) {
            WireConnection connection = WireConnection.load(list.getCompound(j));
            targetSet.add(connection);
            if (manager != null && dimension != null) {
                manager.updatePositionIndex(dimension, connection, true);
            }
        }
    }

    // 判断玩家是否在同一维度
    public static boolean isInSameDimension(Level level, ServerPlayer player) {
        return player.level().dimension().equals(level.dimension());
    }

    // 添加连接
    public boolean addConnection(Level level, BlockPos pos1, BlockPos pos2) {
        if (level.isClientSide) return false;

        ResourceLocation dimension = level.dimension().location();
        WireConnection connection = new WireConnection(pos1, pos2, dimension);
        Set<WireConnection> dimConnections = connections.computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet());

        if (!dimConnections.add(connection) || !validateConnection(level, pos1, pos2)) return false;

        updatePositionIndex(dimension, connection, true);
        tryActivateConnection(level, connection);
        syncAllToDimension(level);
        markDirty(level);

        ModernFurniture.LOGGER.info("Added wire connection: {}", connection);
        return true;
    }

    // 移除连接
    public boolean removeConnection(Level level, BlockPos pos1, BlockPos pos2) {
        if (level.isClientSide) return false;

        ResourceLocation dimension = level.dimension().location();
        WireConnection connection = new WireConnection(pos1, pos2, dimension);
        Set<WireConnection> dimConnections = connections.get(dimension);
        if (dimConnections == null || !dimConnections.remove(connection)) return false;

        deactivateConnection(level, connection);
        updatePositionIndex(dimension, connection, false);
        syncAllToDimension(level);
        markDirty(level);

        ModernFurniture.LOGGER.info("Removed wire connection: {}", connection);
        return true;
    }

    // 移除指定位置的所有连接
    public void removeAllConnections(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        ResourceLocation dimension = level.dimension().location();
        getConnectionsAt(dimension, pos).forEach(conn -> removeConnection(level, pos, conn.getOtherEnd(pos)));
    }

    // 尝试激活连接
    public void tryActivateConnection(Level level, WireConnection connection) {
        if (level.isClientSide || !level.isLoaded(connection.pos1()) || !level.isLoaded(connection.pos2())) return;

        BlockEntity be1 = level.getBlockEntity(connection.pos1());
        BlockEntity be2 = level.getBlockEntity(connection.pos2());

        if (!(be1 instanceof WireConnectable d1) || !(be2 instanceof WireConnectable d2)) return;
        if (!d1.getConnectionType().canConnectTo(d2.getConnectionType())) return;

        Set<WireConnection> activeSet = activeConnections.computeIfAbsent(connection.dimension(), k -> ConcurrentHashMap.newKeySet());
        if (activeSet.add(connection)) {
            d1.onConnectionActivated(level, connection.pos1(), connection.pos2());
            d2.onConnectionActivated(level, connection.pos2(), connection.pos1());
            ModernFurniture.LOGGER.debug("Activated wire connection: {}", connection);
        }
    }

    // 停用连接
    public void deactivateConnection(Level level, WireConnection connection) {
        if (level.isClientSide) return;
        Set<WireConnection> activeSet = activeConnections.get(connection.dimension());
        if (activeSet != null && activeSet.remove(connection)) {
            deactivateNotify(level, connection);
            ModernFurniture.LOGGER.debug("Deactivated wire connection: {}", connection);
        }
    }

    // 通知设备断开连接
    private void deactivateNotify(Level level, WireConnection conn) {
        BlockEntity be1 = level.getBlockEntity(conn.pos1());
        BlockEntity be2 = level.getBlockEntity(conn.pos2());
        if (be1 instanceof WireConnectable d1) d1.onConnectionDeactivated(level, conn.pos1(), conn.pos2());
        if (be2 instanceof WireConnectable d2) d2.onConnectionDeactivated(level, conn.pos2(), conn.pos1());
    }

    // 区块加载时尝试激活相关连接
    public void onChunkLoaded(Level level, BlockPos chunkPos) {
        if (level.isClientSide) return;
        Set<WireConnection> dimConnections = connections.get(level.dimension().location());
        if (dimConnections != null) {
            dimConnections.stream()
                    .filter(c -> isInChunk(c.pos1(), chunkPos) || isInChunk(c.pos2(), chunkPos))
                    .forEach(c -> tryActivateConnection(level, c));
        }
    }

    // 区块卸载时停用相关连接
    public void onChunkUnloaded(Level level, BlockPos chunkPos) {
        if (level.isClientSide) return;
        Set<WireConnection> activeSet = activeConnections.get(level.dimension().location());
        if (activeSet != null) {
            activeSet.stream()
                    .filter(c -> isInChunk(c.pos1(), chunkPos) || isInChunk(c.pos2(), chunkPos))
                    .forEach(c -> deactivateConnection(level, c));
        }
    }

    // 获取指定位置的所有连接
    public Set<WireConnection> getConnectionsAt(ResourceLocation dimension, BlockPos pos) {
        return new HashSet<>(positionIndex.getOrDefault(dimension, Map.of()).getOrDefault(pos, Set.of()));
    }

    // 判断连接是否激活
    public boolean isConnectionActive(ResourceLocation dimension, WireConnection connection) {
        return activeConnections.getOrDefault(dimension, Set.of()).contains(connection);
    }

    // 获取所有连接
    public Set<WireConnection> getAllConnections(ResourceLocation dimension) {
        return new HashSet<>(connections.getOrDefault(dimension, Set.of()));
    }

    // 校验连接是否合法
    private boolean validateConnection(Level level, BlockPos pos1, BlockPos pos2) {
        BlockEntity be1 = level.getBlockEntity(pos1);
        BlockEntity be2 = level.getBlockEntity(pos2);
        return be1 instanceof WireConnectable d1 && be2 instanceof WireConnectable d2 &&
                d1.canConnectTo(level, pos1, pos2) &&
                d2.canConnectTo(level, pos2, pos1) &&
                d1.getConnectionType().canConnectTo(d2.getConnectionType());
    }

    // 更新连接在位置索引中的信息
    private void updatePositionIndex(ResourceLocation dimension, WireConnection conn, boolean add) {
        Map<BlockPos, Set<WireConnection>> dimIndex = positionIndex.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());
        updateSingleIndex(dimIndex, conn.pos1(), conn, add);
        updateSingleIndex(dimIndex, conn.pos2(), conn, add);
    }

    // 单点索引更新
    private void updateSingleIndex(Map<BlockPos, Set<WireConnection>> index, BlockPos pos, WireConnection conn, boolean add) {
        Set<WireConnection> set = index.computeIfAbsent(pos, k -> ConcurrentHashMap.newKeySet());
        if (add) {
            set.add(conn);
        } else {
            set.remove(conn);
            if (set.isEmpty()) index.remove(pos);
        }
    }

    // 判断位置是否处于指定区块内
    private boolean isInChunk(BlockPos blockPos, BlockPos chunkPos) {
        return (blockPos.getX() >> 4) == (chunkPos.getX() >> 4) && (blockPos.getZ() >> 4) == (chunkPos.getZ() >> 4);
    }

    // 标记数据脏并保存
    private void markDirty(Level level) {
        setDirty();
        if (level instanceof ServerLevel serverLevel) serverLevel.getDataStorage().save();
    }

    // 同步当前维度所有连接到所有在线玩家
    private void syncAllToDimension(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ResourceLocation dim = level.dimension().location();
        Set<WireConnection> connSet = getAllConnections(dim);
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (isInSameDimension(serverLevel, player)) {
                NetworkHandler.sendToClient(new WireSyncPacket(dim, connSet), player);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag dimensionsList = new ListTag();
        for (Map.Entry<ResourceLocation, Set<WireConnection>> entry : connections.entrySet()) {
            CompoundTag dimTag = new CompoundTag();
            dimTag.putString("dimension", entry.getKey().toString());
            dimTag.put("connections", saveConnectionList(entry.getValue()));
            dimTag.put("activeConnections", saveConnectionList(activeConnections.getOrDefault(entry.getKey(), Set.of())));
            dimensionsList.add(dimTag);
        }
        tag.put("dimensions", dimensionsList);
        return tag;
    }

    private ListTag saveConnectionList(Set<WireConnection> connections) {
        ListTag list = new ListTag();
        connections.forEach(conn -> list.add(conn.save()));
        return list;
    }

    // 玩家加入时同步所有维度连接数据
    public void syncAllToClient(ServerLevel level, Player player) {
        connections.forEach((dim, connSet) ->
                NetworkHandler.sendToClient(new WireSyncPacket(dim, connSet), (ServerPlayer) player));
    }

    // 世界加载完成后重新验证并激活所有连接
    public void revalidateAllConnections(ServerLevel level) {
        ResourceLocation dim = level.dimension().location();
        Set<WireConnection> activeSet = activeConnections.get(dim);

        if (activeSet == null || activeSet.isEmpty()) {
            getAllConnections(dim).forEach(c -> tryActivateConnection(level, c));
            return;
        }

        for (WireConnection conn : new HashSet<>(activeSet)) {
            if (!level.isLoaded(conn.pos1()) || !level.isLoaded(conn.pos2())) {
                activeSet.remove(conn);
                continue;
            }
            BlockEntity be1 = level.getBlockEntity(conn.pos1());
            BlockEntity be2 = level.getBlockEntity(conn.pos2());
            if (be1 instanceof WireConnectable d1 && be2 instanceof WireConnectable d2) {
                if (!d1.getConnectionType().canConnectTo(d2.getConnectionType())) {
                    activeSet.remove(conn);
                    continue;
                }
                d1.onConnectionActivated(level, conn.pos1(), conn.pos2());
                d2.onConnectionActivated(level, conn.pos2(), conn.pos1());
                level.sendBlockUpdated(conn.pos1(), level.getBlockState(conn.pos1()), level.getBlockState(conn.pos1()), 3);
                level.sendBlockUpdated(conn.pos2(), level.getBlockState(conn.pos2()), level.getBlockState(conn.pos2()), 3);
            } else {
                activeSet.remove(conn);
            }
        }
        setDirty();
    }
}
