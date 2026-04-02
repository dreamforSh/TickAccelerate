package com.xinian.tickaccelerated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

/**
 * In-game command {@code /tickaccelerate} (alias {@code /ta}) for monitoring
 * TPS, MSPT and all active compensation values.
 *
 * <ul>
 *   <li>{@code /ta} or {@code /ta status} — live performance & compensation dashboard with per-system tick data</li>
 *   <li>{@code /ta config} — list all compensation toggle states</li>
 * </ul>
 */
public final class TickAccelerateCommand {

    private static final String BAR_FULL  = "■";
    private static final String BAR_EMPTY = "□";
    private static final int BAR_LENGTH = 20;

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

    /* ══════════════════ /ta status ══════════════════ */

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        TickAccelerateConfig cfg = TickAccelerateConfig.INSTANCE;
        float mspt   = server.getCurrentSmoothedTickTime();
        float tps    = TpsHelper.getTps(server);
        float factor = TpsHelper.getTickFactor(server);
        float mult   = TpsHelper.getSpeedMultiplier(server);
        boolean active = mult > 1.0F;
        int randomTickBase = server.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);

        CommandSourceStack src = ctx.getSource();

        // ── header ──
        src.sendSuccess(() -> header("Tick Accelerate"), false);

        // ── TPS bar + MSPT inline ──
        src.sendSuccess(() -> buildTpsBar(tps, mspt), false);

        // ── compensation overview ──
        if (!active) {
            src.sendSuccess(() ->
                    label("Status").append(lit("● NORMAL", ChatFormatting.GREEN))
                            .append(lit("  TPS is healthy, no compensation applied", ChatFormatting.DARK_GRAY)),
                    false);
            src.sendSuccess(() -> footerLink("/ta config", "View Config"), false);
            return 1;
        }

        src.sendSuccess(() ->
                label("Status").append(lit("● COMPENSATING", ChatFormatting.YELLOW))
                        .append(lit("  ×", ChatFormatting.DARK_GRAY))
                        .append(lit(String.format("%.2f", mult), ChatFormatting.AQUA))
                        .append(lit(String.format("  (factor %.2f)", factor), ChatFormatting.DARK_GRAY)),
                false);

        // ── Player compensations ──
        src.sendSuccess(() -> sectionHeader("Player"), false);
        {
            // speed-multiplier type: per-tick increment boosted
            MutableComponent line = Component.literal("    ");
            line.append(tickChip("Block Break", cfg.enableBlockBreaking.get(), active,
                    String.format("×%.1f speed", mult), "Break progress multiplied per tick"));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            line.append(tickChip("Attack CD", cfg.enableAttackCooldown.get(), active,
                    String.format("×%.1f recovery", mult), "attackStrengthTicker incremented faster"));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            line.append(tickChip("Food", cfg.enableFoodRegen.get(), active,
                    String.format("×%.1f timer", mult), "foodTickTimer incremented faster"));
            src.sendSuccess(() -> line, false);

            MutableComponent line2 = Component.literal("    ");
            // tick-factor type: duration scaled down
            int itemUseTicks = Math.max(1, Math.round(32 * factor)); // typical eat = 32t
            line2.append(tickChip("Item Use", cfg.enableItemUse.get(), active,
                    String.format("32t→%dt", itemUseTicks), "Eating/drinking/bow charge duration"));
            line2.append(lit("  ", ChatFormatting.DARK_GRAY));
            int cooldownTicks = Math.max(1, Math.round(20 * factor)); // typical CD = 20t
            line2.append(tickChip("Item CD", cfg.enableItemCooldown.get(), active,
                    String.format("20t→%dt", cooldownTicks), "Ender pearl / chorus fruit cooldown"));
            line2.append(lit("  ", ChatFormatting.DARK_GRAY));
            line2.append(tickChip("XP Delay", cfg.enableXpPickupDelay.get(), active,
                    String.format("×%.1f drain", mult), "takeXpDelay decremented faster"));
            src.sendSuccess(() -> line2, false);
        }

        // ── Entity compensations ──
        src.sendSuccess(() -> sectionHeader("Entity"), false);
        {
            MutableComponent line = Component.literal("    ");
            line.append(tickChip("Potion", cfg.enablePotionEffect.get(), active,
                    String.format("×%.1f tick-down", mult), "Effect duration ticks down faster"));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            int pickupTicks = Math.max(0, Math.round(10 * factor)); // default pickup=10t
            line.append(tickChip("Pickup", cfg.enableItemPickupDelay.get(), active,
                    String.format("10t→%dt", pickupTicks), "Dropped item pickup delay"));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            line.append(tickChip("Portal", cfg.enablePortalTime.get(), active,
                    String.format("×%.1f charge", mult), "Portal transition portalTime++ faster"));
            src.sendSuccess(() -> line, false);

            MutableComponent line2 = Component.literal("    ");
            int hurtTicks = Math.max(0, Math.round(10 * factor)); // hurtTime default=10
            line2.append(tickChip("Hurt", cfg.enableHurtTime.get(), active,
                    String.format("10t→%dt", hurtTicks), "Damage invulnerability frames"));
            line2.append(lit("  ", ChatFormatting.DARK_GRAY));
            int deathTicks = Math.max(1, Math.round(20 * factor)); // deathTime threshold=20
            line2.append(tickChip("Death", cfg.enableDeathTime.get(), active,
                    String.format("20t→%dt", deathTicks), "Death animation before removal"));
            line2.append(lit("  ", ChatFormatting.DARK_GRAY));
            line2.append(tickChip("Air", cfg.enableAirSupply.get(), active,
                    String.format("×%.1f refill", mult), "Air recovery +4/tick boosted"));
            src.sendSuccess(() -> line2, false);

            MutableComponent line3 = Component.literal("    ");
            int swingDur = Math.max(1, Math.round(6 * factor)); // default swing ≈ 6t
            line3.append(tickChip("Swing", cfg.enableSwingSpeed.get(), active,
                    String.format("6t→%dt", swingDur), "Attack arm swing animation speed"));
            line3.append(lit("  ", ChatFormatting.DARK_GRAY));
            int iFrames = Math.max(0, Math.round(20 * factor)); // invulnerableTime default=20
            line3.append(tickChip("I-Frames", cfg.enableInvulnerability.get(), active,
                    String.format("20t→%dt", iFrames), "Damage i-frames for non-player entities"));
            src.sendSuccess(() -> line3, false);
        }

        // ── World compensations ──
        src.sendSuccess(() -> sectionHeader("World"), false);
        {
            MutableComponent line = Component.literal("    ");
            int waterDelay = Math.max(1, Math.round(5 * factor));  // water tick delay=5
            int lavaDelay  = Math.max(1, Math.round(30 * factor)); // lava overworld=30
            line.append(tickChip("Water", cfg.enableFluidSpeed.get(), active,
                    String.format("5t→%dt", waterDelay), "Water spread tick delay"));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            line.append(tickChip("Lava", cfg.enableFluidSpeed.get(), active,
                    String.format("30t→%dt", lavaDelay), "Lava spread tick delay (overworld)"));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            int rtScaled = Math.round(randomTickBase * mult);
            line.append(tickChip("Rand Tick", cfg.enableRandomTick.get(), active,
                    String.format("%d→%d/chunk", randomTickBase, rtScaled),
                    "randomTickSpeed per chunk per tick"));
            src.sendSuccess(() -> line, false);

            MutableComponent line2 = Component.literal("    ");
            int portalCD = Math.max(0, Math.round(10 * factor)); // player portal cooldown=10
            line2.append(tickChip("Portal CD", cfg.enablePortalCooldown.get(), active,
                    String.format("10t→%dt", portalCD), "Portal re-entry cooldown (player)"));
            src.sendSuccess(() -> line2, false);
        }

        // ── Client animation compensations ──
        src.sendSuccess(() -> sectionHeader("Client"), false);
        {
            MutableComponent line = Component.literal("    ");
            line.append(tickChip("Animations", cfg.enableClientAnimations.get(), active,
                    String.format("×%.1f playback", mult),
                    "Client-side hurt flash, death anim, swing anim,\ni-frames play at correct real-time speed"));
            src.sendSuccess(() -> line, false);
        }

        src.sendSuccess(() -> footerLink("/ta config", "View Config"), false);
        return 1;
    }

    /* ══════════════════ /ta config ══════════════════ */

    private static int showConfig(CommandContext<CommandSourceStack> ctx) {
        TickAccelerateConfig cfg = TickAccelerateConfig.INSTANCE;
        CommandSourceStack src = ctx.getSource();

        src.sendSuccess(() -> header("Tick Accelerate — Config"), false);

        // General
        src.sendSuccess(() ->
                label("Min TPS Clamp").append(lit(String.format("%.1f", cfg.minTps.get()), ChatFormatting.WHITE)),
                false);

        // ── Server ──
        src.sendSuccess(() -> sectionHeader("Server"), false);
        src.sendSuccess(() -> toggleRow("Watchdog Disable", cfg.disableWatchdog.get()), false);

        // ── Player ──
        src.sendSuccess(() -> sectionHeader("Player"), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"Block Break", "Attack CD", "Food Regen"},
                new boolean[]{cfg.enableBlockBreaking.get(), cfg.enableAttackCooldown.get(), cfg.enableFoodRegen.get()}
        ), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"Item Use", "Item CD", "XP Delay"},
                new boolean[]{cfg.enableItemUse.get(), cfg.enableItemCooldown.get(), cfg.enableXpPickupDelay.get()}
        ), false);

        // ── Entity ──
        src.sendSuccess(() -> sectionHeader("Entity"), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"Potion FX", "Item Pickup", "Portal Time"},
                new boolean[]{cfg.enablePotionEffect.get(), cfg.enableItemPickupDelay.get(), cfg.enablePortalTime.get()}
        ), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"Hurt Time", "Death Time", "Air Supply"},
                new boolean[]{cfg.enableHurtTime.get(), cfg.enableDeathTime.get(), cfg.enableAirSupply.get()}
        ), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"Swing Speed", "I-Frames"},
                new boolean[]{cfg.enableSwingSpeed.get(), cfg.enableInvulnerability.get()}
        ), false);

        // ── World ──
        src.sendSuccess(() -> sectionHeader("World"), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"Fluid Flow", "Random Tick", "Portal CD"},
                new boolean[]{cfg.enableFluidSpeed.get(), cfg.enableRandomTick.get(), cfg.enablePortalCooldown.get()}
        ), false);

        // ── Client ──
        src.sendSuccess(() -> sectionHeader("Client"), false);
        src.sendSuccess(() -> toggleRow("Animations", cfg.enableClientAnimations.get()), false);

        src.sendSuccess(() -> footerLink("/ta status", "View Status"), false);
        return 1;
    }

    /* ══════════════════ Formatting helpers ══════════════════ */

    /** Shorthand: styled literal */
    private static MutableComponent lit(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    /** Gold bold header line */
    private static MutableComponent header(String title) {
        return lit("  ══ ", ChatFormatting.DARK_GRAY)
                .append(Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(lit(" ══", ChatFormatting.DARK_GRAY));
    }

    /** Section sub-header: "  ▸ Name" */
    private static MutableComponent sectionHeader(String name) {
        return lit("  ▸ ", ChatFormatting.GOLD).append(lit(name, ChatFormatting.GOLD));
    }

    /** Labeled prefix: "  Key: " */
    private static MutableComponent label(String key) {
        return Component.literal("  " + key + ": ").withStyle(ChatFormatting.GRAY);
    }

    /** Clickable footer link */
    private static MutableComponent footerLink(String command, String label) {
        return Component.literal("")
                .append(Component.literal("  [" + label + "]").withStyle(s -> s
                        .withColor(ChatFormatting.DARK_AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Run " + command)))));
    }

    /* ── tick chip: compact status + compensation data with hover ── */

    /**
     * Builds a compensation chip showing toggle state + computed tick value.
     * <p>Format: {@code ✔ Name (value)} or {@code ✘ Name} when disabled / inactive.</p>
     *
     * @param name       display name
     * @param enabled    config toggle
     * @param active     whether compensation is currently active (TPS < 20)
     * @param valueText  computed compensation value like "×2.0" or "5t→3t"
     * @param hoverDesc  tooltip description
     */
    private static MutableComponent tickChip(String name, boolean enabled, boolean active,
                                             String valueText, String hoverDesc) {
        MutableComponent hover = Component.literal(name).withStyle(ChatFormatting.WHITE)
                .append(Component.literal("\n" + hoverDesc).withStyle(ChatFormatting.GRAY));

        if (!enabled) {
            hover.append(Component.literal("\n\nDisabled in config").withStyle(ChatFormatting.RED));
            return Component.literal("✘ " + name).withStyle(s -> s
                    .withColor(ChatFormatting.DARK_GRAY)
                    .withStrikethrough(true)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        }
        if (!active) {
            hover.append(Component.literal("\n\nEnabled · no compensation needed").withStyle(ChatFormatting.GREEN));
            return Component.literal("✔ " + name).withStyle(s -> s
                    .withColor(ChatFormatting.GREEN)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        }

        // Active compensation — show value
        hover.append(Component.literal("\n\nCompensation: ").withStyle(ChatFormatting.YELLOW))
             .append(Component.literal(valueText).withStyle(ChatFormatting.AQUA));

        return Component.literal("✔ " + name).withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" (" + valueText + ")").withStyle(s -> s
                        .withColor(ChatFormatting.AQUA)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))));
    }

    /* ── config toggle helpers ── */

    /** Single toggle row */
    private static MutableComponent toggleRow(String name, boolean enabled) {
        return Component.literal("    ").append(toggleChip(name, enabled));
    }

    /** Multiple toggle chips on one row */
    private static MutableComponent toggleRow(String[] names, boolean[] values) {
        MutableComponent line = Component.literal("    ");
        for (int i = 0; i < names.length; i++) {
            if (i > 0) line.append(lit("  ", ChatFormatting.DARK_GRAY));
            line.append(toggleChip(names[i], values[i]));
        }
        return line;
    }

    /** Config toggle chip: "✔ Name" / "✘ Name" with hover */
    private static MutableComponent toggleChip(String name, boolean enabled) {
        ChatFormatting color = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;
        String icon = enabled ? "✔ " : "✘ ";
        return Component.literal(icon + name).withStyle(s -> s
                .withColor(color)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal(name + ": ")
                                .append(enabled
                                        ? lit("Enabled", ChatFormatting.GREEN)
                                        : lit("Disabled", ChatFormatting.RED)))));
    }

    /* ── TPS bar ── */

    /** TPS progress bar with inline MSPT: "TPS: ■■■■■■□□□□ 10.0/20  MSPT: 100.0ms" */
    private static MutableComponent buildTpsBar(float tps, float mspt) {
        int filled = Math.round((tps / TpsHelper.MAX_TPS) * BAR_LENGTH);
        filled = Math.max(0, Math.min(BAR_LENGTH, filled));

        ChatFormatting barColor = tps >= 19.0F ? ChatFormatting.GREEN
                : tps >= 15.0F ? ChatFormatting.YELLOW
                : tps >= 10.0F ? ChatFormatting.GOLD
                : ChatFormatting.RED;

        ChatFormatting msptColor = mspt <= 40 ? ChatFormatting.GREEN
                : mspt <= 50 ? ChatFormatting.YELLOW
                : mspt <= 100 ? ChatFormatting.GOLD
                : ChatFormatting.RED;

        return lit("  TPS ", ChatFormatting.GRAY)
                .append(lit(BAR_FULL.repeat(filled), barColor))
                .append(lit(BAR_EMPTY.repeat(BAR_LENGTH - filled), ChatFormatting.DARK_GRAY))
                .append(Component.literal(String.format(" %.1f", tps)).withStyle(barColor))
                .append(lit("/20", ChatFormatting.DARK_GRAY))
                .append(lit("  MSPT ", ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.1f", mspt)).withStyle(msptColor))
                .append(lit("ms", ChatFormatting.DARK_GRAY));
    }
}
