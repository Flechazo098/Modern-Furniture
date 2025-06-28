package com.flechazo.modernfurniture.init;

import com.flechazo.modernfurniture.ModernFurniture;
import com.flechazo.modernfurniture.item.WireConnectorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModernFurniture.MODID);

    public static final RegistryObject<Item> WIRE_CONNECTOR = ITEMS.register("wire_connector", WireConnectorItem::new);
}
