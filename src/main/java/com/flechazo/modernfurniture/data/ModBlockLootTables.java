package com.flechazo.modernfurniture.data;

import com.flechazo.modernfurniture.block.manager.BlockManager;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModBlockLootTables extends BlockLootSubProvider {
    public ModBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        this.dropSelf(BlockManager.BLACK_DISPLAY.get());
        this.dropSelf(BlockManager.WHITE_DISPLAY.get());
        this.dropSelf(BlockManager.LAPTOP.get());
        this.dropSelf(BlockManager.WALL_MOUNTED_AIR_CONDITIONING.get());
        this.dropSelf(BlockManager.AC_OUTDOOR_UNIT.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BlockManager.BLOCKS.getEntries().stream()
                .map(RegistryObject::get)
                .toList();
    }
}