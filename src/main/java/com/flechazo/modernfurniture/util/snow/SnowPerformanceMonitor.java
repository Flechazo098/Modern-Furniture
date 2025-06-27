package com.flechazo.modernfurniture.util.snow;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 积雪性能监控器
 * 
 * <p>监控积雪系统的性能指标，包括处理时间、内存使用、操作统计等。</p>
 * 
 * <h2>主要功能</h2>
 * <ul>
 *   <li>操作性能统计</li>
 *   <li>内存使用监控</li>
 *   <li>系统负载评估</li>
 * </ul>
 */
public class SnowPerformanceMonitor {
    private static final long MEMORY_CHECK_INTERVAL = 10000; // 10秒检查一次内存
    
    private final AtomicLong totalOperations = new AtomicLong();
    private final AtomicLong totalProcessTime = new AtomicLong();
    private final Queue<Long> recentTimes = new ConcurrentLinkedQueue<>();
    private final MemoryMXBean memoryBean;
    private final ScheduledExecutorService scheduledExecutor;
    
    private long lastMemoryCheck = 0;

    /**
     * 构造性能监控器
     */
    public SnowPerformanceMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.scheduledExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "SnowPerformance-Monitor");
            t.setDaemon(true);
            return t;
        });
        
        // 启动定期清理任务
        scheduledExecutor.scheduleAtFixedRate(this::performMaintenance, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 记录操作性能
     * 
     * @param processTimeMs 处理时间（毫秒）
     */
    public void recordOperation(long processTimeMs) {
        totalOperations.incrementAndGet();
        totalProcessTime.addAndGet(processTimeMs);
        recentTimes.offer(processTimeMs);

        // 保持最近100次记录
        while (recentTimes.size() > 100) {
            recentTimes.poll();
        }
    }

    /**
     * 获取平均处理时间
     * 
     * @return 平均处理时间（毫秒）
     */
    public double getAverageProcessTime() {
        if (recentTimes.isEmpty()) {
            return 0;
        }
        return recentTimes.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    /**
     * 获取当前密度
     * 
     * @param roomSize 房间大小
     * @return 当前密度
     */
    public double getCurrentDensity(int roomSize) {
        if (roomSize == 0) {
            return 0;
        }
        return (double) totalOperations.get() / roomSize;
    }

    /**
     * 获取内存使用量
     * 
     * @return 内存使用量（字节）
     */
    public long getMemoryUsage() {
        return memoryBean.getHeapMemoryUsage().getUsed();
    }

    /**
     * 检查是否应该因内存压力跳过处理
     * 
     * @return 是否应该跳过
     */
    public boolean shouldSkipDueToMemory() {
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

    /**
     * 关闭性能监控器
     */
    public void shutdown() {
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
     * 执行维护任务
     */
    private void performMaintenance() {
        // 清理过期的性能记录
        while (recentTimes.size() > 100) {
            recentTimes.poll();
        }
    }
}