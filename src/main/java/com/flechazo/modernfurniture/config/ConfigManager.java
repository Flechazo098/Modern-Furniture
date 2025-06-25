package com.flechazo.modernfurniture.config;

import com.flechazo.modernfurniture.config.flags.ConfigInfo;
import com.flechazo.modernfurniture.config.flags.DoNotLoad;
import com.flechazo.modernfurniture.config.flags.RangeFlag;
import com.flechazo.modernfurniture.util.ClassLoader;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class ConfigManager {
    private static final Set<ConfigModule> configModules = new HashSet<>();
    private static final Logger LOGGER = LogManager.getLogger();

    public static void register(FMLJavaModLoadingContext context) {
        ForgeConfigSpec spec = loadConfig();
        context.registerConfig(ModConfig.Type.COMMON, spec);
    }

    private static ForgeConfigSpec loadConfig() {
        configModules.addAll(ClassLoader.loadClasses("com.flechazo.modernfurniture.config.modules", ConfigModule.class));
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        for (ConfigModule module : configModules) {
            Field[] fields = module.getClass().getDeclaredFields();

            builder.push(module.name());

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

                        if (type == boolean.class) {
                            boolean value = builder.comment(configInfo.comment())
                                    .define(configInfo.name(), (boolean) field.get(null))
                                    .get();
                            field.set(null, value);
                        } else if (type == int.class && rangeFlag != null) {
                            int value = builder.comment(configInfo.comment())
                                    .defineInRange(configInfo.name(), (int) field.get(null), Integer.parseInt(rangeFlag.min()), Integer.parseInt(rangeFlag.max()))
                                    .get();
                            field.set(null, value);
                        } else if (type == double.class && rangeFlag != null) {
                            double value = builder.comment(configInfo.comment())
                                    .defineInRange(configInfo.name(), (double) field.get(null), Double.parseDouble(rangeFlag.min()), Double.parseDouble(rangeFlag.max()))
                                    .get();
                            field.set(null, value);
                        } else if (type == long.class && rangeFlag != null) {
                            long value = builder.comment(configInfo.comment())
                                    .defineInRange(configInfo.name(), (long) field.get(null), Long.parseLong(rangeFlag.min()), Long.parseLong(rangeFlag.max()))
                                    .get();
                            field.set(null, value);
                        }
                    } catch (IllegalAccessException e) {
                        LOGGER.error("Error loading config field: {}", field.getName());
                    } catch (NullPointerException ignored) {
                    }
                }
            }
            builder.pop();
        }
        return builder.build();
    }
}
