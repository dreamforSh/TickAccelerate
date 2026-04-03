package com.xinian.tickaccelerated.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration for the Tick Accelerate mod.
 * <p>Each toggle controls whether TPS compensation is applied to a specific mechanic.</p>
 */
public final class TickAccelerateConfig {

    public static final TickAccelerateConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    /* ── general ── */
    public final ModConfigSpec.DoubleValue minTps;
    public final ModConfigSpec.DoubleValue tpsSmoothingAlpha;
    public final ModConfigSpec.ConfigValue<String> serverLocale;

    /* ── server ── */
    public final ModConfigSpec.BooleanValue disableWatchdog;

    /* ── player ── */
    public final ModConfigSpec.BooleanValue enableBlockBreaking;
    public final ModConfigSpec.BooleanValue enableAttackCooldown;
    public final ModConfigSpec.BooleanValue enableFoodRegen;
    public final ModConfigSpec.BooleanValue enableItemUse;
    public final ModConfigSpec.BooleanValue enableItemCooldown;
    public final ModConfigSpec.BooleanValue enableXpPickupDelay;
    public final ModConfigSpec.BooleanValue enableSleepTimer;

    /* ── entity ── */
    public final ModConfigSpec.BooleanValue enablePotionEffect;
    public final ModConfigSpec.BooleanValue enableItemPickupDelay;
    public final ModConfigSpec.BooleanValue enablePortalTime;
    public final ModConfigSpec.BooleanValue enableHurtTime;
    public final ModConfigSpec.BooleanValue enableDeathTime;
    public final ModConfigSpec.BooleanValue enableAirSupply;
    public final ModConfigSpec.BooleanValue enableSwingSpeed;
    public final ModConfigSpec.BooleanValue enableInvulnerability;
    public final ModConfigSpec.BooleanValue enableFireTick;
    public final ModConfigSpec.BooleanValue enableBoardingCooldown;
    public final ModConfigSpec.BooleanValue enableItemDespawn;
    public final ModConfigSpec.BooleanValue enableXpOrbAge;
    public final ModConfigSpec.BooleanValue enableArrowLife;
    public final ModConfigSpec.BooleanValue enableBreedingTimer;
    public final ModConfigSpec.BooleanValue enableMobGrowth;
    public final ModConfigSpec.BooleanValue enableTntFuse;
    public final ModConfigSpec.BooleanValue enableFallingBlock;

    /* ── world ── */
    public final ModConfigSpec.BooleanValue enableFluidSpeed;
    public final ModConfigSpec.BooleanValue enableRandomTick;
    public final ModConfigSpec.BooleanValue enablePortalCooldown;
    public final ModConfigSpec.BooleanValue enableDayTime;
    public final ModConfigSpec.BooleanValue enableBlockEntityTick;
    public final ModConfigSpec.BooleanValue enableWeatherCycle;
    public final ModConfigSpec.BooleanValue enableSpawnerCooldown;

    /* ── client ── */
    public final ModConfigSpec.BooleanValue enableClientAnimations;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        /* ── General ── */
        builder.comment("General settings").push("general");
        var minTpsVal = builder
                .comment("Minimum TPS to use in calculations (clamp floor). Below this, compensation stops increasing.")
                .defineInRange("minTps", 5.0, 1.0, 20.0);
        var tpsSmoothingAlphaVal = builder
                .comment("EMA smoothing factor for TPS calculation (0 = no change, 1 = no smoothing).",
                         "Lower values produce smoother, slower transitions; higher values react faster.",
                         "Recommended range: 0.05 ~ 0.3. Default 0.15 gives a good balance.")
                .defineInRange("tpsSmoothingAlpha", 0.15, 0.01, 1.0);
        var serverLocaleVal = builder
                .comment("Server display language for command output (e.g. en_us, zh_cn).",
                         "This ensures proper text rendering for clients that do not have the mod installed.")
                .define("serverLocale", "en_us");
        builder.pop();

        /* ── Server ── */
        builder.comment("Server-level compensations").push("server");
        var disableWatchdogVal = builder
                .comment("Disable the dedicated server watchdog to prevent crash on low TPS.")
                .define("disableWatchdog", true);
        builder.pop();

        /* ── Player ── */
        builder.comment("Player-related compensations").push("player");
        var enableBlockBreakingVal = builder
                .comment("Compensate block breaking speed so blocks break in the same real time.")
                .define("enableBlockBreaking", true);
        var enableAttackCooldownVal = builder
                .comment("Compensate attack cooldown recovery speed.")
                .define("enableAttackCooldown", true);
        var enableFoodRegenVal = builder
                .comment("Compensate natural regeneration / starvation timer.")
                .define("enableFoodRegen", true);
        var enableItemUseVal = builder
                .comment("Compensate item use duration (eating, drinking, bow charge, etc).")
                .define("enableItemUse", true);
        var enableItemCooldownVal = builder
                .comment("Compensate item cooldowns (ender pearl, chorus fruit, shield, etc).")
                .define("enableItemCooldown", true);
        var enableXpPickupDelayVal = builder
                .comment("Compensate experience orb pickup delay.")
                .define("enableXpPickupDelay", true);
        var enableSleepTimerVal = builder
                .comment("Compensate sleep timer so the player falls asleep in the same real time.")
                .define("enableSleepTimer", true);
        builder.pop();

        /* ── Entity ── */
        builder.comment("Entity-related compensations").push("entity");
        var enablePotionEffectVal = builder
                .comment("Compensate potion effect durations.")
                .define("enablePotionEffect", true);
        var enableItemPickupDelayVal = builder
                .comment("Compensate dropped item pickup delay.")
                .define("enableItemPickupDelay", true);
        var enablePortalTimeVal = builder
                .comment("Compensate nether portal transition time.")
                .define("enablePortalTime", true);
        var enableHurtTimeVal = builder
                .comment("Compensate hurt (damage) invulnerability cooldown for entities.")
                .define("enableHurtTime", true);
        var enableDeathTimeVal = builder
                .comment("Compensate death animation timer for entities.")
                .define("enableDeathTime", true);
        var enableAirSupplyVal = builder
                .comment("Compensate air supply recovery speed when surfacing from water.")
                .define("enableAirSupply", true);
        var enableSwingSpeedVal = builder
                .comment("Compensate attack swing animation speed so arm swings play at correct real-time speed.")
                .define("enableSwingSpeed", true);
        var enableInvulnerabilityVal = builder
                .comment("Compensate damage invulnerability frames (iFrames) for non-player entities.")
                .define("enableInvulnerability", true);
        var enableFireTickVal = builder
                .comment("Compensate fire tick duration so entities stop burning in the same real time.")
                .define("enableFireTick", true);
        var enableBoardingCooldownVal = builder
                .comment("Compensate vehicle boarding cooldown (60 ticks after dismounting).")
                .define("enableBoardingCooldown", true);
        var enableItemDespawnVal = builder
                .comment("Compensate dropped item despawn timer so items despawn in correct real time.",
                         "Prevents item entity buildup at low TPS.")
                .define("enableItemDespawn", true);
        var enableXpOrbAgeVal = builder
                .comment("Compensate experience orb age/despawn timer.")
                .define("enableXpOrbAge", true);
        var enableArrowLifeVal = builder
                .comment("Compensate arrow life timer (despawn after 60s stuck in block).")
                .define("enableArrowLife", true);
        var enableBreedingTimerVal = builder
                .comment("Compensate animal breeding 'in love' timer (600 ticks = 30 seconds).")
                .define("enableBreedingTimer", true);
        var enableMobGrowthVal = builder
                .comment("Compensate baby mob growth timer so babies grow up in the correct real time.")
                .define("enableMobGrowth", true);
        var enableTntFuseVal = builder
                .comment("Compensate TNT fuse timer so TNT explodes in the correct real time (default 80 ticks = 4s).")
                .define("enableTntFuse", true);
        var enableFallingBlockVal = builder
                .comment("Compensate falling block entity time counter so sand/gravel/anvils land at correct real-time speed.")
                .define("enableFallingBlock", true);
        builder.pop();

        /* ── World ── */
        builder.comment("World-level compensations").push("world");
        var enableFluidSpeedVal = builder
                .comment("Compensate fluid (water/lava) spread tick delay so fluids flow at the same real-time speed.")
                .define("enableFluidSpeed", true);
        var enableRandomTickVal = builder
                .comment("Compensate random tick speed (crop growth, leaf decay, etc) so they progress at the same real-time rate.")
                .define("enableRandomTick", true);
        var enablePortalCooldownVal = builder
                .comment("Compensate portal re-entry cooldown so it lasts the same real time after teleportation.")
                .define("enablePortalCooldown", true);
        var enableDayTimeVal = builder
                .comment("Compensate day/night cycle progression so the sun and moon move at the correct real-time speed.")
                .define("enableDayTime", true);
        var enableBlockEntityTickVal = builder
                .comment("Compensate block entity ticking (furnaces, hoppers, brewing stands, etc).")
                .define("enableBlockEntityTick", true);
        var enableWeatherCycleVal = builder
                .comment("Compensate weather cycle so rain/thunder transitions happen at correct real-time speed.")
                .define("enableWeatherCycle", true);
        var enableSpawnerCooldownVal = builder
                .comment("Compensate mob spawner delay so spawners produce mobs at the correct real-time rate.")
                .define("enableSpawnerCooldown", true);
        builder.pop();

        /* ── Client ── */
        builder.comment("Client-side animation compensations").push("client");
        var enableClientAnimationsVal = builder
                .comment("Compensate client-side animation playback speed (hurt flash, death animation, swing animation, etc).",
                         "When TPS is low, these tick-based animations normally play slower than intended.",
                         "This makes them play at correct real-time speed on the client.")
                .define("enableClientAnimations", true);
        builder.pop();

        SPEC = builder.build();

        INSTANCE = new TickAccelerateConfig(
                minTpsVal, tpsSmoothingAlphaVal, serverLocaleVal, disableWatchdogVal,
                enableBlockBreakingVal, enableAttackCooldownVal, enableFoodRegenVal,
                enableItemUseVal, enableItemCooldownVal, enableXpPickupDelayVal,
                enableSleepTimerVal,
                enablePotionEffectVal, enableItemPickupDelayVal, enablePortalTimeVal,
                enableHurtTimeVal, enableDeathTimeVal, enableAirSupplyVal,
                enableSwingSpeedVal, enableInvulnerabilityVal,
                enableFireTickVal, enableBoardingCooldownVal,
                enableItemDespawnVal, enableXpOrbAgeVal,
                enableArrowLifeVal, enableBreedingTimerVal, enableMobGrowthVal,
                enableTntFuseVal, enableFallingBlockVal,
                enableFluidSpeedVal, enableRandomTickVal, enablePortalCooldownVal,
                enableDayTimeVal, enableBlockEntityTickVal,
                enableWeatherCycleVal, enableSpawnerCooldownVal,
                enableClientAnimationsVal
        );
    }

    private TickAccelerateConfig(
            ModConfigSpec.DoubleValue minTps,
            ModConfigSpec.DoubleValue tpsSmoothingAlpha,
            ModConfigSpec.ConfigValue<String> serverLocale,
            ModConfigSpec.BooleanValue disableWatchdog,
            ModConfigSpec.BooleanValue enableBlockBreaking,
            ModConfigSpec.BooleanValue enableAttackCooldown,
            ModConfigSpec.BooleanValue enableFoodRegen,
            ModConfigSpec.BooleanValue enableItemUse,
            ModConfigSpec.BooleanValue enableItemCooldown,
            ModConfigSpec.BooleanValue enableXpPickupDelay,
            ModConfigSpec.BooleanValue enableSleepTimer,
            ModConfigSpec.BooleanValue enablePotionEffect,
            ModConfigSpec.BooleanValue enableItemPickupDelay,
            ModConfigSpec.BooleanValue enablePortalTime,
            ModConfigSpec.BooleanValue enableHurtTime,
            ModConfigSpec.BooleanValue enableDeathTime,
            ModConfigSpec.BooleanValue enableAirSupply,
            ModConfigSpec.BooleanValue enableSwingSpeed,
            ModConfigSpec.BooleanValue enableInvulnerability,
            ModConfigSpec.BooleanValue enableFireTick,
            ModConfigSpec.BooleanValue enableBoardingCooldown,
            ModConfigSpec.BooleanValue enableItemDespawn,
            ModConfigSpec.BooleanValue enableXpOrbAge,
            ModConfigSpec.BooleanValue enableArrowLife,
            ModConfigSpec.BooleanValue enableBreedingTimer,
            ModConfigSpec.BooleanValue enableMobGrowth,
            ModConfigSpec.BooleanValue enableTntFuse,
            ModConfigSpec.BooleanValue enableFallingBlock,
            ModConfigSpec.BooleanValue enableFluidSpeed,
            ModConfigSpec.BooleanValue enableRandomTick,
            ModConfigSpec.BooleanValue enablePortalCooldown,
            ModConfigSpec.BooleanValue enableDayTime,
            ModConfigSpec.BooleanValue enableBlockEntityTick,
            ModConfigSpec.BooleanValue enableWeatherCycle,
            ModConfigSpec.BooleanValue enableSpawnerCooldown,
            ModConfigSpec.BooleanValue enableClientAnimations
    ) {
        this.minTps = minTps;
        this.tpsSmoothingAlpha = tpsSmoothingAlpha;
        this.serverLocale = serverLocale;
        this.disableWatchdog = disableWatchdog;
        this.enableBlockBreaking = enableBlockBreaking;
        this.enableAttackCooldown = enableAttackCooldown;
        this.enableFoodRegen = enableFoodRegen;
        this.enableItemUse = enableItemUse;
        this.enableItemCooldown = enableItemCooldown;
        this.enableXpPickupDelay = enableXpPickupDelay;
        this.enableSleepTimer = enableSleepTimer;
        this.enablePotionEffect = enablePotionEffect;
        this.enableItemPickupDelay = enableItemPickupDelay;
        this.enablePortalTime = enablePortalTime;
        this.enableHurtTime = enableHurtTime;
        this.enableDeathTime = enableDeathTime;
        this.enableAirSupply = enableAirSupply;
        this.enableSwingSpeed = enableSwingSpeed;
        this.enableInvulnerability = enableInvulnerability;
        this.enableFireTick = enableFireTick;
        this.enableBoardingCooldown = enableBoardingCooldown;
        this.enableItemDespawn = enableItemDespawn;
        this.enableXpOrbAge = enableXpOrbAge;
        this.enableArrowLife = enableArrowLife;
        this.enableBreedingTimer = enableBreedingTimer;
        this.enableMobGrowth = enableMobGrowth;
        this.enableTntFuse = enableTntFuse;
        this.enableFallingBlock = enableFallingBlock;
        this.enableFluidSpeed = enableFluidSpeed;
        this.enableRandomTick = enableRandomTick;
        this.enablePortalCooldown = enablePortalCooldown;
        this.enableDayTime = enableDayTime;
        this.enableBlockEntityTick = enableBlockEntityTick;
        this.enableWeatherCycle = enableWeatherCycle;
        this.enableSpawnerCooldown = enableSpawnerCooldown;
        this.enableClientAnimations = enableClientAnimations;
    }
}

