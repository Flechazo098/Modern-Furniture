package com.flechazo.modernfurniture.util.snow;

import net.minecraft.core.BlockPos;

/**
 * 积雪操作记录
 *
 * @param pos 积雪操作的目标位置
 * @param type 操作类型（放置新雪或添加层级）
 * @param layers 积雪层数（用于新雪放置）
 * @param timestamp 操作时间戳
 */
public record SnowOperation(BlockPos pos, OperationType type, int layers, long timestamp) {
    
    /**
     * 积雪操作类型枚举
     */
    public enum OperationType {
        /**
         * 放置新的积雪方块
         */
        PLACE_NEW,
        
        /**
         * 在现有积雪上添加层级
         */
        ADD_LAYER
    }
}