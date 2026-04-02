package com.xinian.tickaccelerated;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Tickaccelerate mod configuration.
 * All toggles default to true. Minimum TPS defaults to 5.
 */
public final class TickAccelerateConfig {

    public static final ModConfigSpec SPEC;
    public static final TickAccelerateConfig INSTANCE;

    public final ModConfigSpec.BooleanValue enableBlockBreaking;
    public final ModConfigSpec.BooleanValue enableItemUse;
    public final ModConfigSpec.BooleanValue enableAttackCooldown;
    public final ModConfigSpec.BooleanValue enableItemCooldown;
    public final ModConfigSpec.BooleanValue enableFoodRegen;
    public final ModConfigSpec.BooleanValue enablePotionEffect;
    public final ModConfigSpec.BooleanValue enableItemPickupDelay;
    public final ModConfigSpec.BooleanValue enablePortalTime;
    public final ModConfigSpec.BooleanValue disableWatchdog;
    public final ModConfigSpec.IntValue minTps;

    static {
        Pair<TickAccelerateConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(TickAccelerateConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private TickAccelerateConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tickaccelerate Configuration")
               .comment("Formula: newTicks = originalTicks * tps / 20")
               .push("general");

        minTps = builder
                .comment("Minimum TPS used for compensation calculation.",
                         "Lower value = more aggressive compensation at very low TPS.",
                         "Maximum speed multiplier = 20 / minTps.")
                .defineInRange("minTps", 5, 1, 20);

        disableWatchdog = builder
                .comment("Disable the vanilla server watchdog crash on long ticks.")
                .define("disableWatchdog", true);

        builder.pop();

        builder.comment("Toggle individual TPS compensation features")
               .push("features");

        enableBlockBreaking = builder
                .comment("Compensate block breaking speed.")
                .define("blockBreaking", true);

        enableItemUse = builder
                .comment("Compensate item use duration (eating, drinking, bow, crossbow, etc).")
                .define("itemUse", true);

        enableAttackCooldown = builder
                .comment("Compensate attack cooldown recovery.")
                .define("attackCooldown", true);

        enableItemCooldown = builder
                .comment("Compensate item cooldowns (ender pearl, chorus fruit, shield, etc).")
                .define("itemCooldown", true);

        enableFoodRegen = builder
                .comment("Compensate natural regeneration and starvation timer.")
                .define("foodRegen", true);

        enablePotionEffect = builder
                .comment("Compensate potion effect duration countdown.")
                .define("potionEffect", true);

        enableItemPickupDelay = builder
                .comment("Compensate dropped item pickup delay.")
                .define("itemPickupDelay", true);

        enablePortalTime = builder
                .comment("Compensate nether portal transition time.")
                .define("portalTime", true);

        builder.pop();
    }
}

