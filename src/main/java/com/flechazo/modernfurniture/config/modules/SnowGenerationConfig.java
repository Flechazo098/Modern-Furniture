package com.flechazo.modernfurniture.config.modules;

import com.flechazo.modernfurniture.config.ConfigModule;
import com.flechazo.modernfurniture.config.flags.ConfigInfo;
import com.flechazo.modernfurniture.config.flags.RangeFlag;

public class SnowGenerationConfig implements ConfigModule {
    @Override
    public String name() {
        return "Snow Generation";
    }

    @ConfigInfo(name = "enableSnow", comment = "是否在低温房间生成积雪")
    public static boolean enableSnow = true;

    @ConfigInfo(name = "snowDelayTicks", comment = "积雪生成周期间隔（20刻=1秒）")
    @RangeFlag(min = "100", max = "72000")
    public static long snowDelayTicks = 6000;

    @ConfigInfo(name = "snowProbability", comment = "空中方块生成积雪的概率（0=从不，1=总是）应该没人会喜欢这个")
    @RangeFlag(min = "0", max = "1")
    public static double snowProbability = 0;

    @ConfigInfo(name = "groundSnowProbability", comment = "地面方块生成积雪的概率（0=从不，1=总是）")
    @RangeFlag(min = "0", max = "1")
    public static double groundSnowProbability = 0.54;

    @ConfigInfo(name = "maxSnowCycles", comment = "最大积雪生成周期数（-1=无限）")
    @RangeFlag(min = "-1", max = "100")
    public static int maxSnowCycles = -1;

    @ConfigInfo(name = "snowCoverageRatio", comment = "积雪覆盖的方块比例房间最大积雪覆盖率")
    @RangeFlag(min = "0", max = "1")
    public static double snowCoverageRatio = 0.7;
}
