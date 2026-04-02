package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Portal re-entry cooldown TPS compensation.
 *
 * <h3>Vanilla logic (Entity.processPortalCooldown)</h3>
 * <pre>
 * protected void processPortalCooldown() {
 *     if (this.isOnPortalCooldown()) {
 *         this.portalCooldown--;
 *     }
 * }
 * </pre>
 * <p>After teleportation, entities receive a cooldown (300 ticks for entities,
 * 10 ticks for players) before they can use a portal again.
 * At low TPS, this takes longer in real time.
 * We decrement extra ticks so the cooldown expires in the same real time.</p>
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract int getPortalCooldown();

    @Shadow
    public abstract void setPortalCooldown(int portalCooldown);

    @Inject(method = "processPortalCooldown", at = @At("TAIL"))
    private void tickaccelerate$compensatePortalCooldown(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide()) return;
        if (this.getPortalCooldown() <= 0) return;

        try {
            if (!TickAccelerateConfig.INSTANCE.enablePortalCooldown.get()) return;
        } catch (Exception e) {
            return;
        }

        MinecraftServer server = self.level().getServer();
        if (server == null) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
        if (extra > 0) {
            int newCooldown = Math.max(0, this.getPortalCooldown() - extra);
            this.setPortalCooldown(newCooldown);
        }
    }
}

