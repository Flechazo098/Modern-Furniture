package com.flechazo.modernfurniture.util.snow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 积雪分区管理器
 * 
 * <p>负责将房间空间划分为层级化的分区，提供高效的空间索引和查询功能。</p>
 * 
 * <h2>主要功能</h2>
 * <ul>
 *   <li>空间分区划分和索引</li>
 *   <li>活跃分区管理</li>
 *   <li>分区选择策略</li>
 * </ul>
 */
public class SnowSectionManager {
    private final Map<SectionPos, Set<BlockPos>> sectionPartitions;
    private final Map<ChunkPos, Set<SectionPos>> chunkToSections;
    private final Set<SectionPos> activeSections;

    /**
     * 构造分区管理器
     * 
     * @param roomBlocks 房间方块位置集合
     */
    public SnowSectionManager(Set<BlockPos> roomBlocks) {
        this.sectionPartitions = new ConcurrentHashMap<>();
        this.chunkToSections = new ConcurrentHashMap<>();
        this.activeSections = ConcurrentHashMap.newKeySet();
        
        initializeSectionPartitions(roomBlocks);
    }

    /**
     * 获取指定分区的方块位置
     * 
     * @param sectionPos 分区位置
     * @return 方块位置集合
     */
    public Set<BlockPos> getSectionBlocks(SectionPos sectionPos) {
        return sectionPartitions.getOrDefault(sectionPos, Collections.emptySet());
    }

    /**
     * 获取所有活跃分区
     * 
     * @return 活跃分区集合
     */
    public Set<SectionPos> getActiveSections() {
        return new HashSet<>(activeSections);
    }

    /**
     * 获取活跃分区数量
     * 
     * @return 分区数量
     */
    public int getActiveSectionsCount() {
        return activeSections.size();
    }

    /**
     * 选择用于处理的分区
     * 
     * @param densityFactor 密度因子
     * @param random 随机数生成器
     * @return 选中的分区列表
     */
    public List<SectionPos> selectSectionsForProcessing(double densityFactor, Random random) {
        List<SectionPos> sections = new ArrayList<>(activeSections);
        
        // 基于负载和随机性选择分区
        Collections.shuffle(sections, random);
        
        int maxSections = Math.max(1, Math.min(sections.size(),
            (int) (sections.size() * densityFactor * 0.3)));
        
        return sections.subList(0, maxSections);
    }

    /**
     * 初始化分区划分
     */
    private void initializeSectionPartitions(Set<BlockPos> roomBlocks) {
        for (BlockPos pos : roomBlocks) {
            SectionPos sectionPos = SectionPos.of(pos);
            ChunkPos chunkPos = new ChunkPos(pos);

            sectionPartitions.computeIfAbsent(sectionPos, k -> ConcurrentHashMap.newKeySet()).add(pos);
            chunkToSections.computeIfAbsent(chunkPos, k -> ConcurrentHashMap.newKeySet()).add(sectionPos);
            activeSections.add(sectionPos);
        }
    }
}