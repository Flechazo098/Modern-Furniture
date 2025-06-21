package com.flechazo.modernfurniture.client;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.client.renderer.DisplayBlockRenderer;
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
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(
                    ModBlockEntities.DISPLAY_BLOCK_ENTITY.get(),
                    context -> new DisplayBlockRenderer()
            );
        });
    }
}