package com.flechazo.modernfurniture.util.snow;

import com.flechazo.modernfurniture.config.modules.SnowGenerationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 积雪放置检查器
 * 
 * <p>负责检查指定位置是否可以放置积雪，包括各种条件判断和概率计算。</p>
 * 
 * <h2>主要功能</h2>
 * <ul>
 *   <li>积雪放置条件检查</li>
 *   <li>概率计算</li>
 *   <li>积雪层级验证</li>
 * </ul>
 */
public class SnowPlacementChecker {
    private final RandomSource random;

    /**
     * 构造积雪放置检查器
     * 
     * @param random 随机数生成器
     */
    public SnowPlacementChecker(RandomSource random) {
        this.random = random;
    }

    /**
     * 检查是否可以放置积雪
     * 
     * @param pos 位置
     * @param currentState 当前方块状态
     * @param level 服务器世界
     * @return 是否可以放置
     */
    public boolean canPlaceSnow(BlockPos pos, BlockState currentState, ServerLevel level) {
        if (currentState.isAir()) {
            return canPlaceSnowOnAir(pos, level);
        }

        if (currentState.getBlock() == Blocks.SNOW) {
            return canAddSnowLayer(pos, currentState);
        }

        return false;
    }

    /**
     * 检查是否可以在空气中放置积雪
     * 
     * @param pos 位置
     * @param level 服务器世界
     * @return 是否可以放置
     */
    private boolean canPlaceSnowOnAir(BlockPos pos, ServerLevel level) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);

        // 地面积雪逻辑
        if (!belowState.isAir() && belowState.isSolidRender(level, belowPos)) {
            double probability = SnowGenerationConfig.groundSnowProbability;
            return random.nextFloat() < probability;
        } 
        // 空中积雪逻辑
        else {
            double probability = SnowGenerationConfig.snowProbability * 0.5;
            return random.nextFloat() < probability;
        }
    }

    /**
     * 检查是否可以添加积雪层
     * 
     * @param pos 位置
     * @param currentState 当前状态
     * @return 是否可以添加
     */
    private boolean canAddSnowLayer(BlockPos pos, BlockState currentState) {
        if (currentState.getBlock() == Blocks.SNOW) {
            int currentLayers = currentState.getValue(SnowLayerBlock.LAYERS);
            return currentLayers < 8;
        }
        return false;
    }
}