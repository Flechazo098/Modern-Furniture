package com.flechazo.modernfurniture.block.manager;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.entity.ACOutdoorUnitBlockEntity;
import com.flechazo.modernfurniture.block.entity.DisplayBlockEntity;
import com.flechazo.modernfurniture.block.entity.LaptopBlockEntity;
import com.flechazo.modernfurniture.block.entity.WallMountedAirConditioningBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityManager {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ModernFurniture.MODID);

    public static final RegistryObject<BlockEntityType<DisplayBlockEntity>> DISPLAY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("display_block_entity", () ->
                    BlockEntityType.Builder.of(DisplayBlockEntity::new,
                            BlockManager.BLACK_DISPLAY.get(),
                            BlockManager.WHITE_DISPLAY.get()).build(null));

    public static final RegistryObject<BlockEntityType<LaptopBlockEntity>> LAPTOP_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("laptop_block_entity", () ->
                    BlockEntityType.Builder.of(LaptopBlockEntity::new,
                            BlockManager.LAPTOP.get()).build(null));

    public static final RegistryObject<BlockEntityType<WallMountedAirConditioningBlockEntity>> WALL_MOUNTED_AIR_CONDITIONING_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("wall_mounted_air_conditioning_block_entity", () ->
                    BlockEntityType.Builder.of(WallMountedAirConditioningBlockEntity::new,
                            BlockManager.WALL_MOUNTED_AIR_CONDITIONING.get()).build(null));

    public static final RegistryObject<BlockEntityType<ACOutdoorUnitBlockEntity>> AC_OUTDOOR_UNIT_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("ac_outdoor_unit_block_entity", () ->
                    BlockEntityType.Builder.of(ACOutdoorUnitBlockEntity::new,
                            BlockManager.AC_OUTDOOR_UNIT.get()).build(null));
}