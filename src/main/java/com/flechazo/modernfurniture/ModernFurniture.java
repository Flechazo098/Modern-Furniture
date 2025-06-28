package com.flechazo.modernfurniture;

import com.flechazo.modernfurniture.block.manager.BlockEntityManager;
import com.flechazo.modernfurniture.block.manager.BlockManager;
import com.flechazo.modernfurniture.config.ConfigManager;
import com.flechazo.modernfurniture.event.EventManager;
import com.flechazo.modernfurniture.item.manager.BlockItemManager;
import com.flechazo.modernfurniture.item.manager.ItemCreativeTabManager;
import com.flechazo.modernfurniture.item.manager.ItemManager;
import com.flechazo.modernfurniture.network.NetworkHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ModernFurniture.MODID)
public class ModernFurniture {
    public static final String MODID = "modern_furniture";
    public static final Logger LOGGER = LogManager.getLogger();

    public ModernFurniture(FMLJavaModLoadingContext context) {
        var modEventBus = context.getModEventBus();

        BlockManager.BLOCKS.register(modEventBus);
        BlockItemManager.ITEMS.register(modEventBus);
        BlockEntityManager.BLOCK_ENTITIES.register(modEventBus);
        ItemManager.ITEMS.register(modEventBus);
        ItemCreativeTabManager.CREATIVE_MODE_TABS.register(modEventBus);

        EventManager.register();
        ConfigManager.register(context);
        NetworkHandler.register(modEventBus);
    }
}