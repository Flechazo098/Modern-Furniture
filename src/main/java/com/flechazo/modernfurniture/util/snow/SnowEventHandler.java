package com.flechazo.modernfurniture.util.snow;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;

/**
 * 积雪事件处理器
 *
 * <p>监听方块变化事件，触发相应的缓存失效和状态更新。</p>
 *
 * <h2>主要功能</h2>
 * <ul>
 *   <li>监听方块放置/破坏事件</li>
 *   <li>触发缓存失效</li>
 *   <li>维护积雪状态一致性</li>
 * </ul>
 */
public class SnowEventHandler {
    private final ServerLevel level;
    private final Set<BlockPos> roomBlocks;
    private final SnowCacheManager cacheManager;

    /**
     * 构造事件处理器
     *
     * @param level        服务器世界
     * @param roomBlocks   房间方块集合
     * @param cacheManager 缓存管理器
     */
    public SnowEventHandler(ServerLevel level, Set<BlockPos> roomBlocks, SnowCacheManager cacheManager) {
        this.level = level;
        this.roomBlocks = roomBlocks;
        this.cacheManager = cacheManager;

        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 处理方块放置事件
     *
     * @param event 方块放置事件
     */
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() == level) {
            BlockPos pos = event.getPos();
            if (isInRoomArea(pos)) {
                cacheManager.scheduleInvalidation(pos, "block_place");
            }
        }
    }

    /**
     * 处理方块破坏事件
     *
     * @param event 方块破坏事件
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() == level) {
            BlockPos pos = event.getPos();
            if (isInRoomArea(pos)) {
                cacheManager.scheduleInvalidation(pos, "block_break");
            }
        }
    }

    /**
     * 关闭事件处理器
     */
    public void shutdown() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    /**
     * 检查位置是否在房间区域内
     *
     * @param pos 位置
     * @return 是否在房间区域内
     */
    private boolean isInRoomArea(BlockPos pos) {
        return roomBlocks.contains(pos) ||
                roomBlocks.stream().anyMatch(roomPos -> roomPos.distSqr(pos) <= 9); // 3格范围内
    }
}