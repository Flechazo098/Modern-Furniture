package com.flechazo.modernfurniture.client;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.manager.BlockEntityManager;
import com.flechazo.modernfurniture.client.renderer.ACOutdoorUnitBlockRenderer;
import com.flechazo.modernfurniture.client.renderer.DisplayBlockRenderer;
import com.flechazo.modernfurniture.client.renderer.LaptopBlockRenderer;
import com.flechazo.modernfurniture.client.renderer.WallMountedAirConditioningBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = ModernFurniture.MODID, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientHandler::registerRenderers);
    }

    private static void registerRenderers() {
        BlockEntityRenderers.register(BlockEntityManager.DISPLAY_BLOCK_ENTITY.get(), context -> new DisplayBlockRenderer());
        BlockEntityRenderers.register(BlockEntityManager.LAPTOP_BLOCK_ENTITY.get(), context -> new LaptopBlockRenderer());
        BlockEntityRenderers.register(BlockEntityManager.WALL_MOUNTED_AIR_CONDITIONING_BLOCK_ENTITY.get(), context -> new WallMountedAirConditioningBlockRenderer());
        BlockEntityRenderers.register(BlockEntityManager.AC_OUTDOOR_UNIT_BLOCK_ENTITY.get(), context -> new ACOutdoorUnitBlockRenderer());
    }
}