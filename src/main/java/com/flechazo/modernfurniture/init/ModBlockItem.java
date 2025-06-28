package com.flechazo.modernfurniture.init;

import com.flechazo.modernfurniture.ModernFurniture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModernFurniture.MODID);

    public static final RegistryObject<Item> BLACK_DISPLAY_ITEM = ITEMS.register("black_display",
            () -> new BlockItem(ModBlocks.BLACK_DISPLAY.get(), new Item.Properties()));

    public static final RegistryObject<Item> WHITE_DISPLAY_ITEM = ITEMS.register("white_display",
            () -> new BlockItem(ModBlocks.WHITE_DISPLAY.get(), new Item.Properties()));

    public static final RegistryObject<Item> LAPTOP_ITEM = ITEMS.register("laptop",
            () -> new BlockItem(ModBlocks.LAPTOP.get(), new Item.Properties()));

    public static final RegistryObject<Item> WALL_MOUNTED_AIR_CONDITIONING_ITEM = ITEMS.register("wall_mounted_air_conditioning",
            () -> new BlockItem(ModBlocks.WALL_MOUNTED_AIR_CONDITIONING.get(), new Item.Properties()));

    public static final RegistryObject<Item> AC_OUTDOOR_UNIT_ITEM = ITEMS.register("ac_outdoor_unit",
            () -> new BlockItem(ModBlocks.AC_OUTDOOR_UNIT.get(), new Item.Properties()));
}
