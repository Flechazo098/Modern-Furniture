package com.flechazo.modernfurniture.config.modules;

import com.flechazo.modernfurniture.config.ConfigModule;
import com.flechazo.modernfurniture.config.flags.ConfigInfo;
import com.flechazo.modernfurniture.config.flags.RangeFlag;

public class RoomDetectionConfig implements ConfigModule {
    @ConfigInfo(name = "maxRoomSize", comment = "能被识别为房间的最大方块数量")
    @RangeFlag(min = "10", max = "100000")
    public static int maxRoomSize = 100000;
    @ConfigInfo(name = "maxSearchDistance", comment = "搜索房间边界的最大距离")
    @RangeFlag(min = "5", max = "200")
    public static int maxSearchDistance = 50;
    @ConfigInfo(name = "maxSearchTimeMs", comment = "房间检测的最大耗时（毫秒）")
    @RangeFlag(min = "10", max = "1000")
    public static long maxSearchTimeMs = 100;

    @Override
    public String name() {
        return "Room Detection";
    }
}
