package com.flechazo.modernfurniture.event;

import com.flechazo.modernfurniture.command.ConfigCommand;
import com.flechazo.modernfurniture.event.handler.WireEventHandler;
import net.minecraftforge.common.MinecraftForge;

public class EventManager {
    public static void register() {
        // MinecraftForge.EVENT_BUS.register(SnowEventHandler.class); // Use SnowManager to manage this
        MinecraftForge.EVENT_BUS.register(WireEventHandler.class);

        // Command register
        MinecraftForge.EVENT_BUS.register(ConfigCommand.class);
    }
}
