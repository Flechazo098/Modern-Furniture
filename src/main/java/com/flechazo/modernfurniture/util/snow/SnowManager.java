package com.flechazo.modernfurniture.util.snow;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.config.modules.SnowGenerationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <h1>SnowManager - 积雪管理器</h1>
 * 
 * <p>专为服务器环境的大型房间设计的高效积雪模拟系统核心管理类。</p>
 * <p>负责协调各个子系统，提供统一的积雪管理接口。</p>
 * 
 * <h2>主要职责</h2>
 * <ul>
 *   <li>协调积雪生成、缓存、性能监控等子系统</li>
 *   <li>提供外部调用的统一接口</li>
 *   <li>管理积雪生命周期</li>
 * </ul>
 * 
 * @author ModernFurniture
 * @see SnowSectionManager 分区管理
 * @see SnowCacheManager 缓存管理
 * @see SnowPerformanceMonitor 性能监控
 */
public class SnowManager {
    private final ServerLevel level;
    private final Set<BlockPos> roomBlocks;

    // 子系统组件
    private final SnowSectionManager sectionManager;
    private final SnowCacheManager cacheManager;
    private final SnowOperationExecutor operationExecutor;
    private final SnowPerformanceMonitor performanceMonitor;
    private final SnowEventHandler eventHandler;
    private final SnowAlgorithm algorithm;
    
    // 异步操作管理
    private final AtomicReference<CompletableFuture<List<SnowOperation>>> pendingOperations;
    
    // 状态跟踪
    private long lastSnowTime = 0;
    private int snowCycles = 0;

    /**
     * 构造积雪管理器
     * 
     * @param level 服务器世界
     * @param roomBlocks 房间方块位置集合
     */
    public SnowManager(ServerLevel level, Set<BlockPos> roomBlocks) {
        this.level = level;
        this.roomBlocks = new HashSet<>(roomBlocks);
        this.pendingOperations = new AtomicReference<>();
        RandomSource random = level.getRandom();
        
        // 初始化子系统
        this.sectionManager = new SnowSectionManager(roomBlocks);
        this.cacheManager = new SnowCacheManager();
        this.operationExecutor = new SnowOperationExecutor(level);
        this.performanceMonitor = new SnowPerformanceMonitor();
        this.algorithm = new SnowAlgorithm(random, roomBlocks.size());
        this.eventHandler = new SnowEventHandler(level, roomBlocks, cacheManager);
    }

    /**
     * 执行异步降雪处理
     * 
     * @param currentTime 当前时间
     * @return 是否成功执行降雪操作
     */
    public boolean performSnowingAsync(long currentTime) {
        if (!shouldPerformSnowing(currentTime)) {
            return false;
        }

        // 处理待完成的异步操作
        if (processPendingOperations()) {
            return true;
        }

        // 启动新的降雪计算
        startNewSnowCalculation(currentTime);
        return false;
    }

    /**
     * 清除所有积雪
     */
    public void clearAllSnow() {
        // 取消待处理操作
        cancelPendingOperations();
        
        // 清除积雪方块
        operationExecutor.clearAllSnow();
        
        // 重置状态
        resetState();
        
        // 清理缓存
        cacheManager.clearAll();
    }

    /**
     * 获取积雪统计信息
     * 
     * @return 积雪统计数据
     */
    public SnowStats getSnowStats() {
        return new SnowStats(
            operationExecutor.getSnowedPositionsCount(),
            snowCycles,
            operationExecutor.getSnowLayers(),
            performanceMonitor.getAverageProcessTime(),
            performanceMonitor.getCurrentDensity(roomBlocks.size()),
            sectionManager.getActiveSectionsCount(),
            cacheManager.getCacheHitRate(),
            performanceMonitor.getMemoryUsage(),
            calculateCurrentCoverage(),
            hasReachedCycleLimit(),
            hasReachedCoverageLimit()
        );
    }

    /**
     * 关闭管理器，释放资源
     */
    public void shutdown() {
        clearAllSnow();
        eventHandler.shutdown();
        cacheManager.shutdown();
        performanceMonitor.shutdown();
    }

    /**
     * 检查是否应该执行降雪
     */
    private boolean shouldPerformSnowing(long currentTime) {
        if (!SnowGenerationConfig.enableSnow) {
            return false;
        }

        if (hasReachedCycleLimit() || hasReachedCoverageLimit()) {
            return false;
        }

        long snowDelayTicks = SnowGenerationConfig.snowDelayTicks;
        if (currentTime - lastSnowTime < snowDelayTicks) {
            return false;
        }

        return !performanceMonitor.shouldSkipDueToMemory();
    }

    /**
     * 处理待完成的异步操作
     */
    private boolean processPendingOperations() {
        CompletableFuture<List<SnowOperation>> current = pendingOperations.get();
        if (current == null) {
            return false;
        }

        if (current.isDone()) {
            try {
                List<SnowOperation> operations = current.get();
                long startTime = System.currentTimeMillis();
                
                operationExecutor.executeOperations(operations);
                
                long processTime = System.currentTimeMillis() - startTime;
                performanceMonitor.recordOperation(processTime);
                algorithm.updateParameters(
                    performanceMonitor.getCurrentDensity(roomBlocks.size()),
                    performanceMonitor.getAverageProcessTime()
                );
                
                pendingOperations.set(null);
                return true;
            } catch (Exception e) {
                ModernFurniture.LOGGER.warn("异步降雪计算失败", e);
                pendingOperations.set(null);
            }
        } else if (System.currentTimeMillis() - lastSnowTime > 5000) {
            // 超时取消
            current.cancel(true);
            pendingOperations.set(null);
            ModernFurniture.LOGGER.warn("异步降雪任务超时，已取消");
        }
        
        return false;
    }

    /**
     * 启动新的降雪计算
     */
    private void startNewSnowCalculation(long currentTime) {
        lastSnowTime = currentTime;
        snowCycles++;

        CompletableFuture<List<SnowOperation>> future = CompletableFuture
            .supplyAsync(this::calculateSnowOperations, algorithm.getExecutor())
            .orTimeout(5000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .exceptionally(throwable -> {
                ModernFurniture.LOGGER.warn("异步降雪计算异常", throwable);
                return Collections.emptyList();
            });

        pendingOperations.set(future);
    }

    /**
     * 计算降雪操作
     */
    private List<SnowOperation> calculateSnowOperations() {
        return algorithm.calculateSnowOperations(
            sectionManager,
            cacheManager,
            level,
            operationExecutor.getSnowedPositions()
        );
    }

    /**
     * 取消待处理的操作
     */
    private void cancelPendingOperations() {
        CompletableFuture<List<SnowOperation>> current = pendingOperations.getAndSet(null);
        if (current != null && !current.isDone()) {
            current.cancel(true);
        }
    }

    /**
     * 重置状态
     */
    private void resetState() {
        snowCycles = 0;
        lastSnowTime = 0;
    }

    /**
     * 计算当前覆盖率
     */
    private double calculateCurrentCoverage() {
        if (roomBlocks.isEmpty()) {
            return 0.0;
        }

        int validPositions = 0;
        int snowedValidPositions = 0;
        Set<BlockPos> snowedPositions = operationExecutor.getSnowedPositions();

        for (BlockPos pos : roomBlocks) {
            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                BlockPos belowPos = pos.below();
                BlockState belowState = level.getBlockState(belowPos);
                if (!belowState.isAir() && belowState.isSolidRender(level, belowPos)) {
                    validPositions++;
                    if (snowedPositions.contains(pos)) {
                        snowedValidPositions++;
                    }
                }
            } else if (state.getBlock() == Blocks.SNOW) {
                validPositions++;
                snowedValidPositions++;
            }
        }

        return validPositions > 0 ? (double) snowedValidPositions / validPositions : 0.0;
    }

    /**
     * 检查是否达到周期限制
     */
    private boolean hasReachedCycleLimit() {
        return SnowGenerationConfig.maxSnowCycles >= 0 && 
               snowCycles >= SnowGenerationConfig.maxSnowCycles;
    }

    /**
     * 检查是否达到覆盖率限制
     */
    private boolean hasReachedCoverageLimit() {
        if (SnowGenerationConfig.snowCoverageRatio <= 0) {
            return false;
        }
        return calculateCurrentCoverage() >= SnowGenerationConfig.snowCoverageRatio;
    }
}