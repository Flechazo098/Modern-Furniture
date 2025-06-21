package com.flechazo.modernfurniture.init;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.DisplayBlockEntity;
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
                ModBlocks.BLACK_DISPLAY_BLOCK.get(), 
                ModBlocks.WHITE_DISPLAY_BLOCK.get()).build(null));
}