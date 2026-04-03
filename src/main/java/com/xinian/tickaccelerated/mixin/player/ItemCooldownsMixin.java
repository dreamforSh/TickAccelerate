package com.xinian.tickaccelerated.mixin.player;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ServerItemCooldowns;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Item cooldown TPS compensation (ender pearl, chorus fruit, wind charge, shield, etc).
 *
 * <h3>Vanilla logic (ItemCooldowns.addCooldown)</h3>
 * <pre>
 * public void addCooldown(Item item, int ticks) {
 *     this.cooldowns.put(item, new CooldownInstance(this.tickCount, this.tickCount + ticks));
 * }
 * </pre>
 * We scale the {@code ticks} parameter: {@code newTicks = ticks * tps / 20}.
 */
@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin {

    @ModifyVariable(method = "addCooldown", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int tickaccelerate$compensateCooldownTicks(int originalTicks) {
        if (!((Object) this instanceof ServerItemCooldowns)) return originalTicks;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return originalTicks;

        ConfigSnapshot config = ConfigSnapshot.get(server);
        if (!config.enableItemCooldown) return originalTicks;

        float tickFactor = TpsHelper.getTickFactor(server);
        if (tickFactor >= 1.0F) return originalTicks;
        return Math.max(1, Math.round(originalTicks * tickFactor));
    }
}
