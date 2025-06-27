package com.flechazo.modernfurniture.init;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.BlackDisplayBlock;
import com.flechazo.modernfurniture.block.LaptopBlock;
import com.flechazo.modernfurniture.block.WallMountedAirConditioningBlock;
import com.flechazo.modernfurniture.block.WhiteDisplayBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            ModernFurniture.MODID);

    public static final RegistryObject<Block> BLACK_DISPLAY = BLOCKS.register("black_display", BlackDisplayBlock::new);
    public static final RegistryObject<Block> WHITE_DISPLAY = BLOCKS.register("white_display", WhiteDisplayBlock::new);
    public static final RegistryObject<Block> LAPTOP = BLOCKS.register("laptop", LaptopBlock::new);
    public static final RegistryObject<Block> WALL_MOUNTED_AIR_CONDITIONING = BLOCKS.register("wall_mounted_air_conditioning", WallMountedAirConditioningBlock::new);
}
