package com.flechazo.modernfurniture.config;

import com.flechazo.modernfurniture.config.flags.ConfigInfo;
import com.flechazo.modernfurniture.config.flags.DoNotLoad;
import com.flechazo.modernfurniture.config.flags.RangeFlag;
import com.flechazo.modernfurniture.util.ClassLoader;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigManager {
    public static final Map<ForgeConfigSpec.ConfigValue, Field> map = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();

    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, init());
        context.getModEventBus().addListener(ConfigManager::onConfigLoad);
    }

    private static void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            load(); // need to load after this is loaded
        }
    }

    private static ForgeConfigSpec init() {
        // first load all modules
        final Set<ConfigModule> configModules = new HashSet<>(ClassLoader.loadClasses("com.flechazo.modernfurniture.config.modules", ConfigModule.class));
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        for (ConfigModule module : configModules) {
            Field[] fields = module.getClass().getDeclaredFields();

            builder.push(module.name());

            // load single instance field
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
                    boolean skipLoad = field.getAnnotation(DoNotLoad.class) != null;
                    ConfigInfo configInfo = field.getAnnotation(ConfigInfo.class);
                    RangeFlag rangeFlag = field.getAnnotation(RangeFlag.class);

                    if (skipLoad || configInfo == null) {
                        continue;
                    }

                    field.setAccessible(true);

                    try {
                        Class<?> type = field.getType();

                        ForgeConfigSpec.ConfigValue value = null;

                        if (type == boolean.class) {
                            value = builder.comment(configInfo.comment())
                                    .define(configInfo.name(), (boolean) field.get(null));
                        } else if (type == int.class && rangeFlag != null) {
                            value = builder.comment(configInfo.comment())
                                    .defineInRange(configInfo.name(), (int) field.get(null), Integer.parseInt(rangeFlag.min()), Integer.parseInt(rangeFlag.max()));
                        } else if (type == double.class && rangeFlag != null) {
                            value = builder.comment(configInfo.comment())
                                    .defineInRange(configInfo.name(), (double) field.get(null), Double.parseDouble(rangeFlag.min()), Double.parseDouble(rangeFlag.max()));
                        } else if (type == long.class && rangeFlag != null) {
                            value = builder.comment(configInfo.comment())
                                    .defineInRange(configInfo.name(), (long) field.get(null), Long.parseLong(rangeFlag.min()), Long.parseLong(rangeFlag.max()));
                        }
                        map.put(value, field); // put into map - wait for next process
                    } catch (IllegalAccessException e) {
                        LOGGER.error("Error loading config field: {}", field.getName());
                    }
                }
            }
            builder.pop();
        }
        return builder.build();
    }

    public static void load() { // load all fields
        map.forEach((value, field) -> {
            field.setAccessible(true);
            try {
                if (value != null)
                    field.set(null, value.get());
            } catch (IllegalAccessException e) {
                LOGGER.error("Error setting value to config field: {}", field.getName());
            }
        });
    }

    public static void syncValueFromServer(Map<String, Object> serverConfig) {
        map.forEach((value, field) -> {
            field.setAccessible(true);
            try {
                if (value != null) {
                    Object newValue = serverConfig.get(field.getName());
                    if (newValue == null) newValue = value.getDefault();
                    field.set(null, newValue);
                }
            } catch (IllegalAccessException e) {
                LOGGER.error("Error sync config field: {}", field.getName());
            }
        });
    }
}
