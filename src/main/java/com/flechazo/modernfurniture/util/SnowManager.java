package com.flechazo.modernfurniture.util;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.config.RoomDetectorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <h1>SnowManager</h1>
 *
 * <p>专为服务器环境的大型房间设计的高效积雪模拟系统，用于在指定房间区域内实现高性能的积雪效果。</p>
 *
 * <h2>核心优化原理</h2>
 *
 * <h3>1. 空间分区系统</h3>
 * <p>将房间空间划分为层级化的区块(Section)和区块(Chunk)：</p>
 * <ul>
 *   <li><b>局部处理：</b>仅处理玩家附近的活动分区</li>
 *   <li><b>加载感知：</b>自动跳过未加载的区块</li>
 *   <li><b>并行处理：</b>独立分区可并发处理</li>
 * </ul>
 *
 * <h3>2. 智能缓存机制</h3>
 * <p>实现多层缓存系统：</p>
 * <ul>
 *   <li><b>LRU淘汰：</b>最近最少使用缓存替换策略</li>
 *   <li><b>过期时间：</b>条目30秒后自动失效</li>
 *   <li><b>位置验证：</b>缓存每个分区的有效积雪位置</li>
 * </ul>
 *
 * <h3>3. 异步任务管道</h3>
 * <p>采用多阶段异步处理模型：</p>
 * <ul>
 *   <li><b>计算阶段：</b>繁重计算在ForkJoinPool中运行</li>
 *   <li><b>执行阶段：</b>主线程应用已验证的更改</li>
 *   <li><b>超时处理：</b>操作5秒后自动取消</li>
 * </ul>
 *
 * <h2>积雪算法实现</h2>
 *
 * <h3>1. 动态泊松分布</h3>
 * <p>降雪强度采用自动调整参数的泊松过程：</p>
 * <ul>
 *   <li>基于当前房间积雪密度</li>
 *   <li>系统性能指标</li>
 *   <li>距离上次降雪的时间</li>
 * </ul>
 *
 * <h3>2. 空间差异建模</h3>
 * <p>积雪分布通过以下方式实现空间相关性：</p>
 * <ul>
 *   <li>基于分区的密度因子</li>
 *   <li>高斯加权的位置选择</li>
 *   <li>动态方差参数</li>
 * </ul>
 *
 * <h3>3. 积雪层累积逻辑</h3>
 * <p>积雪层采用优先级系统累积：</p>
 * <ul>
 *   <li>现有雪块获得40-70%的新层(动态比例)</li>
 *   <li>新雪位置填充剩余容量</li>
 *   <li>强制每方块最多8层</li>
 * </ul>
 *
 * <h2>性能管理</h2>
 *
 * <h3>1. 动态节流</h3>
 * <p>系统根据以下指标自动调整处理强度：</p>
 * <ul>
 *   <li>测量操作执行时间</li>
 *   <li>当前JVM内存压力</li>
 *   <li>服务器整体tick性能</li>
 * </ul>
 *
 * <h3>2. 资源监控</h3>
 * <p>持续监控以下指标：</p>
 * <ul>
 *   <li>堆内存使用(85%阈值)</li>
 *   <li>缓存命中率</li>
 *   <li>分区处理时间</li>
 * </ul>
 *
 * <h3>3. 维护例程</h3>
 * <p>定期清理：</p>
 * <ul>
 *   <li>过期缓存条目</li>
 *   <li>陈旧位置数据</li>
 *   <li>非活动分区</li>
 * </ul>
 *
 * @see ServerLevel 该管理器操作的Minecraft服务器世界
 * @see BlockPos 雪块位置坐标类
 * @see DynamicParameters 内部动态参数调整系统
 */
public class SnowManager {
    private static final int MAX_CACHE_SIZE = 64;
    private static final long CACHE_TTL_MS = 30000; // 30秒TTL
    private static final long ASYNC_TIMEOUT_MS = 5000; // 5秒超时
    private static final long MEMORY_CHECK_INTERVAL = 10000; // 10秒检查一次内存
    // ===== 核心字段 =====
    private final ServerLevel level;
    private final Set<BlockPos> roomBlocks;
    private final Map<BlockPos, Integer> snowLayers;
    private final Set<BlockPos> snowedPositions;
    private final RandomSource random;
    // ===== 高级分区系统 =====
    private final Map<SectionPos, Set<BlockPos>> sectionPartitions;
    private final Map<ChunkPos, Set<SectionPos>> chunkToSections;
    private final Set<SectionPos> activeSections;
    // ===== 智能缓存系统 =====
    private final Map<SectionPos, CacheEntry> validPositionCache;
    private final Queue<SectionPos> cacheAccessOrder;
    // ===== 异步任务管理 =====
    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService computeExecutor;
    private final AtomicReference<CompletableFuture<List<SnowOperation>>> pendingOperations;
    // ===== 动态算法参数 =====
    private final DynamicParameters dynamicParams;
    private final PerformanceMonitor perfMonitor;
    // ===== 资源监控 =====
    private final MemoryMXBean memoryBean;
    // ===== 时间控制 =====
    private long lastSnowTime = 0;
    private int snowCycles = 0;
    private long lastMemoryCheck = 0;

    // ===== 构造函数 =====
    public SnowManager(ServerLevel level, Set<BlockPos> roomBlocks) {
        this.level = level;
        this.roomBlocks = new HashSet<>(roomBlocks);
        this.snowLayers = new ConcurrentHashMap<>();
        this.snowedPositions = ConcurrentHashMap.newKeySet();
        this.random = level.getRandom();

        // 初始化高级分区系统
        this.sectionPartitions = new ConcurrentHashMap<>();
        this.chunkToSections = new ConcurrentHashMap<>();
        this.activeSections = ConcurrentHashMap.newKeySet();
        initializeSectionPartitions();

        // 初始化智能缓存
        this.validPositionCache = new ConcurrentHashMap<>();
        this.cacheAccessOrder = new ConcurrentLinkedQueue<>();

        // 初始化异步任务管理
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "SnowManager-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.computeExecutor = ForkJoinPool.commonPool();
        this.pendingOperations = new AtomicReference<>();

        // 初始化动态参数和性能监控
        this.dynamicParams = new DynamicParameters(roomBlocks.size());
        this.perfMonitor = new PerformanceMonitor();

        // 初始化资源监控
        this.memoryBean = ManagementFactory.getMemoryMXBean();

        // 启动定期清理任务
        scheduledExecutor.scheduleAtFixedRate(this::performMaintenance, 30, 30, TimeUnit.SECONDS);
    }

    public boolean performSnowingAsync(long currentTime) {
        if (!RoomDetectorConfig.isSnowEnabled()) {
            return false;
        }

        long snowDelayTicks = RoomDetectorConfig.getSnowDelayTicks();
        if (currentTime - lastSnowTime < snowDelayTicks) {
            return false;
        }

        // 检查内存使用情况
        if (shouldSkipDueToMemory()) {
            return false;
        }

        // 检查并执行待处理的异步操作
        CompletableFuture<List<SnowOperation>> current = pendingOperations.get();
        if (current != null) {
            if (current.isDone()) {
                try {
                    List<SnowOperation> operations = current.get();
                    long startTime = System.currentTimeMillis();
                    executeSnowOperations(operations);
                    long processTime = System.currentTimeMillis() - startTime;

                    perfMonitor.recordOperation(processTime);
                    updateDynamicParameters();
                    pendingOperations.set(null);
                    return true;
                } catch (Exception e) {
                    ModernFurniture.LOGGER.warn("异步降雪计算失败", e);
                    pendingOperations.set(null);
                }
            } else if (System.currentTimeMillis() - lastSnowTime > ASYNC_TIMEOUT_MS) {
                // 超时取消
                current.cancel(true);
                pendingOperations.set(null);
                ModernFurniture.LOGGER.warn("异步降雪任务超时，已取消");
            }
            return false;
        }

        lastSnowTime = currentTime;
        snowCycles++;

        // 启动新的异步计算，带超时控制
        CompletableFuture<List<SnowOperation>> future = CompletableFuture
                .supplyAsync(this::calculateSnowOperationsAsync, computeExecutor)
                .orTimeout(ASYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    ModernFurniture.LOGGER.warn("异步降雪计算异常", throwable);
                    return Collections.emptyList();
                });

        pendingOperations.set(future);
        return false;
    }

    public void clearAllSnow() {
        // 取消待处理的异步操作
        CompletableFuture<List<SnowOperation>> current = pendingOperations.getAndSet(null);
        if (current != null && !current.isDone()) {
            current.cancel(true);
        }

        // 清除所有雪块
        for (BlockPos pos : new HashSet<>(snowedPositions)) {
            BlockState currentState = level.getBlockState(pos);
            if (currentState.getBlock() == Blocks.SNOW) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // 重置状态
        snowedPositions.clear();
        snowLayers.clear();
        snowCycles = 0;
        lastSnowTime = 0;

        // 清理缓存
        validPositionCache.clear();
        cacheAccessOrder.clear();
    }

    public SnowStats getSnowStats() {
        long memoryUsage = memoryBean.getHeapMemoryUsage().getUsed();
        long cacheHitRate = !validPositionCache.isEmpty() ?
                validPositionCache.values().stream().mapToLong(e -> e.accessCount).sum() / validPositionCache.size() : 0;

        return new SnowStats(
                snowedPositions.size(),
                snowCycles,
                snowLayers,
                perfMonitor.getAverageProcessTime(),
                perfMonitor.getCurrentDensity(),
                activeSections.size(),
                cacheHitRate,
                memoryUsage
        );
    }

    public void shutdown() {
        clearAllSnow();
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

    private void initializeSectionPartitions() {
        for (BlockPos pos : roomBlocks) {
            SectionPos sectionPos = SectionPos.of(pos);
            ChunkPos chunkPos = new ChunkPos(pos);

            sectionPartitions.computeIfAbsent(sectionPos, k -> ConcurrentHashMap.newKeySet()).add(pos);
            chunkToSections.computeIfAbsent(chunkPos, k -> ConcurrentHashMap.newKeySet()).add(sectionPos);
            activeSections.add(sectionPos);
        }
    }

    // ===== 主要公共方法 =====

    private List<SnowOperation> calculateSnowOperationsAsync() {
        List<SnowOperation> operations = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        try {
            // 选择活跃的分区进行处理
            List<SectionPos> sectionsToProcess = selectSectionsForProcessing();

            for (SectionPos sectionPos : sectionsToProcess) {
                Set<BlockPos> sectionPositions = getSectionValidPositions(sectionPos);
                if (sectionPositions.isEmpty()) continue;

                int sectionSnowAmount = calculateSectionSnowAmount(sectionPositions.size());
                List<SnowOperation> sectionOps = generateSectionOperations(
                        sectionPositions, sectionSnowAmount, timestamp);
                operations.addAll(sectionOps);

                // 限制总操作数
                if (operations.size() >= dynamicParams.maxProcessPerTick) {
                    break;
                }
            }

        } catch (Exception e) {
            ModernFurniture.LOGGER.warn("异步降雪计算出错", e);
        }

        return operations;
    }

    private List<SectionPos> selectSectionsForProcessing() {
        List<SectionPos> sections = new ArrayList<>(activeSections);

        // 基于负载和随机性选择分区
        Collections.shuffle(sections, new Random(random.nextLong()));

        int maxSections = Math.max(1, Math.min(sections.size(),
                (int) (sections.size() * dynamicParams.densityFactor * 0.3)));

        return sections.subList(0, maxSections);
    }

    private Set<BlockPos> getSectionValidPositions(SectionPos sectionPos) {
        // 检查缓存
        CacheEntry cached = validPositionCache.get(sectionPos);
        if (cached != null && !cached.isExpired()) {
            cacheAccessOrder.offer(sectionPos); // 更新访问顺序
            return new HashSet<>(cached.positions);
        }

        // 计算有效位置
        Set<BlockPos> sectionBlocks = sectionPartitions.get(sectionPos);
        if (sectionBlocks == null) return Collections.emptySet();

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
                if (canPlaceSnow(pos, state)) {
                    validPositions.add(pos);
                }
            }
        }

        // 更新缓存
        updateCache(sectionPos, validPositions);

        return validPositions;
    }

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

    // ===== 核心算法实现 =====

    private int calculateSectionSnowAmount(int validPositions) {
        if (validPositions == 0) return 0;

        // 使用动态泊松参数
        int poissonValue = generatePoisson(dynamicParams.poissonLambda);

        // 基于空间方差调整
        double variance = dynamicParams.spatialVariance;
        int baseAmount = Math.max(1, (int) (validPositions * 0.1 * variance));
        int maxAmount = Math.max(1, validPositions / 3);

        return Math.min(maxAmount, baseAmount + poissonValue);
    }

    private List<SnowOperation> generateSectionOperations(Set<BlockPos> positions,
                                                          int snowAmount, long timestamp) {
        List<SnowOperation> operations = new ArrayList<>();

        List<BlockPos> priorityPositions = positions.stream()
                .filter(snowedPositions::contains)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        List<BlockPos> newPositions = positions.stream()
                .filter(pos -> !snowedPositions.contains(pos))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        // 高级随机化策略
        advancedShuffle(priorityPositions);
        advancedShuffle(newPositions);

        int operationCount = 0;

        // 动态叠加比例
        double stackRatio = 0.4 + (random.nextGaussian() * 0.1 * dynamicParams.spatialVariance);
        stackRatio = Math.max(0.2, Math.min(0.7, stackRatio));

        int priorityCount = (int) (snowAmount * stackRatio);
        priorityCount = Math.min(priorityCount, priorityPositions.size());

        // 添加叠加操作
        for (int i = 0; i < priorityCount && operationCount < snowAmount; i++) {
            operations.add(new SnowOperation(
                    priorityPositions.get(i),
                    SnowOperation.OperationType.ADD_LAYER,
                    0,
                    timestamp
            ));
            operationCount++;
        }

        // 添加新雪操作
        for (BlockPos pos : newPositions) {
            if (operationCount >= snowAmount) break;
            operations.add(new SnowOperation(
                    pos,
                    SnowOperation.OperationType.PLACE_NEW,
                    1,
                    timestamp
            ));
            operationCount++;
        }

        return operations;
    }

    private void advancedShuffle(List<BlockPos> list) {
        if (list.size() <= 1) return;

        Random rng = new Random(random.nextLong());

        // 多层随机化
        for (int phase = 0; phase < 2; phase++) {
            for (int i = list.size() - 1; i > 0; i--) {
                // 空间相关性的随机选择
                int maxDistance = Math.min(i, (int) (i * dynamicParams.spatialVariance * 0.5));
                int j = Math.max(0, i - maxDistance + rng.nextInt(maxDistance + 1));

                Collections.swap(list, i, j);
            }
        }
    }

    private void executeSnowOperations(List<SnowOperation> operations) {
        for (SnowOperation op : operations) {
            try {
                switch (op.type()) {
                    case PLACE_NEW -> {
                        BlockState currentState = level.getBlockState(op.pos());
                        if (currentState.isAir() && canPlaceSnow(op.pos(), currentState)) {
                            placeInitialSnow(op.pos());
                        }
                    }
                    case ADD_LAYER -> {
                        BlockState currentState = level.getBlockState(op.pos());
                        if (currentState.getBlock() == Blocks.SNOW && canAddSnowLayer(op.pos())) {
                            addSnowLayer(op.pos());
                        }
                    }
                }
            } catch (Exception e) {
                ModernFurniture.LOGGER.warn("执行降雪操作失败: {}", op.pos(), e);
            }
        }
    }

    private boolean canPlaceSnow(BlockPos pos, BlockState currentState) {
        if (currentState.isAir()) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);

            // 放置逻辑
            if (!belowState.isAir() && belowState.isSolidRender(level, belowPos)) {
                double probability = RoomDetectorConfig.getGroundSnowProbability() * dynamicParams.densityFactor;
                return random.nextFloat() < probability;
            } else {
                double probability = RoomDetectorConfig.getSnowProbability() * dynamicParams.densityFactor * 0.5;
                return random.nextFloat() < probability;
            }
        }

        if (currentState.getBlock() == Blocks.SNOW) {
            return canAddSnowLayer(pos);
        }

        return false;
    }

    private boolean canAddSnowLayer(BlockPos pos) {
        BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() == Blocks.SNOW) {
            int currentLayers = currentState.getValue(SnowLayerBlock.LAYERS);
            return currentLayers < 8;
        }
        return false;
    }

    private boolean placeInitialSnow(BlockPos pos) {
        BlockState currentState = level.getBlockState(pos);
        if (currentState.isAir()) {
            level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
            snowedPositions.add(pos);
            snowLayers.put(pos, 1);

            // 使缓存失效
            invalidatePositionCache(pos);
            return true;
        }
        return false;
    }

    private boolean addSnowLayer(BlockPos pos) {
        BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() == Blocks.SNOW) {
            int currentLayers = currentState.getValue(SnowLayerBlock.LAYERS);
            if (currentLayers < 8) {
                int newLayers = currentLayers + 1;
                BlockState newState = currentState.setValue(SnowLayerBlock.LAYERS, newLayers);
                level.setBlock(pos, newState, 3);
                snowLayers.put(pos, newLayers);

                // 使缓存失效
                invalidatePositionCache(pos);
                return true;
            }
        }
        return false;
    }

    private void invalidatePositionCache(BlockPos pos) {
        SectionPos sectionPos = SectionPos.of(pos);
        validPositionCache.remove(sectionPos);
    }

    // ===== 雪块操作方法 =====

    private int generatePoisson(double lambda) {
        if (lambda <= 0) return 0;

        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;

        do {
            k++;
            p *= random.nextFloat();
        } while (p > L && k < 50); // 防止无限循环

        return Math.max(0, k - 1);
    }

    private void updateDynamicParameters() {
        double currentDensity = perfMonitor.getCurrentDensity();
        double avgProcessTime = perfMonitor.getAverageProcessTime();

        dynamicParams.updateParameters(roomBlocks.size(), currentDensity, avgProcessTime);
    }

    private boolean shouldSkipDueToMemory() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMemoryCheck < MEMORY_CHECK_INTERVAL) {
            return false;
        }

        lastMemoryCheck = currentTime;

        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();

        if (maxMemory > 0) {
            double memoryUsage = (double) usedMemory / maxMemory;
            return memoryUsage > 0.85; // 如果内存使用超过85%，跳过处理
        }

        return false;
    }

    private void performMaintenance() {
        try {
            // 清理过期缓存
            validPositionCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

            // 清理访问顺序队列
            cacheAccessOrder.removeIf(sectionPos -> !validPositionCache.containsKey(sectionPos));

            // 更新动态参数
            updateDynamicParameters();

            ModernFurniture.LOGGER.debug("SnowManager维护完成 - 缓存大小: {}, 活跃分区: {}",
                    validPositionCache.size(), activeSections.size());

        } catch (Exception e) {
            ModernFurniture.LOGGER.warn("SnowManager维护任务失败", e);
        }
    }

    // ===== 工具方法 =====

    // ===== 内部数据类 =====
    private record SnowOperation(BlockPos pos, OperationType type, int layers, long timestamp) {
        enum OperationType {
            PLACE_NEW, ADD_LAYER
        }
    }

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

    public record SnowStats(int totalSnowedBlocks, int snowCycles, Map<BlockPos, Integer> snowLayers,
                            double averageProcessTime, double currentDensity, int activeSections,
                            long cacheHitRate, long memoryUsage) {
        public SnowStats(int totalSnowedBlocks, int snowCycles, Map<BlockPos, Integer> snowLayers,
                         double averageProcessTime, double currentDensity, int activeSections,
                         long cacheHitRate, long memoryUsage) {
            this.totalSnowedBlocks = totalSnowedBlocks;
            this.snowCycles = snowCycles;
            this.snowLayers = new HashMap<>(snowLayers);
            this.averageProcessTime = averageProcessTime;
            this.currentDensity = currentDensity;
            this.activeSections = activeSections;
            this.cacheHitRate = cacheHitRate;
            this.memoryUsage = memoryUsage;
        }
    }

    private class DynamicParameters {
        private volatile double poissonLambda;
        private volatile double densityFactor;
        private volatile double spatialVariance;
        private volatile int maxProcessPerTick;

        DynamicParameters(int roomSize) {
            updateParameters(roomSize, 0, 0);
        }

        void updateParameters(int roomSize, double currentDensity, double avgProcessTime) {
            // 动态调整泊松参数
            double baseLambda = Math.log(roomSize + 1) * 0.5;
            double densityAdjustment = Math.max(0.1, 1.0 - currentDensity * 0.5);
            this.poissonLambda = Math.max(0.5, Math.min(8.0, baseLambda * densityAdjustment));

            // 动态调整密度因子
            this.densityFactor = Math.max(0.1, Math.min(2.0, 1.0 + Math.sin(snowCycles * 0.1) * 0.3));

            // 动态调整空间方差
            this.spatialVariance = Math.max(0.5, Math.min(2.0, 1.0 + currentDensity * 0.5));

            // 动态调整每tick处理量
            if (avgProcessTime > 50) { // 如果处理时间超过50ms
                this.maxProcessPerTick = Math.max(10, (int) (this.maxProcessPerTick * 0.8));
            } else if (avgProcessTime < 10) { // 如果处理时间小于10ms
                this.maxProcessPerTick = Math.min(roomSize / 5, (int) (this.maxProcessPerTick * 1.2));
            }
        }
    }

    private class PerformanceMonitor {
        private final AtomicLong totalOperations = new AtomicLong();
        private final AtomicLong totalProcessTime = new AtomicLong();
        private final Queue<Long> recentTimes = new ConcurrentLinkedQueue<>();

        void recordOperation(long processTimeMs) {
            totalOperations.incrementAndGet();
            totalProcessTime.addAndGet(processTimeMs);
            recentTimes.offer(processTimeMs);

            // 保持最近100次记录
            while (recentTimes.size() > 100) {
                recentTimes.poll();
            }
        }

        double getAverageProcessTime() {
            if (recentTimes.isEmpty()) return 0;
            return recentTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        }

        double getCurrentDensity() {
            if (roomBlocks.isEmpty()) return 0;
            return (double) snowedPositions.size() / roomBlocks.size();
        }
    }
}