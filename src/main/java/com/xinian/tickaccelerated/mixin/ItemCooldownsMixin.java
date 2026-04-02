package com.xinian.tickaccelerated.mixin;

import com.xinian.tickaccelerated.TickAccelerateConfig;
import com.xinian.tickaccelerated.TpsHelper;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ServerItemCooldowns;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Item cooldown TPS compensation (ender pearl, chorus fruit, wind charge, shield, etc).
 * <p>Scales cooldown tick count: {@code newTicks = originalTicks * tps / 20}.</p>
 */
@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin {

    /**
     * Modifies the cooldown tick parameter in {@code addCooldown}. Server-side only.
     */
    @ModifyVariable(method = "addCooldown", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int tickaccelerate$compensateCooldownTicks(int originalTicks) {
        if (!((Object) this instanceof ServerItemCooldowns)) return originalTicks;
        if (!TickAccelerateConfig.INSTANCE.enableItemCooldown.get()) return originalTicks;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return originalTicks;
        float tickFactor = TpsHelper.getTickFactor(server);
        if (tickFactor >= 1.0F) return originalTicks;
        return Math.max(1, Math.round(originalTicks * tickFactor));
    }
}
