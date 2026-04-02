package com.xinian.tickaccelerated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

/**
 * In-game command {@code /tickaccelerate} (alias {@code /ta}) for monitoring
 * TPS, MSPT and all active compensation values.
 */
public final class TickAccelerateCommand {

    private TickAccelerateCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("tickaccelerate")
                .requires(src -> src.hasPermission(0))
                .executes(TickAccelerateCommand::showStatus)
                .then(Commands.literal("status").executes(TickAccelerateCommand::showStatus))
                .then(Commands.literal("config").executes(TickAccelerateCommand::showConfig));

        var alias = Commands.literal("ta")
                .requires(src -> src.hasPermission(0))
                .executes(TickAccelerateCommand::showStatus)
                .then(Commands.literal("status").executes(TickAccelerateCommand::showStatus))
                .then(Commands.literal("config").executes(TickAccelerateCommand::showConfig));

        dispatcher.register(root);
        dispatcher.register(alias);
    }

    /* ────────────── /ta status ────────────── */

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        float mspt = server.getCurrentSmoothedTickTime();
        float tps = TpsHelper.getTps(server);
        float tickFactor = TpsHelper.getTickFactor(server);
        float speedMultiplier = TpsHelper.getSpeedMultiplier(server);

        ctx.getSource().sendSuccess(() -> header("Tick Accelerate — Status"), false);

        // ── TPS / MSPT ──
        ChatFormatting tpsColor = tps >= 19.0F ? ChatFormatting.GREEN
                : tps >= 15.0F ? ChatFormatting.YELLOW
                : tps >= 10.0F ? ChatFormatting.GOLD
                : ChatFormatting.RED;

        ctx.getSource().sendSuccess(() -> keyValue("TPS",
                String.format("%.1f / 20.0", tps), tpsColor), false);
        ctx.getSource().sendSuccess(() -> keyValue("MSPT",
                String.format("%.1f ms", mspt),
                mspt <= 50 ? ChatFormatting.GREEN : ChatFormatting.RED), false);

        // ── Compensation values ──
        ctx.getSource().sendSuccess(() -> separator(), false);

        boolean compensating = speedMultiplier > 1.0F;
        ctx.getSource().sendSuccess(() -> keyValue("Compensation",
                compensating ? "ACTIVE" : "INACTIVE",
                compensating ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false);

        if (compensating) {
            ctx.getSource().sendSuccess(() -> keyValue("Speed Multiplier",
                    String.format("%.2f×", speedMultiplier), ChatFormatting.AQUA), false);
            ctx.getSource().sendSuccess(() -> keyValue("Tick Factor",
                    String.format("%.2f", tickFactor), ChatFormatting.AQUA), false);
            ctx.getSource().sendSuccess(() -> keyValue("Example",
                    String.format("20-tick duration → %.0f ticks", 20 * tickFactor),
                    ChatFormatting.GRAY), false);
            ctx.getSource().sendSuccess(() -> keyValue("Example",
                    String.format("Per-tick value ×%.2f effective", speedMultiplier),
                    ChatFormatting.GRAY), false);
        }

        return 1;
    }

    /* ────────────── /ta config ────────────── */

    private static int showConfig(CommandContext<CommandSourceStack> ctx) {
        TickAccelerateConfig cfg = TickAccelerateConfig.INSTANCE;

        ctx.getSource().sendSuccess(() -> header("Tick Accelerate — Config"), false);

        // General
        ctx.getSource().sendSuccess(() -> keyValue("Min TPS",
                String.format("%.1f", cfg.minTps.get()), ChatFormatting.WHITE), false);

        // Server
        ctx.getSource().sendSuccess(() -> separator(), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("  Server").withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> toggle("Disable Watchdog", cfg.disableWatchdog.get()), false);

        // Player
        ctx.getSource().sendSuccess(() -> separator(), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("  Player").withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> toggle("Block Breaking", cfg.enableBlockBreaking.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Attack Cooldown", cfg.enableAttackCooldown.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Food Regen", cfg.enableFoodRegen.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Item Use", cfg.enableItemUse.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Item Cooldown", cfg.enableItemCooldown.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("XP Pickup Delay", cfg.enableXpPickupDelay.get()), false);

        // Entity
        ctx.getSource().sendSuccess(() -> separator(), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("  Entity").withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> toggle("Potion Effect", cfg.enablePotionEffect.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Item Pickup Delay", cfg.enableItemPickupDelay.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Portal Time", cfg.enablePortalTime.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Hurt Time", cfg.enableHurtTime.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Death Time", cfg.enableDeathTime.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Air Supply", cfg.enableAirSupply.get()), false);

        // World
        ctx.getSource().sendSuccess(() -> separator(), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("  World").withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> toggle("Fluid Speed", cfg.enableFluidSpeed.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Random Tick", cfg.enableRandomTick.get()), false);
        ctx.getSource().sendSuccess(() -> toggle("Portal Cooldown", cfg.enablePortalCooldown.get()), false);

        return 1;
    }

    /* ────────────── Formatting helpers ────────────── */

    private static MutableComponent header(String title) {
        return Component.literal("══ " + title + " ══").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static MutableComponent separator() {
        return Component.literal("  ──────────────────").withStyle(ChatFormatting.DARK_GRAY);
    }

    private static MutableComponent keyValue(String key, String value, ChatFormatting valueColor) {
        return Component.literal("  " + key + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(valueColor));
    }

    private static MutableComponent toggle(String name, boolean enabled) {
        return Component.literal("  " + name + ": ").withStyle(ChatFormatting.GRAY)
                .append(enabled
                        ? Component.literal("✔ ON").withStyle(ChatFormatting.GREEN)
                        : Component.literal("✘ OFF").withStyle(ChatFormatting.RED));
    }
}

