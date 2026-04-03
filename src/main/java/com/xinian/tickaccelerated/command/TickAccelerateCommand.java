package com.xinian.tickaccelerated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.ServerI18n;
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
 *   <li>{@code /ta} or {@code /ta status} — live performance & compensation dashboard</li>
 *   <li>{@code /ta config} — list all compensation toggle states</li>
 * </ul>
 *
 * <p>All user-facing text is resolved server-side via {@link ServerI18n} and sent as
 * literal components, ensuring proper display on clients without the mod installed.</p>
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
        src.sendSuccess(() -> header("tickaccelerate.cmd.title"), false);

        // ── TPS bar + MSPT inline ──
        src.sendSuccess(() -> buildTpsBar(tps, mspt), false);

        // ── compensation overview ──
        if (!active) {
            src.sendSuccess(() ->
                    label("tickaccelerate.cmd.status")
                            .append(tr("tickaccelerate.cmd.status.normal", ChatFormatting.GREEN))
                            .append(tr("tickaccelerate.cmd.status.normal_desc", ChatFormatting.DARK_GRAY)),
                    false);
            src.sendSuccess(() -> footerLink("/ta config", "tickaccelerate.cmd.link.view_config"), false);
            return 1;
        }

        src.sendSuccess(() ->
                label("tickaccelerate.cmd.status")
                        .append(tr("tickaccelerate.cmd.status.compensating", ChatFormatting.YELLOW))
                        .append(lit("  ×", ChatFormatting.DARK_GRAY))
                        .append(lit(String.format("%.2f", mult), ChatFormatting.AQUA))
                        .append(lit(ServerI18n.get("tickaccelerate.cmd.status.factor",
                                String.format("%.2f", factor)), ChatFormatting.DARK_GRAY)),
                false);

        // ── Player compensations ──
        src.sendSuccess(() -> sectionHeader("tickaccelerate.cmd.section.player"), false);
        {
            MutableComponent line = Component.literal("    ");
            line.append(tickChip("block_break", cfg.enableBlockBreaking.get(), active,
                    String.format("%.1f", mult)));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            line.append(tickChip("attack_cd", cfg.enableAttackCooldown.get(), active,
                    String.format("%.1f", mult)));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            line.append(tickChip("food", cfg.enableFoodRegen.get(), active,
                    String.format("%.1f", mult)));
            src.sendSuccess(() -> line, false);

            MutableComponent line2 = Component.literal("    ");
            int itemUseTicks = Math.max(1, Math.round(32 * factor));
            line2.append(tickChip("item_use", cfg.enableItemUse.get(), active,
                    "32", String.valueOf(itemUseTicks)));
            line2.append(lit("  ", ChatFormatting.DARK_GRAY));
            int cooldownTicks = Math.max(1, Math.round(20 * factor));
            line2.append(tickChip("item_cd", cfg.enableItemCooldown.get(), active,
                    "20", String.valueOf(cooldownTicks)));
            line2.append(lit("  ", ChatFormatting.DARK_GRAY));
            line2.append(tickChip("xp_delay", cfg.enableXpPickupDelay.get(), active,
                    String.format("%.1f", mult)));
            src.sendSuccess(() -> line2, false);

            MutableComponent line3 = Component.literal("    ");
            int sleepTicks = Math.max(1, Math.round(100 * factor));
            line3.append(tickChip("sleep", cfg.enableSleepTimer.get(), active,
                    "100", String.valueOf(sleepTicks)));
            src.sendSuccess(() -> line3, false);
        }

        // ── Entity compensations ──
        src.sendSuccess(() -> sectionHeader("tickaccelerate.cmd.section.entity"), false);
        {
            MutableComponent line = Component.literal("    ");
            line.append(tickChip("potion", cfg.enablePotionEffect.get(), active,
                    String.format("%.1f", mult)));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            int pickupTicks = Math.max(0, Math.round(10 * factor));
            line.append(tickChip("pickup", cfg.enableItemPickupDelay.get(), active,
                    "10", String.valueOf(pickupTicks)));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            line.append(tickChip("portal", cfg.enablePortalTime.get(), active,
                    String.format("%.1f", mult)));
            src.sendSuccess(() -> line, false);

            MutableComponent line2 = Component.literal("    ");
            int hurtTicks = Math.max(0, Math.round(10 * factor));
            line2.append(tickChip("hurt", cfg.enableHurtTime.get(), active,
                    "10", String.valueOf(hurtTicks)));
            line2.append(lit("  ", ChatFormatting.DARK_GRAY));
            int deathTicks = Math.max(1, Math.round(20 * factor));
            line2.append(tickChip("death", cfg.enableDeathTime.get(), active,
                    "20", String.valueOf(deathTicks)));
            line2.append(lit("  ", ChatFormatting.DARK_GRAY));
            line2.append(tickChip("air", cfg.enableAirSupply.get(), active,
                    String.format("%.1f", mult)));
            src.sendSuccess(() -> line2, false);

            MutableComponent line3 = Component.literal("    ");
            int swingDur = Math.max(1, Math.round(6 * factor));
            line3.append(tickChip("swing", cfg.enableSwingSpeed.get(), active,
                    "6", String.valueOf(swingDur)));
            line3.append(lit("  ", ChatFormatting.DARK_GRAY));
            int iFrames = Math.max(0, Math.round(20 * factor));
            line3.append(tickChip("iframes", cfg.enableInvulnerability.get(), active,
                    "20", String.valueOf(iFrames)));
            line3.append(lit("  ", ChatFormatting.DARK_GRAY));
            int fireDur = Math.max(1, Math.round(160 * factor));
            line3.append(tickChip("fire", cfg.enableFireTick.get(), active,
                    "160", String.valueOf(fireDur)));
            src.sendSuccess(() -> line3, false);

            MutableComponent line4 = Component.literal("    ");
            int boardDur = Math.max(1, Math.round(60 * factor));
            line4.append(tickChip("boarding", cfg.enableBoardingCooldown.get(), active,
                    "60", String.valueOf(boardDur)));
            line4.append(lit("  ", ChatFormatting.DARK_GRAY));
            int despawnDur = Math.max(1, Math.round(6000 * factor));
            line4.append(tickChip("despawn", cfg.enableItemDespawn.get(), active,
                    "6000", String.valueOf(despawnDur)));
            line4.append(lit("  ", ChatFormatting.DARK_GRAY));
            line4.append(tickChip("xp_orb", cfg.enableXpOrbAge.get(), active,
                    String.format("%.1f", mult)));
            src.sendSuccess(() -> line4, false);

            MutableComponent line5 = Component.literal("    ");
            int arrowDur = Math.max(1, Math.round(1200 * factor));
            line5.append(tickChip("arrow", cfg.enableArrowLife.get(), active,
                    "1200", String.valueOf(arrowDur)));
            line5.append(lit("  ", ChatFormatting.DARK_GRAY));
            int breedDur = Math.max(1, Math.round(600 * factor));
            line5.append(tickChip("breed", cfg.enableBreedingTimer.get(), active,
                    "600", String.valueOf(breedDur)));
            line5.append(lit("  ", ChatFormatting.DARK_GRAY));
            line5.append(tickChip("growth", cfg.enableMobGrowth.get(), active,
                    String.format("%.1f", mult)));
            src.sendSuccess(() -> line5, false);
        }

        // ── World compensations ──
        src.sendSuccess(() -> sectionHeader("tickaccelerate.cmd.section.world"), false);
        {
            MutableComponent line = Component.literal("    ");
            int waterDelay = Math.max(1, Math.round(5 * factor));
            int lavaDelay  = Math.max(1, Math.round(30 * factor));
            line.append(tickChip("water", cfg.enableFluidSpeed.get(), active,
                    "5", String.valueOf(waterDelay)));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            line.append(tickChip("lava", cfg.enableFluidSpeed.get(), active,
                    "30", String.valueOf(lavaDelay)));
            line.append(lit("  ", ChatFormatting.DARK_GRAY));
            int rtScaled = Math.round(randomTickBase * mult);
            line.append(tickChip("rand_tick", cfg.enableRandomTick.get(), active,
                    String.valueOf(randomTickBase), String.valueOf(rtScaled)));
            src.sendSuccess(() -> line, false);

            MutableComponent line2 = Component.literal("    ");
            int portalCD = Math.max(0, Math.round(10 * factor));
            line2.append(tickChip("portal_cd", cfg.enablePortalCooldown.get(), active,
                    "10", String.valueOf(portalCD)));
            line2.append(lit("  ", ChatFormatting.DARK_GRAY));
            line2.append(tickChip("day_time", cfg.enableDayTime.get(), active,
                    String.format("%.1f", mult)));
            line2.append(lit("  ", ChatFormatting.DARK_GRAY));
            line2.append(tickChip("block_entity", cfg.enableBlockEntityTick.get(), active,
                    String.format("%.1f", mult)));
            src.sendSuccess(() -> line2, false);
        }

        // ── Client animation compensations ──
        src.sendSuccess(() -> sectionHeader("tickaccelerate.cmd.section.client"), false);
        {
            MutableComponent line = Component.literal("    ");
            line.append(tickChip("animations", cfg.enableClientAnimations.get(), active,
                    String.format("%.1f", mult)));
            src.sendSuccess(() -> line, false);
        }

        src.sendSuccess(() -> footerLink("/ta config", "tickaccelerate.cmd.link.view_config"), false);
        return 1;
    }

    /* ══════════════════ /ta config ══════════════════ */

    private static int showConfig(CommandContext<CommandSourceStack> ctx) {
        TickAccelerateConfig cfg = TickAccelerateConfig.INSTANCE;
        CommandSourceStack src = ctx.getSource();

        src.sendSuccess(() -> header("tickaccelerate.cmd.config_title"), false);

        // General
        src.sendSuccess(() ->
                label("tickaccelerate.cmd.config.min_tps")
                        .append(lit(String.format("%.1f", cfg.minTps.get()), ChatFormatting.WHITE)),
                false);

        // ── Server ──
        src.sendSuccess(() -> sectionHeader("tickaccelerate.cmd.section.server"), false);
        src.sendSuccess(() -> toggleRow("watchdog", cfg.disableWatchdog.get()), false);

        // ── Player ──
        src.sendSuccess(() -> sectionHeader("tickaccelerate.cmd.section.player"), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"block_break", "attack_cd", "food"},
                new boolean[]{cfg.enableBlockBreaking.get(), cfg.enableAttackCooldown.get(), cfg.enableFoodRegen.get()}
        ), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"item_use", "item_cd", "xp_delay"},
                new boolean[]{cfg.enableItemUse.get(), cfg.enableItemCooldown.get(), cfg.enableXpPickupDelay.get()}
        ), false);
        src.sendSuccess(() -> toggleRow("sleep", cfg.enableSleepTimer.get()), false);

        // ── Entity ──
        src.sendSuccess(() -> sectionHeader("tickaccelerate.cmd.section.entity"), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"potion", "pickup", "portal"},
                new boolean[]{cfg.enablePotionEffect.get(), cfg.enableItemPickupDelay.get(), cfg.enablePortalTime.get()}
        ), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"hurt", "death", "air"},
                new boolean[]{cfg.enableHurtTime.get(), cfg.enableDeathTime.get(), cfg.enableAirSupply.get()}
        ), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"swing", "iframes", "fire"},
                new boolean[]{cfg.enableSwingSpeed.get(), cfg.enableInvulnerability.get(), cfg.enableFireTick.get()}
        ), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"boarding", "despawn", "xp_orb"},
                new boolean[]{cfg.enableBoardingCooldown.get(), cfg.enableItemDespawn.get(), cfg.enableXpOrbAge.get()}
        ), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"arrow", "breed", "growth"},
                new boolean[]{cfg.enableArrowLife.get(), cfg.enableBreedingTimer.get(), cfg.enableMobGrowth.get()}
        ), false);

        // ── World ──
        src.sendSuccess(() -> sectionHeader("tickaccelerate.cmd.section.world"), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"water", "rand_tick", "portal_cd"},
                new boolean[]{cfg.enableFluidSpeed.get(), cfg.enableRandomTick.get(), cfg.enablePortalCooldown.get()}
        ), false);
        src.sendSuccess(() -> toggleRow(
                new String[]{"day_time", "block_entity"},
                new boolean[]{cfg.enableDayTime.get(), cfg.enableBlockEntityTick.get()}
        ), false);

        // ── Client ──
        src.sendSuccess(() -> sectionHeader("tickaccelerate.cmd.section.client"), false);
        src.sendSuccess(() -> toggleRow("animations", cfg.enableClientAnimations.get()), false);

        src.sendSuccess(() -> footerLink("/ta status", "tickaccelerate.cmd.link.view_status"), false);
        return 1;
    }

    /* ══════════════════ Formatting helpers ══════════════════ */

    /**
     * Shorthand: styled literal.
     */
    private static MutableComponent lit(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    /**
     * Shorthand: server-resolved translatable as literal component.
     */
    private static MutableComponent tr(String key, ChatFormatting color) {
        return Component.literal(ServerI18n.get(key)).withStyle(color);
    }

    /**
     * Gold bold header line.
     */
    private static MutableComponent header(String translationKey) {
        return lit("  ══ ", ChatFormatting.DARK_GRAY)
                .append(Component.literal(ServerI18n.get(translationKey))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(lit(" ══", ChatFormatting.DARK_GRAY));
    }

    /**
     * Section sub-header: "  ▸ Name".
     */
    private static MutableComponent sectionHeader(String translationKey) {
        return lit("  ▸ ", ChatFormatting.GOLD)
                .append(Component.literal(ServerI18n.get(translationKey)).withStyle(ChatFormatting.GOLD));
    }

    /**
     * Labeled prefix: "  Key: ".
     */
    private static MutableComponent label(String translationKey) {
        return Component.literal("  ")
                .append(Component.literal(ServerI18n.get(translationKey)))
                .append(Component.literal(": "))
                .withStyle(ChatFormatting.GRAY);
    }

    /**
     * Clickable footer link.
     */
    private static MutableComponent footerLink(String command, String labelKey) {
        return Component.literal("")
                .append(Component.literal("  [")
                        .append(Component.literal(ServerI18n.get(labelKey)))
                        .append(Component.literal("]"))
                        .withStyle(s -> s
                                .withColor(ChatFormatting.DARK_AQUA)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal(ServerI18n.get("tickaccelerate.cmd.link.run", command))))));
    }

    /* ── tick chip: compact status + compensation data with hover ── */

    /**
     * Builds a compensation chip showing toggle state + computed tick value.
     * <p>Format: {@code ✔ Name (value)} or {@code ✘ Name} when disabled / inactive.</p>
     *
     * @param featureKey feature identifier (e.g. "block_break", "item_use")
     * @param enabled    config toggle
     * @param active     whether compensation is currently active (TPS < 20)
     * @param valueArgs  arguments for the value format pattern
     */
    private static MutableComponent tickChip(String featureKey, boolean enabled, boolean active,
                                             Object... valueArgs) {
        String name = ServerI18n.get("tickaccelerate.feature." + featureKey);
        String value = ServerI18n.get("tickaccelerate.val." + featureKey, valueArgs);

        MutableComponent hover = Component.literal(name)
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("\n")
                        .append(Component.literal(ServerI18n.get("tickaccelerate.hover." + featureKey)))
                        .withStyle(ChatFormatting.GRAY));

        if (!enabled) {
            hover.append(Component.literal("\n\n")
                    .append(Component.literal(ServerI18n.get("tickaccelerate.cmd.chip.disabled")))
                    .withStyle(ChatFormatting.RED));
            return Component.literal("✘ ").append(Component.literal(name)).withStyle(s -> s
                    .withColor(ChatFormatting.DARK_GRAY)
                    .withStrikethrough(true)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        }
        if (!active) {
            hover.append(Component.literal("\n\n")
                    .append(Component.literal(ServerI18n.get("tickaccelerate.cmd.chip.enabled_idle")))
                    .withStyle(ChatFormatting.GREEN));
            return Component.literal("✔ ").append(Component.literal(name)).withStyle(s -> s
                    .withColor(ChatFormatting.GREEN)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        }

        hover.append(Component.literal("\n\n")
                .append(Component.literal(ServerI18n.get("tickaccelerate.cmd.chip.compensation")))
                .withStyle(ChatFormatting.YELLOW))
             .append(Component.literal(value).withStyle(ChatFormatting.AQUA));

        return Component.literal("✔ ").append(Component.literal(name)).withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" (").append(Component.literal(value)).append(Component.literal(")"))
                        .withStyle(s -> s
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))));
    }

    /* ── config toggle helpers ── */

    /**
     * Single toggle row.
     */
    private static MutableComponent toggleRow(String featureKey, boolean enabled) {
        return Component.literal("    ").append(toggleChip(featureKey, enabled));
    }

    /**
     * Multiple toggle chips on one row.
     */
    private static MutableComponent toggleRow(String[] featureKeys, boolean[] values) {
        MutableComponent line = Component.literal("    ");
        for (int i = 0; i < featureKeys.length; i++) {
            if (i > 0) line.append(lit("  ", ChatFormatting.DARK_GRAY));
            line.append(toggleChip(featureKeys[i], values[i]));
        }
        return line;
    }

    /**
     * Config toggle chip: "✔ Name" / "✘ Name" with hover.
     */
    private static MutableComponent toggleChip(String featureKey, boolean enabled) {
        ChatFormatting color = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;
        String icon = enabled ? "✔ " : "✘ ";
        String name = ServerI18n.get("tickaccelerate.feature." + featureKey);
        String stateText = enabled
                ? ServerI18n.get("tickaccelerate.cmd.toggle.enabled")
                : ServerI18n.get("tickaccelerate.cmd.toggle.disabled");
        ChatFormatting stateColor = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;

        MutableComponent hoverText = Component.literal(name)
                .append(Component.literal(": "))
                .append(Component.literal(stateText).withStyle(stateColor));

        return Component.literal(icon).append(Component.literal(name)).withStyle(s -> s
                .withColor(color)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
    }

    /* ── TPS bar ── */

    /**
     * TPS progress bar with inline MSPT: "TPS: ■■■■■■□□□□ 10.0/20  MSPT: 100.0ms"
     */
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
