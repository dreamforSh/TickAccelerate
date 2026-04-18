package com.xinian.tickaccelerated.config;

import net.minecraft.server.MinecraftServer;

/**
 * Per-tick snapshot of all config toggles.
 * <p>Reads {@link TickAccelerateConfig} once per server tick and exposes
 * all values as primitive fields, eliminating repeated {@code .get()}
 * calls and boxing overhead across dozens of mixin injection sites.</p>
 */
public final class ConfigSnapshot {

    private static volatile ConfigSnapshot cached;
    private static int lastTick = -1;

    public final boolean disableWatchdog;
    public final boolean enableAntiKick;
    public final boolean enableBlockBreaking;
    public final boolean enableAttackCooldown;
    public final boolean enableFoodRegen;
    public final boolean enableItemUse;
    public final boolean enableItemCooldown;
    public final boolean enableXpPickupDelay;
    public final boolean enableSleepTimer;
    public final boolean enablePotionEffect;
    public final boolean enableItemPickupDelay;
    public final boolean enablePortalTime;
    public final boolean enableHurtTime;
    public final boolean enableDeathTime;
    public final boolean enableAirSupply;
    public final boolean enableSwingSpeed;
    public final boolean enableInvulnerability;
    public final boolean enableFireTick;
    public final boolean enableBoardingCooldown;
    public final boolean enableItemDespawn;
    public final boolean enableXpOrbAge;
    public final boolean enableArrowLife;
    public final boolean enableBreedingTimer;
    public final boolean enableMobGrowth;
    public final boolean enableFluidSpeed;
    public final boolean enableRandomTick;
    public final boolean enablePortalCooldown;
    public final boolean enableDayTime;
    public final boolean enableBlockEntityTick;
    public final boolean enableClientAnimations;
    public final boolean enableTntFuse;
    public final boolean enableWeatherCycle;
    public final boolean enableSpawnerCooldown;
    public final boolean enableFallingBlock;
    public final double minTps;

    private ConfigSnapshot(TickAccelerateConfig cfg) {
        this.disableWatchdog = cfg.disableWatchdog.get();
        this.enableAntiKick = cfg.enableAntiKick.get();
        this.enableBlockBreaking = cfg.enableBlockBreaking.get();
        this.enableAttackCooldown = cfg.enableAttackCooldown.get();
        this.enableFoodRegen = cfg.enableFoodRegen.get();
        this.enableItemUse = cfg.enableItemUse.get();
        this.enableItemCooldown = cfg.enableItemCooldown.get();
        this.enableXpPickupDelay = cfg.enableXpPickupDelay.get();
        this.enableSleepTimer = cfg.enableSleepTimer.get();
        this.enablePotionEffect = cfg.enablePotionEffect.get();
        this.enableItemPickupDelay = cfg.enableItemPickupDelay.get();
        this.enablePortalTime = cfg.enablePortalTime.get();
        this.enableHurtTime = cfg.enableHurtTime.get();
        this.enableDeathTime = cfg.enableDeathTime.get();
        this.enableAirSupply = cfg.enableAirSupply.get();
        this.enableSwingSpeed = cfg.enableSwingSpeed.get();
        this.enableInvulnerability = cfg.enableInvulnerability.get();
        this.enableFireTick = cfg.enableFireTick.get();
        this.enableBoardingCooldown = cfg.enableBoardingCooldown.get();
        this.enableItemDespawn = cfg.enableItemDespawn.get();
        this.enableXpOrbAge = cfg.enableXpOrbAge.get();
        this.enableArrowLife = cfg.enableArrowLife.get();
        this.enableBreedingTimer = cfg.enableBreedingTimer.get();
        this.enableMobGrowth = cfg.enableMobGrowth.get();
        this.enableFluidSpeed = cfg.enableFluidSpeed.get();
        this.enableRandomTick = cfg.enableRandomTick.get();
        this.enablePortalCooldown = cfg.enablePortalCooldown.get();
        this.enableDayTime = cfg.enableDayTime.get();
        this.enableBlockEntityTick = cfg.enableBlockEntityTick.get();
        this.enableClientAnimations = cfg.enableClientAnimations.get();
        this.enableTntFuse = cfg.enableTntFuse.get();
        this.enableWeatherCycle = cfg.enableWeatherCycle.get();
        this.enableSpawnerCooldown = cfg.enableSpawnerCooldown.get();
        this.enableFallingBlock = cfg.enableFallingBlock.get();
        this.minTps = cfg.minTps.get();
    }

    /**
     * Returns a cached snapshot, refreshing once per server tick.
     * Falls back to a default-all-enabled snapshot if config is not yet loaded.
     */
    public static ConfigSnapshot get(MinecraftServer server) {
        int tick = server != null ? server.getTickCount() : -1;
        if (cached == null || tick != lastTick) {
            lastTick = tick;
            try {
                cached = new ConfigSnapshot(TickAccelerateConfig.INSTANCE);
            } catch (Exception e) {
                cached = defaults();
            }
        }
        return cached;
    }

    /**
     * Returns a snapshot with all features enabled (safe defaults).
     */
    public static ConfigSnapshot defaults() {
        try {
            return new ConfigSnapshot(TickAccelerateConfig.INSTANCE);
        } catch (Exception e) {
            return FALLBACK;
        }
    }

    private static final ConfigSnapshot FALLBACK = new ConfigSnapshot();

    private ConfigSnapshot() {
        this.disableWatchdog = true;
        this.enableAntiKick = true;
        this.enableBlockBreaking = true;
        this.enableAttackCooldown = true;
        this.enableFoodRegen = true;
        this.enableItemUse = true;
        this.enableItemCooldown = true;
        this.enableXpPickupDelay = true;
        this.enableSleepTimer = true;
        this.enablePotionEffect = true;
        this.enableItemPickupDelay = true;
        this.enablePortalTime = true;
        this.enableHurtTime = true;
        this.enableDeathTime = true;
        this.enableAirSupply = true;
        this.enableSwingSpeed = true;
        this.enableInvulnerability = true;
        this.enableFireTick = true;
        this.enableBoardingCooldown = true;
        this.enableItemDespawn = true;
        this.enableXpOrbAge = true;
        this.enableArrowLife = true;
        this.enableBreedingTimer = true;
        this.enableMobGrowth = true;
        this.enableFluidSpeed = true;
        this.enableRandomTick = true;
        this.enablePortalCooldown = true;
        this.enableDayTime = true;
        this.enableBlockEntityTick = true;
        this.enableClientAnimations = true;
        this.enableTntFuse = true;
        this.enableWeatherCycle = true;
        this.enableSpawnerCooldown = true;
        this.enableFallingBlock = true;
        this.minTps = 5.0;
    }
}

