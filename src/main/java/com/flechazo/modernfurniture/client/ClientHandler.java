package com.flechazo.modernfurniture.client;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.client.renderer.ACOutdoorUnitBlockRenderer;
import com.flechazo.modernfurniture.client.renderer.DisplayBlockRenderer;
import com.flechazo.modernfurniture.client.renderer.LaptopBlockRenderer;
import com.flechazo.modernfurniture.client.renderer.WallMountedAirConditioningBlockRenderer;
import com.flechazo.modernfurniture.init.ModBlockEntities;
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
        BlockEntityRenderers.register(ModBlockEntities.DISPLAY_BLOCK_ENTITY.get(), context -> new DisplayBlockRenderer());
        BlockEntityRenderers.register(ModBlockEntities.LAPTOP_BLOCK_ENTITY.get(), context -> new LaptopBlockRenderer());
        BlockEntityRenderers.register(ModBlockEntities.WALL_MOUNTED_AIR_CONDITIONING_BLOCK_ENTITY.get(), context -> new WallMountedAirConditioningBlockRenderer());
        BlockEntityRenderers.register(ModBlockEntities.AC_OUTDOOR_UNIT_BLOCK_ENTITY.get(), context -> new ACOutdoorUnitBlockRenderer());
    }
}