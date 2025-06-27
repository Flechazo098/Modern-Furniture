package com.flechazo.modernfurniture.util.snow;

import com.flechazo.modernfurniture.ModernFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 积雪操作执行器
 * 
 * <p>负责执行具体的积雪放置、移除、层级管理等操作。</p>
 * 
 * <h2>主要功能</h2>
 * <ul>
 *   <li>积雪方块的放置和移除</li>
 *   <li>积雪层级管理</li>
 *   <li>积雪状态跟踪</li>
 * </ul>
 */
public class SnowOperationExecutor {
    private final ServerLevel level;
    private final Map<BlockPos, Integer> snowLayers;
    private final Set<BlockPos> snowedPositions;

    /**
     * 构造操作执行器
     * 
     * @param level 服务器世界
     */
    public SnowOperationExecutor(ServerLevel level) {
        this.level = level;
        this.snowLayers = new ConcurrentHashMap<>();
        this.snowedPositions = ConcurrentHashMap.newKeySet();
    }

    /**
     * 执行积雪操作列表
     * 
     * @param operations 操作列表
     */
    public void executeOperations(List<SnowOperation> operations) {
        for (SnowOperation op : operations) {
            try {
                switch (op.type()) {
                    case PLACE_NEW -> {
                        BlockState currentState = level.getBlockState(op.pos());
                        if (currentState.isAir()) {
                            placeInitialSnow(op.pos());
                        }
                    }
                    case ADD_LAYER -> {
                        BlockState currentState = level.getBlockState(op.pos());
                        if (currentState.getBlock() == Blocks.SNOW) {
                            addSnowLayer(op.pos());
                        }
                    }
                }
            } catch (Exception e) {
                ModernFurniture.LOGGER.warn("执行降雪操作失败: {}", op.pos(), e);
            }
        }
    }

    /**
     * 清除所有积雪
     */
    public void clearAllSnow() {
        for (BlockPos pos : new HashSet<>(snowedPositions)) {
            BlockState currentState = level.getBlockState(pos);
            if (currentState.getBlock() == Blocks.SNOW) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        
        snowedPositions.clear();
        snowLayers.clear();
    }

    /**
     * 获取积雪位置集合
     * 
     * @return 积雪位置集合
     */
    public Set<BlockPos> getSnowedPositions() {
        return new HashSet<>(snowedPositions);
    }

    /**
     * 获取积雪位置数量
     * 
     * @return 积雪位置数量
     */
    public int getSnowedPositionsCount() {
        return snowedPositions.size();
    }

    /**
     * 获取积雪层级映射
     * 
     * @return 积雪层级映射
     */
    public Map<BlockPos, Integer> getSnowLayers() {
        return new HashMap<>(snowLayers);
    }

    /**
     * 放置初始积雪
     * 
     * @param pos 位置
     * @return 是否成功放置
     */
    private boolean placeInitialSnow(BlockPos pos) {
        BlockState snowState = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
        if (level.setBlock(pos, snowState, 3)) {
            snowedPositions.add(pos);
            snowLayers.put(pos, 1);
            return true;
        }
        return false;
    }

    /**
     * 添加积雪层
     * 
     * @param pos 位置
     * @return 是否成功添加
     */
    private boolean addSnowLayer(BlockPos pos) {
        BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() == Blocks.SNOW) {
            int currentLayers = currentState.getValue(SnowLayerBlock.LAYERS);
            if (currentLayers < 8) {
                int newLayers = currentLayers + 1;
                BlockState newState = currentState.setValue(SnowLayerBlock.LAYERS, newLayers);
                if (level.setBlock(pos, newState, 3)) {
                    snowLayers.put(pos, newLayers);
                    return true;
                }
            }
        }
        return false;
    }
}