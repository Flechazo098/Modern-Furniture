package com.flechazo.modernfurniture.init;

import com.flechazo.modernfurniture.ModernFurniture;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModernFurniture.MODID);

    public static final RegistryObject<CreativeModeTab> MODERN_FURNITURE_TAB = CREATIVE_MODE_TABS.register("modern_furniture_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.modern_furniture"))
                    .icon(() -> new ItemStack(ModBlockItem.BLACK_DISPLAY_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlockItem.BLACK_DISPLAY_ITEM.get());
                        output.accept(ModBlockItem.WHITE_DISPLAY_ITEM.get());
                        output.accept(ModBlockItem.LAPTOP_ITEM.get());
                        output.accept(ModBlockItem.WALL_MOUNTED_AIR_CONDITIONING_ITEM.get());
                        output.accept(ModBlockItem.AC_OUTDOOR_UNIT_ITEM.get());
                        output.accept(ModItem.WIRE_CONNECTOR.get());
                    })
                    .build());
}