package com.flechazo.modernfurniture;

import com.flechazo.modernfurniture.config.ConfigManager;
import com.flechazo.modernfurniture.init.ModBlockEntities;
import com.flechazo.modernfurniture.init.ModBlockItem;
import com.flechazo.modernfurniture.init.ModBlocks;
import com.flechazo.modernfurniture.init.ModCreativeTab;
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

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockItem.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);

        ConfigManager.register(context);
    }
}