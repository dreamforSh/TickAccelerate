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

    /* ── server ── */
    public final ModConfigSpec.BooleanValue disableWatchdog;

    /* ── player ── */
    public final ModConfigSpec.BooleanValue enableBlockBreaking;
    public final ModConfigSpec.BooleanValue enableAttackCooldown;
    public final ModConfigSpec.BooleanValue enableFoodRegen;
    public final ModConfigSpec.BooleanValue enableItemUse;
    public final ModConfigSpec.BooleanValue enableItemCooldown;
    public final ModConfigSpec.BooleanValue enableXpPickupDelay;

    /* ── entity ── */
    public final ModConfigSpec.BooleanValue enablePotionEffect;
    public final ModConfigSpec.BooleanValue enableItemPickupDelay;
    public final ModConfigSpec.BooleanValue enablePortalTime;
    public final ModConfigSpec.BooleanValue enableHurtTime;
    public final ModConfigSpec.BooleanValue enableDeathTime;
    public final ModConfigSpec.BooleanValue enableAirSupply;

    /* ── world ── */
    public final ModConfigSpec.BooleanValue enableFluidSpeed;
    public final ModConfigSpec.BooleanValue enableRandomTick;
    public final ModConfigSpec.BooleanValue enablePortalCooldown;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        /* ── General ── */
        builder.comment("General settings").push("general");
        var minTpsVal = builder
                .comment("Minimum TPS to use in calculations (clamp floor). Below this, compensation stops increasing.")
                .defineInRange("minTps", 5.0, 1.0, 20.0);
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
        builder.pop();

        SPEC = builder.build();

        INSTANCE = new TickAccelerateConfig(
                minTpsVal, disableWatchdogVal,
                enableBlockBreakingVal, enableAttackCooldownVal, enableFoodRegenVal,
                enableItemUseVal, enableItemCooldownVal, enableXpPickupDelayVal,
                enablePotionEffectVal, enableItemPickupDelayVal, enablePortalTimeVal,
                enableHurtTimeVal, enableDeathTimeVal, enableAirSupplyVal,
                enableFluidSpeedVal, enableRandomTickVal, enablePortalCooldownVal
        );
    }

    private TickAccelerateConfig(
            ModConfigSpec.DoubleValue minTps,
            ModConfigSpec.BooleanValue disableWatchdog,
            ModConfigSpec.BooleanValue enableBlockBreaking,
            ModConfigSpec.BooleanValue enableAttackCooldown,
            ModConfigSpec.BooleanValue enableFoodRegen,
            ModConfigSpec.BooleanValue enableItemUse,
            ModConfigSpec.BooleanValue enableItemCooldown,
            ModConfigSpec.BooleanValue enableXpPickupDelay,
            ModConfigSpec.BooleanValue enablePotionEffect,
            ModConfigSpec.BooleanValue enableItemPickupDelay,
            ModConfigSpec.BooleanValue enablePortalTime,
            ModConfigSpec.BooleanValue enableHurtTime,
            ModConfigSpec.BooleanValue enableDeathTime,
            ModConfigSpec.BooleanValue enableAirSupply,
            ModConfigSpec.BooleanValue enableFluidSpeed,
            ModConfigSpec.BooleanValue enableRandomTick,
            ModConfigSpec.BooleanValue enablePortalCooldown
    ) {
        this.minTps = minTps;
        this.disableWatchdog = disableWatchdog;
        this.enableBlockBreaking = enableBlockBreaking;
        this.enableAttackCooldown = enableAttackCooldown;
        this.enableFoodRegen = enableFoodRegen;
        this.enableItemUse = enableItemUse;
        this.enableItemCooldown = enableItemCooldown;
        this.enableXpPickupDelay = enableXpPickupDelay;
        this.enablePotionEffect = enablePotionEffect;
        this.enableItemPickupDelay = enableItemPickupDelay;
        this.enablePortalTime = enablePortalTime;
        this.enableHurtTime = enableHurtTime;
        this.enableDeathTime = enableDeathTime;
        this.enableAirSupply = enableAirSupply;
        this.enableFluidSpeed = enableFluidSpeed;
        this.enableRandomTick = enableRandomTick;
        this.enablePortalCooldown = enablePortalCooldown;
    }
}

