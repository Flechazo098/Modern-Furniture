package com.flechazo.modernfurniture.util;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.config.RoomDetectorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 高性能房间检测器
 * <p>
 * 使用广度优先搜索算法检测封闭房间，专为大型房间优化。
 *
 * @author flechazo
 * @version 0.0.1
 */
public class RoomDetector {

    /**
     * 检测指定起始点的房间范围
     *
     * @param world 世界对象
     * @param startPos 检测起始位置
     * @return 房间内所有可通过方块的位置集合，如果检测失败或超时返回空集合
     */
    public static Set<BlockPos> findRoom(Level world, BlockPos startPos) {
        long startTime = System.currentTimeMillis();

        Set<BlockPos> visited = new HashSet<>(8192);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(1024);
        Set<BlockPos> roomBlocks = new HashSet<>(8192);

        if (!isPassable(world, startPos)) {
            return Collections.emptySet();
        }

        // 从配置获取参数
        final int maxRadius = RoomDetectorConfig.getMaxSearchDistance();
        final int maxVolume = RoomDetectorConfig.getMaxRoomSize();
        final long maxTime = RoomDetectorConfig.getMaxSearchTimeMs();

        final int minX = startPos.getX() - maxRadius;
        final int maxX = startPos.getX() + maxRadius;
        final int minY = Math.max(world.getMinBuildHeight(), startPos.getY() - maxRadius);
        final int maxY = Math.min(world.getMaxBuildHeight(), startPos.getY() + maxRadius);
        final int minZ = startPos.getZ() - maxRadius;
        final int maxZ = startPos.getZ() + maxRadius;

        queue.offer(startPos);
        visited.add(startPos);
        roomBlocks.add(startPos);

        int processed = 0;

        while (!queue.isEmpty()) {
            if (++processed % 1000 == 0) {
                if (System.currentTimeMillis() - startTime > maxTime) {
                    ModernFurniture.LOGGER.debug("[房间检测] 超时: {}方块, {}ms", processed, System.currentTimeMillis() - startTime);
                    return Collections.emptySet();
                }
                if (roomBlocks.size() >= maxVolume) {
                    ModernFurniture.LOGGER.debug("[房间检测] 体积过大: {}方块, {}ms", roomBlocks.size(), System.currentTimeMillis() - startTime);
                    return Collections.emptySet();
                }
            }

            BlockPos current = queue.poll();
            int x = current.getX();
            int y = current.getY();
            int z = current.getZ();

            checkAndAdd(world, new BlockPos(x, y + 1, z), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
            checkAndAdd(world, new BlockPos(x, y - 1, z), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
            checkAndAdd(world, new BlockPos(x + 1, y, z), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
            checkAndAdd(world, new BlockPos(x - 1, y, z), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
            checkAndAdd(world, new BlockPos(x, y, z + 1), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
            checkAndAdd(world, new BlockPos(x, y, z - 1), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        ModernFurniture.LOGGER.debug("[房间检测] 完成: {}方块, {}ms", roomBlocks.size(), elapsed);

        return roomBlocks;
    }

    /**
     * 检查并添加相邻方块到队列
     */
    private static void checkAndAdd(Level world, BlockPos pos, Set<BlockPos> visited,
                                    ArrayDeque<BlockPos> queue, Set<BlockPos> roomBlocks,
                                    int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
            return;
        }

        if (visited.add(pos)) {
            if (!world.hasChunkAt(pos)) {
                return;
            }

            if (isPassable(world, pos)) {
                queue.offer(pos);
                roomBlocks.add(pos);
            }
        }
    }

    /**
     * 检查方块是否可通过
     *
     * @param level 世界对象
     * @param pos 方块位置
     * @return 如果方块可通过返回true
     */
    public static boolean isPassable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return true;

        return state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
    }

    /**
     * 异步房间检测
     *
     * @param world 世界对象
     * @param startPos 检测起始位置
     * @return 异步结果
     */
    public static CompletableFuture<Set<BlockPos>> findRoomAsync(Level world, BlockPos startPos) {
        return CompletableFuture.supplyAsync(() -> findRoom(world, startPos));
    }
}