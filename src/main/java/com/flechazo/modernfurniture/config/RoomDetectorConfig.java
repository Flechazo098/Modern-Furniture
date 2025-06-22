package com.flechazo.modernfurniture.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class RoomDetectorConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 房间检测配置
    public static final ForgeConfigSpec.IntValue MAX_ROOM_SIZE;
    public static final ForgeConfigSpec.IntValue MAX_SEARCH_DISTANCE;
    public static final ForgeConfigSpec.IntValue MAX_SEARCH_TIME_MS;

    // 降雪配置
    public static final ForgeConfigSpec.BooleanValue ENABLE_SNOW;
    public static final ForgeConfigSpec.LongValue SNOW_DELAY_TICKS;
    public static final ForgeConfigSpec.DoubleValue SNOW_PROBABILITY;
    public static final ForgeConfigSpec.DoubleValue GROUND_SNOW_PROBABILITY;
    public static final ForgeConfigSpec.IntValue MAX_SNOW_CYCLES;
    public static final ForgeConfigSpec.DoubleValue SNOW_COVERAGE_RATIO;

    static {
        // 开始"房间检测"配置组
        BUILDER.push("Room Detection");

        // 最大房间尺寸（单位：方块数）
        MAX_ROOM_SIZE = BUILDER
                .comment("能被识别为房间的最大方块数量")
                .defineInRange("maxRoomSize", 100000, 10, 10000);  // 默认1000，范围10-10000

        // 最大搜索距离（单位：方块）
        MAX_SEARCH_DISTANCE = BUILDER
                .comment("搜索房间边界的最大距离")
                .defineInRange("maxSearchDistance", 100, 5, 200);  // 默认50，范围5-200

        // 最大搜索时间（单位：毫秒）
        MAX_SEARCH_TIME_MS = BUILDER
                .comment("房间检测的最大耗时（毫秒）")
                .defineInRange("maxSearchTimeMs", 3000, 10, 1000);  // 默认100ms，范围10-1000ms

        BUILDER.pop();  // 结束"房间检测"配置组

        // 开始"积雪生成"配置组
        BUILDER.push("Snow Generation");

        // 是否启用积雪功能
        ENABLE_SNOW = BUILDER
                .comment("是否在低温房间生成积雪")
                .define("enableSnow", true);  // 默认开启

        // 积雪生成间隔（单位：游戏刻，20刻=1秒）
        SNOW_DELAY_TICKS = BUILDER
                .comment("积雪生成周期间隔（20刻=1秒）")
                .defineInRange("snowDelayTicks", 6000L, 100L, 72000L);  // 默认5分钟（6000刻），范围5秒-1小时

        // 空中方块积雪概率
        SNOW_PROBABILITY = BUILDER
                .comment("空中方块生成积雪的概率（0=从不，1=总是）应该没人会喜欢这个")
                .defineInRange("snowProbability", 0.0, 0.0, 1.0);  // 默认0%，范围0-100%

        // 地面方块积雪概率
        GROUND_SNOW_PROBABILITY = BUILDER
                .comment("地面方块生成积雪的概率（0=从不，1=总是）")
                .defineInRange("groundSnowProbability", 0.54, 0.0, 1.0);  // 默认30%，范围0-100%

        // 最大积雪周期数
        MAX_SNOW_CYCLES = BUILDER
                .comment("最大积雪生成周期数（-1=无限）")
                .defineInRange("maxSnowCycles", -1, -1, 100);  // 默认无限，范围-1到100

        // 最大积雪覆盖率
        SNOW_COVERAGE_RATIO = BUILDER
                .comment("房间最大积雪覆盖率（0.0-1.0）")
                .defineInRange("snowCoverageRatio", 0.7, 0.1, 1.0);  // 默认70%，范围10%-100%

        BUILDER.pop();

        // 构建最终配置规范
        SPEC = BUILDER.build();
    }

    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    // 房间检测方法
    public static int getMaxRoomSize() {
        return MAX_ROOM_SIZE.get();
    }

    public static int getMaxSearchDistance() {
        return MAX_SEARCH_DISTANCE.get();
    }

    public static int getMaxSearchTimeMs() {
        return MAX_SEARCH_TIME_MS.get();
    }

    // 降雪方法
    public static boolean isSnowEnabled() {
        return ENABLE_SNOW.get();
    }

    public static long getSnowDelayTicks() {
        return SNOW_DELAY_TICKS.get();
    }

    public static double getSnowProbability() {
        return SNOW_PROBABILITY.get();
    }

    public static double getGroundSnowProbability() {
        return GROUND_SNOW_PROBABILITY.get();
    }
    
    public static int getMaxSnowCycles() {
        return MAX_SNOW_CYCLES.get();
    }
    
    public static double getSnowCoverageRatio() {
        return SNOW_COVERAGE_RATIO.get();
    }
}