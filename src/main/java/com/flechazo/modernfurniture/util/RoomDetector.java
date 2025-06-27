package com.flechazo.modernfurniture.util;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.config.modules.RoomDetectionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * <h1>高性能房间检测器</h1>
 *
 * <p>基于广度优先搜索(BFS)算法实现的通用封闭空间检测系统，专为大型建筑结构优化设计。</p>
 *
 * <h2>核心设计原理</h2>
 *
 * <h3>1. 边界约束搜索</h3>
 * <p>采用立方体边界约束的BFS算法：</p>
 * <ul>
 *   <li><b>三维搜索半径：</b>通过maxRadius参数控制XYZ三个方向的搜索范围</li>
 *   <li><b>世界高度保护：</b>自动适配世界构建高度限制(minBuildHeight/maxBuildHeight)</li>
 *   <li><b>区块加载检查：</b>实时验证目标区块是否已加载</li>
 * </ul>
 *
 * <h3>2. 性能保护机制</h3>
 * <p>三重防护措施确保系统稳定性：</p>
 * <ul>
 *   <li><b>超时中断：</b>默认最长处理时间限制(maxSearchTimeMs)</li>
 *   <li><b>体积限制：</b>最大房间尺寸限制(maxRoomSize)</li>
 *   <li><b>批量处理：</b>每处理1000方块执行一次条件检查</li>
 * </ul>
 *
 * <h3>3. 通过性判定逻辑</h3>
 * <p>多层级的方块可通过性检测：</p>
 * <ul>
 *   <li><b>基础检测：</b>空气方块自动通过</li>
 *   <li><b>流体检测：</b>含流体方块视为可通过</li>
 *   <li><b>特殊属性：</b>支持OPEN属性方块的动态判定</li>
 * </ul>
 *
 * <h2>算法实现细节</h2>
 *
 * <h3>1. 搜索流程</h3>
 * <ol>
 *   <li>初始化双端队列(ArrayDeque)和已访问集合(HashSet)</li>
 *   <li>从起始点向6个方向扩展搜索</li>
 *   <li>使用visit标记防止重复处理</li>
 * </ol>
 *
 * <h3>2. 内存优化</h3>
 * <p>预设集合初始容量减少扩容开销：</p>
 * <ul>
 *   <li>visited集合初始8192容量</li>
 *   <li>queue队列初始1024容量</li>
 *   <li>roomBlocks集合初始8192容量</li>
 * </ul>
 *
 * <h3>3. 异步支持</h3>
 * <p>通过CompletableFuture提供非阻塞调用：</p>
 * <ul>
 *   <li>findRoomAsync方法实现异步执行</li>
 *   <li>与主线程搜索逻辑保持相同约束条件</li>
 * </ul>
 *
 * @author flechazo
 * @version 0.0.1
 * @see BlockPos 使用Minecraft坐标系表示方块位置
 * @see Level 表示检测目标世界
 * @see RoomDetectionConfig 获取搜索参数配置
 */
public class RoomDetector {

    /**
     * 执行封闭空间检测
     *
     * @param level    目标世界对象
     * @param startPos 检测起始坐标
     * @return 包含所有可通过方块的集合，若失败返回空集合
     * @implNote 典型处理时间与房间体积成正比，10,000方块约需50-100ms
     */
    public static Set<BlockPos> findRoom(Level level, BlockPos startPos) {
        long startTime = System.currentTimeMillis();

        Set<BlockPos> visited = new HashSet<>(8192);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(1024);
        Set<BlockPos> roomBlocks = new HashSet<>(8192);

        if (!isPassable(level, startPos)) {
            return Collections.emptySet();
        }

        final int maxRadius = RoomDetectionConfig.maxSearchDistance;
        final int maxVolume = RoomDetectionConfig.maxRoomSize;
        final long maxTime = RoomDetectionConfig.maxSearchTimeMs;

        final int minX = startPos.getX() - maxRadius;
        final int maxX = startPos.getX() + maxRadius;
        final int minY = Math.max(level.getMinBuildHeight(), startPos.getY() - maxRadius);
        final int maxY = Math.min(level.getMaxBuildHeight(), startPos.getY() + maxRadius);
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

            checkAndAdd(level, new BlockPos(x, y + 1, z), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
            checkAndAdd(level, new BlockPos(x, y - 1, z), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
            checkAndAdd(level, new BlockPos(x + 1, y, z), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
            checkAndAdd(level, new BlockPos(x - 1, y, z), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
            checkAndAdd(level, new BlockPos(x, y, z + 1), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
            checkAndAdd(level, new BlockPos(x, y, z - 1), visited, queue, roomBlocks, minX, maxX, minY, maxY, minZ, maxZ);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        ModernFurniture.LOGGER.debug("[房间检测] 完成: {}方块, {}ms", roomBlocks.size(), elapsed);

        return roomBlocks;
    }

    /**
     * 检查并添加相邻方块到队列
     */
    private static void checkAndAdd(Level level, BlockPos pos, Set<BlockPos> visited,
                                    ArrayDeque<BlockPos> queue, Set<BlockPos> roomBlocks,
                                    int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
            return;
        }

        if (visited.add(pos)) {
            if (!level.hasChunkAt(pos)) {
                return;
            }

            if (isPassable(level, pos)) {
                queue.offer(pos);
                roomBlocks.add(pos);
            }
        }
    }

    /**
     * 检查方块是否可通过
     *
     * @param level 世界对象
     * @param pos   方块位置
     * @return 如果方块可通过返回true
     */
    public static boolean isPassable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return true;

        return state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
    }

    /**
     * 异步执行房间检测
     *
     * @param level    目标世界对象
     * @param startPos 检测起始坐标
     * @return 包含搜索结果的CompletableFuture
     * @apiNote 建议配合thenAcceptAsync等异步方法使用
     * @implSpec 实际使用ForkJoinPool.commonPool()线程池
     */
    public static CompletableFuture<Set<BlockPos>> findRoomAsync(Level level, BlockPos startPos) {
        return CompletableFuture.supplyAsync(() -> findRoom(level, startPos));
    }
}