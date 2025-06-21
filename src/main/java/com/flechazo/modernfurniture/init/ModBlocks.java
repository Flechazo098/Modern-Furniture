package com.flechazo.modernfurniture.init;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.block.BlackDisplayBlock;
import com.flechazo.modernfurniture.block.WhiteDisplayBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            ModernFurniture.MODID);

    public static final RegistryObject<Block> BLACK_DISPLAY_BLOCK = BLOCKS.register("black_display_block", BlackDisplayBlock::new);
    public static final RegistryObject<Block> WHITE_DISPLAY_BLOCK = BLOCKS.register("white_display_block", WhiteDisplayBlock::new);
}
