package com.flechazo.modernfurniture.util.wire;

import com.flechazo.modernfurniture.ModernFurniture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 监听区块加载/卸载事件，动态管理连接激活
 */
@Mod.EventBusSubscriber(modid = ModernFurniture.MODID)
public class ChunkLoadMonitor {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        handleChunkEvent(event, true);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        handleChunkEvent(event, false);
    }

    private static void handleChunkEvent(ChunkEvent event, boolean isLoad) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        ChunkPos chunkPos = event.getChunk().getPos();
        BlockPos blockPos = new BlockPos(chunkPos.x << 4, 0, chunkPos.z << 4);

        WireNetworkManager manager = WireNetworkManager.get(level);

        if (isLoad) {
            manager.onChunkLoaded(level, blockPos);
        } else {
            manager.onChunkUnloaded(level, blockPos);
        }
    }
}
