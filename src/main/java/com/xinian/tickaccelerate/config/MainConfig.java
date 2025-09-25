package com.xinian.tickaccelerate.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainConfig extends TOMLConfiguration {

    private static final List<ConfigOption<Boolean>> BOOLEAN_OPTIONS = new ArrayList<>();
    private int tickRepeatCap;
    private int dynamicCapMsptThreshold;
    private int extraCompensationMsptThreshold;
    private int extraCompensationAmountMs;
    private Map<String, Number> perBlockCaps = new HashMap<>();

    static {
        // General
        BOOLEAN_OPTIONS.add(new ConfigOption<>("general.enabled", true, "Enable or disable the mod entirely."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("general.singleplayer-warning", true, "Show a warning when using the mod in singleplayer."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("general.automatic-updater", true, "Enable automatic update checks."));

        // Acceleration Modules
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.block_entity", false, "Accelerate block entities (e.g., furnaces, hoppers). Can cause issues."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.block_breaking", true, "Accelerate block breaking."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.potion_effect", true, "Accelerate potion effect timers."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.fluid", true, "Accelerate fluid flow."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.item_pickup", true, "Accelerate item pickup delay."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.eating", true, "Accelerate eating and drinking animations."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.nether_portal", true, "Accelerate nether portal teleportation time."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.sleeping", true, "Accelerate time to pass the night when sleeping."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.time", true, "Accelerate the day-night cycle."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.bow", true, "Accelerate bow drawing speed."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.crossbow", true, "Accelerate crossbow loading speed."));
        BOOLEAN_OPTIONS.add(new ConfigOption<>("acceleration_modules.random_tick_speed", true, "Accelerate random block ticks (e.g., crop growth)."));

        // Performance Tuning - Dynamic Cap
        BOOLEAN_OPTIONS.add(new ConfigOption<>("performance_tuning.dynamic_cap.enabled", true, "Enable the dynamic tick repeat cap. This helps prevent feedback loops under heavy lag."));

        // Performance Tuning - Extra Compensation
        BOOLEAN_OPTIONS.add(new ConfigOption<>("performance_tuning.extra_compensation.enabled", true, "Enable extra tick compensation."));

        // Tweaks
        BOOLEAN_OPTIONS.add(new ConfigOption<>("tweaks.disable_server_watchdog", true, "Disable the vanilla server watchdog to prevent crashes due to low TPS."));
    }

    private final Map<String, Boolean> valueCache = new HashMap<>();

    public MainConfig() {
        super("config.toml");

        // Set comments for tables
        setComment("general", "General settings for the mod.");
        setComment("acceleration_modules", "Enable or disable specific acceleration features.");
        setComment("performance_tuning", "Advanced settings for performance and stability.");
        setComment("performance_tuning.dynamic_cap", "Dynamically adjusts the tick cap based on server MSPT.");
        setComment("performance_tuning.extra_compensation", "Adds extra ticks to compensate for lag.");
        setComment("performance_tuning.block_entity_caps", "Fine-grained control for block entity acceleration.");
        setComment("tweaks", "Miscellaneous adjustments.");

        // Set boolean options
        for (ConfigOption<Boolean> option : BOOLEAN_OPTIONS) {
            putIfEmpty(option.getKey(), option.getDefaultValue());
            setComment(option.getKey(), option.getComment());
        }

        // Set non-boolean options
        putIfEmpty("performance_tuning.tick_repeat_cap", 10);
        setComment("performance_tuning.tick_repeat_cap", "The global maximum number of times a tick can be repeated. -1 for no limit.");

        putIfEmpty("performance_tuning.dynamic_cap.mspt_threshold", 100);
        setComment("performance_tuning.dynamic_cap.mspt_threshold", "If MSPT goes above this value, the tick repeat cap is dynamically set to 0.");

        putIfEmpty("performance_tuning.extra_compensation.mspt_threshold", 40);
        setComment("performance_tuning.extra_compensation.mspt_threshold", "If MSPT is above this value, extra compensation will be applied.");
        putIfEmpty("performance_tuning.extra_compensation.amount_ms", 10);
        setComment("performance_tuning.extra_compensation.amount_ms", "The amount of extra compensation to apply in milliseconds.");

        if (!getRawConfig().contains("performance_tuning.block_entity_caps")) {
            Map<String, Integer> defaultCaps = new HashMap<>();
            defaultCaps.put("minecraft:hopper", 2);
            defaultCaps.put("*", -1);
            defaultCaps.forEach((key, value) -> config.set("performance_tuning.block_entity_caps." + key, value));
        }

        save();
        reload();
    }

    @Override
    public void reload() {
        super.reload();

        for (ConfigOption<Boolean> option : BOOLEAN_OPTIONS) {
            valueCache.put(option.getKey(), getOrDefault(option.getKey(), option.getDefaultValue()));
        }
        this.tickRepeatCap = getOrDefault("performance_tuning.tick_repeat_cap", 10);
        this.dynamicCapMsptThreshold = getOrDefault("performance_tuning.dynamic_cap.mspt_threshold", 100);
        this.extraCompensationMsptThreshold = getOrDefault("performance_tuning.extra_compensation.mspt_threshold", 40);
        this.extraCompensationAmountMs = getOrDefault("performance_tuning.extra_compensation.amount_ms", 10);

        this.perBlockCaps.clear();
        Object rawCaps = get("performance_tuning.block_entity_caps");
        if (rawCaps instanceof CommentedConfig) {
            ((CommentedConfig) rawCaps).valueMap().forEach((key, value) -> {
                if (value instanceof Number) {
                    this.perBlockCaps.put(key, (Number) value);
                }
            });
        }
    }

    public int getCapForBlock(ResourceLocation blockId) {
        if (perBlockCaps == null || perBlockCaps.isEmpty()) {
            return getTickRepeatCap();
        }
        String idString = blockId.toString();
        if (perBlockCaps.containsKey(idString)) {
            int cap = perBlockCaps.get(idString).intValue();
            return cap == -1 ? getTickRepeatCap() : cap;
        }
        String namespaceWildcard = blockId.getNamespace() + ":*";
        if (perBlockCaps.containsKey(namespaceWildcard)) {
            int cap = perBlockCaps.get(namespaceWildcard).intValue();
            return cap == -1 ? getTickRepeatCap() : cap;
        }
        if (perBlockCaps.containsKey("*")) {
            int cap = perBlockCaps.get("*").intValue();
            return cap == -1 ? getTickRepeatCap() : cap;
        }
        return getTickRepeatCap();
    }

    public static List<ConfigOption<Boolean>> getBooleanOptions() {
        return BOOLEAN_OPTIONS;
    }

    public void setBoolean(String key, boolean value) {
        set(key, value);
        valueCache.put(key, value);
    }

    public boolean getBoolean(String key) {
        return valueCache.getOrDefault(key, false);
    }

    public int getTickRepeatCap() { return tickRepeatCap; }
    public int getDynamicCapMsptThreshold() { return dynamicCapMsptThreshold; }
    public int getExtraCompensationMsptThreshold() { return extraCompensationMsptThreshold; }
    public int getExtraCompensationAmountMs() { return extraCompensationAmountMs; }

    // Getters for cached values
    public boolean enabled() { return getBoolean("general.enabled"); }
    public boolean singlePlayerWarning() { return getBoolean("general.singleplayer-warning"); }
    public boolean automaticUpdater() { return getBoolean("general.automatic-updater"); }

    public boolean blockEntityAcceleration() { return getBoolean("acceleration_modules.block_entity"); }
    public boolean blockBreakingAcceleration() { return getBoolean("acceleration_modules.block_breaking"); }
    public boolean potionEffectAcceleration() { return getBoolean("acceleration_modules.potion_effect"); }
    public boolean fluidAcceleration() { return getBoolean("acceleration_modules.fluid"); }
    public boolean pickupAcceleration() { return getBoolean("acceleration_modules.item_pickup"); }
    public boolean eatingAcceleration() { return getBoolean("acceleration_modules.eating"); }
    public boolean portalAcceleration() { return getBoolean("acceleration_modules.nether_portal"); }
    public boolean sleepingAcceleration() { return getBoolean("acceleration_modules.sleeping"); }
    public boolean timeAcceleration() { return getBoolean("acceleration_modules.time"); }
    public boolean bowAcceleration() { return getBoolean("acceleration_modules.bow"); }
    public boolean crossbowAcceleration() { return getBoolean("acceleration_modules.crossbow"); }
    public boolean randomTickSpeedAcceleration() { return getBoolean("acceleration_modules.random_tick_speed"); }

    public boolean dynamicCapEnabled() { return getBoolean("performance_tuning.dynamic_cap.enabled"); }
    public boolean extraCompensationEnabled() { return getBoolean("performance_tuning.extra_compensation.enabled"); }

    public boolean disableServerWatchdog() { return getBoolean("tweaks.disable_server_watchdog"); }
}