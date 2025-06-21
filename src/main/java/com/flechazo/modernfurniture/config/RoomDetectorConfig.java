package com.flechazo.modernfurniture.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 房间检测器配置
 */
public class RoomDetectorConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    public static final ForgeConfigSpec.IntValue MAX_ROOM_VOLUME;
    public static final ForgeConfigSpec.IntValue MAX_RADIUS;
    public static final ForgeConfigSpec.LongValue MAX_TIME_MS;
    
    static {
        BUILDER.push("房间检测器设置");
        
        MAX_ROOM_VOLUME = BUILDER
                .comment("房间最大体积限制（方块数）")
                .defineInRange("maxRoomVolume", 500_000, 1000, 10_000_000);
                
        MAX_RADIUS = BUILDER
                .comment("检测半径限制")
                .defineInRange("maxRadius", 256, 16, 1024);
                
        MAX_TIME_MS = BUILDER
                .comment("检测超时时间（毫秒）")
                .defineInRange("maxTimeMs", 15_000L, 1000L, 120_000L);
                
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
    /**
     * 注册配置
     */
    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SPEC, "modern_furniture-common.toml");
    }
    
    /**
     * 获取房间最大体积
     */
    public static int getMaxRoomVolume() {
        return MAX_ROOM_VOLUME.get();
    }
    
    /**
     * 获取检测半径
     */
    public static int getMaxRadius() {
        return MAX_RADIUS.get();
    }
    
    /**
     * 获取检测超时时间
     */
    public static long getMaxTimeMs() {
        return MAX_TIME_MS.get();
    }
}