package com.flechazo.modernfurniture.init;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.DisplayBlockEntity;
import com.flechazo.modernfurniture.block.LaptopBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ModernFurniture.MODID);

    public static final RegistryObject<BlockEntityType<DisplayBlockEntity>> DISPLAY_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("display_block_entity", () ->
            BlockEntityType.Builder.of(DisplayBlockEntity::new, 
                ModBlocks.BLACK_DISPLAY.get(),
                ModBlocks.WHITE_DISPLAY.get()).build(null));
                
    public static final RegistryObject<BlockEntityType<LaptopBlockEntity>> LAPTOP_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("laptop_block_entity", () ->
            BlockEntityType.Builder.of(LaptopBlockEntity::new, 
                ModBlocks.LAPTOP.get()).build(null));
}