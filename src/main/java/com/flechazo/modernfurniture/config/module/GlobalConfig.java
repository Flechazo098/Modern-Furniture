package com.flechazo.modernfurniture.config.module;

import com.flechazo.modernfurniture.config.ConfigModule;
import com.flechazo.modernfurniture.config.flag.ConfigInfo;
import com.flechazo.modernfurniture.config.flag.DoNotSync;

public class GlobalConfig implements ConfigModule {
    @DoNotSync
    @ConfigInfo(name = "syncDataFromServer", comment = "是否启用服务器数据同步，关闭将使用本地数据（强制推送除外）")
    public static boolean syncDataFromServer = true;

    @DoNotSync
    @ConfigInfo(name = "enforceServerConfigDataSync", comment = "服务是否要求强制推送数据，启用客户端将强制使用来自服务端的数据")
    public static boolean enforceServerConfigDataSync = false;

    @Override
    public String name() {
        return "Global Config";
    }
}
