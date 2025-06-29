package com.flechazo.modernfurniture.config;

import com.flechazo.modernfurniture.config.flag.ConfigInfo;
import com.flechazo.modernfurniture.config.flag.DoNotLoad;
import com.flechazo.modernfurniture.config.flag.RangeFlag;
import com.flechazo.modernfurniture.util.ClassLoaderUtil;
import com.mojang.datafixers.util.Pair;
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
    public static final Map<ForgeConfigSpec.ConfigValue<?>, Field> configMap = new HashMap<>();
    private static final Map<String, Object> defaultValues = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();

    private static ModConfig commonConfig;
    private static boolean isConfigLoaded = false;

    public static void register(FMLJavaModLoadingContext context) {
        ForgeConfigSpec configSpec = init();
        context.registerConfig(ModConfig.Type.COMMON, configSpec);
        context.getModEventBus().addListener(ConfigManager::onConfigLoad);
        context.getModEventBus().addListener(ConfigManager::onConfigReload);
    }

    public static void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            commonConfig = event.getConfig();
            isConfigLoaded = true;
            // 延迟加载，确保配置系统完全初始化
            loadConfigValues();
        }
    }

    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            loadConfigValues();
        }
    }

    private static ForgeConfigSpec init() {
        final Set<ConfigModule> configModules = new HashSet<>(ClassLoaderUtil.loadClasses(
                "com.flechazo.modernfurniture.config.module", ConfigModule.class));
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        for (ConfigModule module : configModules) {
            Field[] fields = module.getClass().getDeclaredFields();
            builder.push(module.name());

            for (Field field : fields) {
                if (shouldProcessField(field)) {
                    processConfigField(field, builder);
                }
            }
            builder.pop();
        }
        return builder.build();
    }

    private static boolean shouldProcessField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) &&
                !Modifier.isFinal(modifiers) &&
                field.getAnnotation(DoNotLoad.class) == null &&
                field.getAnnotation(ConfigInfo.class) != null;
    }

    private static void processConfigField(Field field, ForgeConfigSpec.Builder builder) {
        ConfigInfo configInfo = field.getAnnotation(ConfigInfo.class);
        RangeFlag rangeFlag = field.getAnnotation(RangeFlag.class);

        field.setAccessible(true);

        try {
            Class<?> type = field.getType();
            Object defaultValue = field.get(null);
            defaultValues.put(field.getName(), defaultValue);

            ForgeConfigSpec.ConfigValue<?> configValue = createConfigValue(
                    builder, type, configInfo, rangeFlag, defaultValue);

            if (configValue != null) {
                configMap.put(configValue, field);
            }
        } catch (IllegalAccessException e) {
            LOGGER.error("Error loading config field: {}", field.getName(), e);
        }
    }

    private static ForgeConfigSpec.ConfigValue<?> createConfigValue(
            ForgeConfigSpec.Builder builder, Class<?> type, ConfigInfo configInfo,
            RangeFlag rangeFlag, Object defaultValue) {

        if (type == boolean.class) {
            return builder.comment(configInfo.comment())
                    .define(configInfo.name(), (Boolean) defaultValue);
        } else if (type == int.class && rangeFlag != null) {
            return builder.comment(configInfo.comment())
                    .defineInRange(configInfo.name(), (Integer) defaultValue,
                            Integer.parseInt(rangeFlag.min()), Integer.parseInt(rangeFlag.max()));
        } else if (type == double.class && rangeFlag != null) {
            return builder.comment(configInfo.comment())
                    .defineInRange(configInfo.name(), (Double) defaultValue,
                            Double.parseDouble(rangeFlag.min()), Double.parseDouble(rangeFlag.max()));
        } else if (type == long.class && rangeFlag != null) {
            return builder.comment(configInfo.comment())
                    .defineInRange(configInfo.name(), (Long) defaultValue,
                            Long.parseLong(rangeFlag.min()), Long.parseLong(rangeFlag.max()));
        } else if (type == String.class) {
            return builder.comment(configInfo.comment())
                    .define(configInfo.name(), (String) defaultValue);
        }
        return null;
    }

    /**
     * 同步配置值
     *
     * @param serverConfig 要同步的配置数据
     * @param saveToFile   是否保存到配置文件
     */
    public static void syncValue(Map<String, Object> serverConfig, boolean saveToFile) {
        if (!isConfigLoaded) {
            LOGGER.warn("Config not loaded yet, skipping sync");
            return;
        }

        if (saveToFile && commonConfig != null) {
            // 更新配置文件
            updateConfigFile(serverConfig);
        } else {
            // 只更新内存中的静态字段（客户端同步）
            updateStaticFields(serverConfig);
        }
    }

    private static void updateConfigFile(Map<String, Object> serverConfig) {
        try {
            configMap.forEach((configValue, field) -> {
                Object newValue = serverConfig.get(field.getName());
                if (newValue != null) {
                    try {
                        Object convertedValue = convertToFieldType(newValue, field.getType());
                        // 类型安全的设置配置值
                        @SuppressWarnings("unchecked")
                        ForgeConfigSpec.ConfigValue<Object> typedConfigValue =
                                (ForgeConfigSpec.ConfigValue<Object>) configValue;
                        typedConfigValue.set(convertedValue);
                    } catch (Exception e) {
                        LOGGER.error("Error updating config value: {}", field.getName(), e);
                    }
                }
            });

            // 保存配置文件
            commonConfig.save();
            LOGGER.info("Configuration saved to file");

            // 重新加载静态字段
            loadConfigValues();
        } catch (Exception e) {
            LOGGER.error("Error saving config file", e);
        }
    }

    private static void updateStaticFields(Map<String, Object> serverConfig) {
        configMap.forEach((configValue, field) -> {
            field.setAccessible(true);
            try {
                Object newValue = serverConfig.get(field.getName());
                if (newValue != null) {
                    Object convertedValue = convertToFieldType(newValue, field.getType());
                    field.set(null, convertedValue);
                }
            } catch (IllegalAccessException e) {
                LOGGER.error("Error sync config field: {}", field.getName(), e);
            }
        });
    }

    private static Object convertToFieldType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        try {
            if (targetType == int.class || targetType == Integer.class) {
                return value instanceof Number ? ((Number) value).intValue() :
                        Integer.parseInt(value.toString());
            } else if (targetType == long.class || targetType == Long.class) {
                return value instanceof Number ? ((Number) value).longValue() :
                        Long.parseLong(value.toString());
            } else if (targetType == double.class || targetType == Double.class) {
                return value instanceof Number ? ((Number) value).doubleValue() :
                        Double.parseDouble(value.toString());
            } else if (targetType == float.class || targetType == Float.class) {
                return value instanceof Number ? ((Number) value).floatValue() :
                        Float.parseFloat(value.toString());
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return value instanceof Boolean ? value : Boolean.parseBoolean(value.toString());
            } else if (targetType == String.class) {
                return value.toString();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to convert value {} to type {}", value, targetType, e);
        }

        return value;
    }

    /**
     * 重置配置到默认值
     */
    public static void resetToDefaults() {
        if (!isConfigLoaded || commonConfig == null) {
            LOGGER.warn("Config not loaded, cannot reset to defaults");
            return;
        }

        try {
            configMap.forEach((configValue, field) -> {
                Object defaultValue = defaultValues.get(field.getName());
                if (defaultValue != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        ForgeConfigSpec.ConfigValue<Object> typedConfigValue =
                                (ForgeConfigSpec.ConfigValue<Object>) configValue;
                        typedConfigValue.set(defaultValue);
                    } catch (Exception e) {
                        LOGGER.error("Error resetting config value: {}", field.getName(), e);
                    }
                }
            });

            commonConfig.save();
            loadConfigValues();
            LOGGER.info("Configuration reset to defaults");
        } catch (Exception e) {
            LOGGER.error("Error resetting config to defaults", e);
        }
    }

    /**
     * 安全地加载配置值到静态字段
     */
    private static void loadConfigValues() {
        if (!isConfigLoaded) {
            LOGGER.warn("Config not loaded yet, skipping value loading");
            return;
        }

        configMap.forEach((configValue, field) -> {
            field.setAccessible(true);
            try {
                // 安全地获取配置值
                Object value = configValue.get();
                field.set(null, value);
            } catch (Exception e) {
                LOGGER.error("Error loading config field: {}", field.getName(), e);
                // 使用默认值作为后备
                try {
                    Object defaultValue = defaultValues.get(field.getName());
                    if (defaultValue != null) {
                        field.set(null, defaultValue);
                    }
                } catch (IllegalAccessException ex) {
                    LOGGER.error("Error setting default value for field: {}", field.getName(), ex);
                }
            }
        });
    }

    public static Field getField(String key) {
        return configMap.entrySet().stream()
                .filter(entry -> entry.getValue().getName().equals(key))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    public static Pair<Number, Number> getRange(String key) {
        Field field = getField(key);
        if (field == null) return null;

        RangeFlag rangeFlag = field.getAnnotation(RangeFlag.class);
        if (rangeFlag == null) return null;

        try {
            Class<?> type = field.getType();
            if (type == int.class || type == Integer.class) {
                return Pair.of(Integer.parseInt(rangeFlag.min()), Integer.parseInt(rangeFlag.max()));
            } else if (type == long.class || type == Long.class) {
                return Pair.of(Long.parseLong(rangeFlag.min()), Long.parseLong(rangeFlag.max()));
            } else if (type == double.class || type == Double.class) {
                return Pair.of(Double.parseDouble(rangeFlag.min()), Double.parseDouble(rangeFlag.max()));
            } else if (type == float.class || type == Float.class) {
                return Pair.of(Float.parseFloat(rangeFlag.min()), Float.parseFloat(rangeFlag.max()));
            }
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid range values for field: {}", key, e);
        }

        return null;
    }

    /**
     * 获取当前所有配置值的映射
     */
    public static Map<String, Object> getCurrentConfigValues() {
        Map<String, Object> values = new HashMap<>();
        if (!isConfigLoaded) {
            return values;
        }

        configMap.forEach((configValue, field) -> {
            try {
                values.put(field.getName(), configValue.get());
            } catch (Exception e) {
                LOGGER.error("Error getting config value: {}", field.getName(), e);
            }
        });

        return values;
    }

    public static boolean isConfigLoaded() {
        return isConfigLoaded;
    }
}