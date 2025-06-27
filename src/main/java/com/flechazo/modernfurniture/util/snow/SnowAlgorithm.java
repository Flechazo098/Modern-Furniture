package com.flechazo.modernfurniture.util.snow;

import com.flechazo.modernfurniture.ModernFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * 积雪算法实现
 * 
 * <p>实现动态泊松分布、空间差异建模、积雪层累积等核心算法。</p>
 * 
 * <h2>主要功能</h2>
 * <ul>
 *   <li>动态泊松分布计算</li>
 *   <li>空间相关性建模</li>
 *   <li>积雪操作生成</li>
 *   <li>参数动态调整</li>
 * </ul>
 */
public class SnowAlgorithm {
    private final RandomSource random;
    private final ExecutorService executor;
    private final DynamicParameters dynamicParams;
    private final SnowPlacementChecker placementChecker;

    /**
     * 构造积雪算法
     * 
     * @param random 随机数生成器
     * @param roomSize 房间大小
     */
    public SnowAlgorithm(RandomSource random, int roomSize) {
        this.random = random;
        this.executor = ForkJoinPool.commonPool();
        this.dynamicParams = new DynamicParameters(roomSize);
        this.placementChecker = new SnowPlacementChecker(random);
    }

    /**
     * 计算积雪操作
     * 
     * @param sectionManager 分区管理器
     * @param cacheManager 缓存管理器
     * @param level 服务器世界
     * @param snowedPositions 已有积雪位置
     * @return 积雪操作列表
     */
    public List<SnowOperation> calculateSnowOperations(SnowSectionManager sectionManager,
                                                       SnowCacheManager cacheManager,
                                                       ServerLevel level,
                                                       Set<BlockPos> snowedPositions) {
        List<SnowOperation> operations = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        try {
            // 选择活跃的分区进行处理
            List<SectionPos> sectionsToProcess = sectionManager.selectSectionsForProcessing(
                dynamicParams.densityFactor, new Random(random.nextLong()));

            for (SectionPos sectionPos : sectionsToProcess) {
                Set<BlockPos> sectionBlocks = sectionManager.getSectionBlocks(sectionPos);
                Set<BlockPos> sectionPositions = cacheManager.getSectionValidPositions(
                    sectionPos, sectionBlocks, level, placementChecker);
                
                if (sectionPositions.isEmpty()) continue;

                int sectionSnowAmount = calculateSectionSnowAmount(sectionPositions.size());
                List<SnowOperation> sectionOps = generateSectionOperations(
                    sectionPositions, sectionSnowAmount, timestamp, snowedPositions);
                operations.addAll(sectionOps);

                // 限制总操作数
                if (operations.size() >= dynamicParams.maxProcessPerTick) {
                    break;
                }
            }

        } catch (Exception e) {
            ModernFurniture.LOGGER.warn("积雪算法计算出错", e);
        }

        return operations;
    }

    /**
     * 更新动态参数
     * 
     * @param currentDensity 当前密度
     * @param avgProcessTime 平均处理时间
     */
    public void updateParameters(double currentDensity, double avgProcessTime) {
        dynamicParams.updateParameters(currentDensity, avgProcessTime);
    }

    /**
     * 获取执行器
     * 
     * @return 执行器
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * 计算分区积雪数量
     * 
     * @param validPositions 有效位置数量
     * @return 积雪数量
     */
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

    /**
     * 生成分区操作
     * 
     * @param positions 位置集合
     * @param snowAmount 积雪数量
     * @param timestamp 时间戳
     * @param snowedPositions 已有积雪位置
     * @return 操作列表
     */
    private List<SnowOperation> generateSectionOperations(Set<BlockPos> positions,
                                                          int snowAmount, 
                                                          long timestamp,
                                                          Set<BlockPos> snowedPositions) {
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

    /**
     * 高级随机化
     * 
     * @param list 列表
     */
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

    /**
     * 生成泊松分布随机数
     * 
     * @param lambda 参数
     * @return 随机数
     */
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

    /**
     * 动态参数类
     */
    private static class DynamicParameters {
        private volatile double poissonLambda;
        private volatile double densityFactor;
        private volatile double spatialVariance;
        private volatile int maxProcessPerTick;

        DynamicParameters(int roomSize) {
            updateParameters(0, 0);
            this.maxProcessPerTick = Math.max(10, roomSize / 10);
        }

        void updateParameters(double currentDensity, double avgProcessTime) {
            // 动态调整泊松参数
            double baseLambda = Math.log(maxProcessPerTick + 1) * 0.5;
            double densityAdjustment = Math.max(0.1, 1.0 - currentDensity * 0.5);
            this.poissonLambda = Math.max(0.5, Math.min(8.0, baseLambda * densityAdjustment));

            // 动态调整密度因子
            this.densityFactor = Math.max(0.1, Math.min(2.0, 1.0 + Math.sin(System.currentTimeMillis() * 0.001) * 0.3));

            // 动态调整空间方差
            this.spatialVariance = Math.max(0.5, Math.min(2.0, 1.0 + currentDensity * 0.5));

            // 动态调整每tick处理量
            if (avgProcessTime > 50) { // 如果处理时间超过50ms
                this.maxProcessPerTick = Math.max(10, (int) (this.maxProcessPerTick * 0.8));
            } else if (avgProcessTime < 10) { // 如果处理时间小于10ms
                this.maxProcessPerTick = Math.min(maxProcessPerTick * 2, (int) (this.maxProcessPerTick * 1.2));
            }
        }
    }
}