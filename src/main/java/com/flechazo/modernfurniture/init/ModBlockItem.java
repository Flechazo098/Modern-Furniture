package com.flechazo.modernfurniture.init;

import com.flechazo.modernfurniture.ModernFurniture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModernFurniture.MODID);

    public static final RegistryObject<Item> BLACK_DISPLAY_BLOCK_ITEM = ITEMS.register("black_display_block",
            () -> new BlockItem(ModBlocks.BLACK_DISPLAY_BLOCK.get(), new Item.Properties()));
    
    public static final RegistryObject<Item> WHITE_DISPLAY_BLOCK_ITEM = ITEMS.register("white_display_block",
            () -> new BlockItem(ModBlocks.WHITE_DISPLAY_BLOCK.get(), new Item.Properties()));
}
