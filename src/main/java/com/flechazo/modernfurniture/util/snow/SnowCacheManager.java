package com.flechazo.modernfurniture.util.snow;

import com.flechazo.modernfurniture.ModernFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.concurrent.*;

/**
 * 积雪缓存管理器
 *
 * <p>实现智能缓存机制，包括LRU淘汰、TTL过期、事件驱动失效等功能。</p>
 *
 * <h2>主要功能</h2>
 * <ul>
 *   <li>有效位置缓存管理</li>
 *   <li>LRU + TTL 缓存策略</li>
 *   <li>批量失效处理</li>
 *   <li>缓存统计和监控</li>
 * </ul>
 */
public class SnowCacheManager {
    private static final int MAX_CACHE_SIZE = 64;
    private static final long CACHE_TTL_MS = 30000; // 30秒TTL
    private static final long CACHE_INVALIDATION_BATCH_DELAY = 100; // 100ms批量失效延迟
    private static final int MAX_INVALIDATION_BATCH_SIZE = 20; // 最大批量失效数量

    private final Map<SectionPos, CacheEntry> validPositionCache;
    private final Queue<SectionPos> cacheAccessOrder;
    private final Set<SectionPos> pendingInvalidations;
    private final Map<SectionPos, Long> sectionChangeFrequency;
    private final ScheduledExecutorService scheduledExecutor;
    private final ScheduledFuture<?> batchInvalidationTask;

    /**
     * 构造缓存管理器
     */
    public SnowCacheManager() {
        this.validPositionCache = new ConcurrentHashMap<>();
        this.cacheAccessOrder = new ConcurrentLinkedQueue<>();
        this.pendingInvalidations = ConcurrentHashMap.newKeySet();
        this.sectionChangeFrequency = new ConcurrentHashMap<>();

        this.scheduledExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "SnowCache-Scheduler");
            t.setDaemon(true);
            return t;
        });

        // 启动批量失效任务
        this.batchInvalidationTask = scheduledExecutor.scheduleAtFixedRate(
                this::processBatchInvalidation,
                CACHE_INVALIDATION_BATCH_DELAY,
                CACHE_INVALIDATION_BATCH_DELAY,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 获取分区的有效位置（带缓存）
     *
     * @param sectionPos           分区位置
     * @param sectionBlocks        分区方块集合
     * @param level                服务器世界
     * @param snowPlacementChecker 积雪放置检查器
     * @return 有效位置集合
     */
    public Set<BlockPos> getSectionValidPositions(SectionPos sectionPos,
                                                  Set<BlockPos> sectionBlocks,
                                                  ServerLevel level,
                                                  SnowPlacementChecker snowPlacementChecker) {
        // 检查缓存
        CacheEntry cached = validPositionCache.get(sectionPos);
        if (cached != null && !cached.isExpired()) {
            cacheAccessOrder.offer(sectionPos); // 更新访问顺序
            return new HashSet<>(cached.positions);
        }

        // 计算有效位置
        Set<BlockPos> validPositions = calculateValidPositions(sectionPos, sectionBlocks, level, snowPlacementChecker);

        // 更新缓存
        updateCache(sectionPos, validPositions);

        return validPositions;
    }

    /**
     * 计划缓存失效
     *
     * @param pos    位置
     * @param reason 失效原因
     */
    public void scheduleInvalidation(BlockPos pos, String reason) {
        SectionPos sectionPos = SectionPos.of(pos);

        // 更新变化频率统计
        long currentTime = System.currentTimeMillis();
        sectionChangeFrequency.merge(sectionPos, currentTime, (oldTime, newTime) -> {
            // 如果变化频繁（1秒内多次变化），立即失效
            if (newTime - oldTime < 1000) {
                immediateInvalidation(sectionPos, reason + "_frequent");
                return newTime;
            }
            return newTime;
        });

        // 添加到待处理队列
        pendingInvalidations.add(sectionPos);

        ModernFurniture.LOGGER.debug("计划缓存失效: {} 原因: {}", sectionPos, reason);
    }

    /**
     * 获取缓存命中率
     *
     * @return 命中率
     */
    public long getCacheHitRate() {
        if (validPositionCache.isEmpty()) {
            return 0;
        }
        return validPositionCache.values().stream()
                .mapToLong(e -> e.accessCount)
                .sum() / validPositionCache.size();
    }

    /**
     * 清除所有缓存
     */
    public void clearAll() {
        validPositionCache.clear();
        cacheAccessOrder.clear();
        pendingInvalidations.clear();
        sectionChangeFrequency.clear();
    }

    /**
     * 关闭缓存管理器
     */
    public void shutdown() {
        if (batchInvalidationTask != null) {
            batchInvalidationTask.cancel(false);
        }

        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 计算有效位置
     */
    private Set<BlockPos> calculateValidPositions(SectionPos sectionPos,
                                                  Set<BlockPos> sectionBlocks,
                                                  ServerLevel level,
                                                  SnowPlacementChecker checker) {
        if (sectionBlocks == null || sectionBlocks.isEmpty()) {
            return Collections.emptySet();
        }

        Set<BlockPos> validPositions = new HashSet<>();
        ChunkPos chunkPos = new ChunkPos(sectionPos.x(), sectionPos.z());

        // 检查区块是否加载
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return Collections.emptySet();
        }

        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);

        for (BlockPos pos : sectionBlocks) {
            if (pos.getY() >= level.getMinBuildHeight() && pos.getY() <= level.getMaxBuildHeight()) {
                BlockState state = chunk.getBlockState(pos);
                if (checker.canPlaceSnow(pos, state, level)) {
                    validPositions.add(pos);
                }
            }
        }

        return validPositions;
    }

    /**
     * 更新缓存
     */
    private void updateCache(SectionPos sectionPos, Set<BlockPos> positions) {
        // LRU缓存管理
        if (validPositionCache.size() >= MAX_CACHE_SIZE) {
            SectionPos oldest = cacheAccessOrder.poll();
            if (oldest != null) {
                validPositionCache.remove(oldest);
            }
        }

        validPositionCache.put(sectionPos, new CacheEntry(positions));
        cacheAccessOrder.offer(sectionPos);
    }

    /**
     * 立即失效缓存
     */
    private void immediateInvalidation(SectionPos sectionPos, String reason) {
        boolean removed = validPositionCache.remove(sectionPos) != null;
        cacheAccessOrder.removeIf(pos -> pos.equals(sectionPos));

        if (removed) {
            ModernFurniture.LOGGER.debug("立即失效缓存: {} 原因: {}", sectionPos, reason);
        }
    }

    /**
     * 批量处理缓存失效
     */
    private void processBatchInvalidation() {
        if (pendingInvalidations.isEmpty()) {
            return;
        }

        Set<SectionPos> toInvalidate = new HashSet<>();
        Iterator<SectionPos> iterator = pendingInvalidations.iterator();

        // 批量处理，限制数量避免性能问题
        int count = 0;
        while (iterator.hasNext() && count < MAX_INVALIDATION_BATCH_SIZE) {
            toInvalidate.add(iterator.next());
            iterator.remove();
            count++;
        }

        // 执行批量失效
        for (SectionPos sectionPos : toInvalidate) {
            boolean removed = validPositionCache.remove(sectionPos) != null;
            if (removed) {
                cacheAccessOrder.removeIf(pos -> pos.equals(sectionPos));
                ModernFurniture.LOGGER.debug("批量失效缓存: {}", sectionPos);
            }
        }

        if (!toInvalidate.isEmpty()) {
            ModernFurniture.LOGGER.debug("批量处理缓存失效: {} 个分区", toInvalidate.size());
        }
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        final Set<BlockPos> positions;
        final long timestamp;
        final long accessCount;

        CacheEntry(Set<BlockPos> positions) {
            this.positions = new HashSet<>(positions);
            this.timestamp = System.currentTimeMillis();
            this.accessCount = 1;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}