package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity-level TPS compensation for portal cooldown, fire ticks,
 * and boarding cooldown.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract int getPortalCooldown();

    @Shadow
    public abstract void setPortalCooldown(int portalCooldown);

    @Shadow
    public abstract int getRemainingFireTicks();

    @Shadow
    public abstract void setRemainingFireTicks(int ticks);

    @Shadow
    protected int boardingCooldown;

    @Unique
    private final float[] tickaccelerate$portalAccum = new float[1];

    @Unique
    private final float[] tickaccelerate$fireAccum = new float[1];

    @Unique
    private final float[] tickaccelerate$boardingAccum = new float[1];

    /**
     * Compensate portal re-entry cooldown at the end of processPortalCooldown.
     */
    @Inject(method = "processPortalCooldown", at = @At("TAIL"))
    private void tickaccelerate$compensatePortalCooldown(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide()) return;
        if (this.getPortalCooldown() <= 0) return;
        try {
            if (!TickAccelerateConfig.INSTANCE.enablePortalCooldown.get()) return;
        } catch (Exception e) { return; }

        MinecraftServer server = self.level().getServer();
        if (server == null) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$portalAccum);
        if (extra > 0) {
            this.setPortalCooldown(Math.max(0, this.getPortalCooldown() - extra));
        }
    }

    /**
     * Compensate fire tick duration at the end of baseTick.
     * <p>Vanilla: {@code remainingFireTicks--} per tick. We decrement extra
     * so entities extinguish in the same real time.</p>
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void tickaccelerate$compensateBaseTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide()) return;

        MinecraftServer server = self.level().getServer();
        if (server == null) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        if (multiplier <= 1.0F) return;

        int fireTicks = this.getRemainingFireTicks();
        if (fireTicks > 0) {
            try {
                if (!TickAccelerateConfig.INSTANCE.enableFireTick.get()) return;
            } catch (Exception e) { return; }

            int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$fireAccum);
            if (extra > 0) {
                this.setRemainingFireTicks(Math.max(0, fireTicks - extra));
            }
        }

        if (this.boardingCooldown > 0) {
            try {
                if (!TickAccelerateConfig.INSTANCE.enableBoardingCooldown.get()) return;
            } catch (Exception e) { return; }

            int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$boardingAccum);
            if (extra > 0) {
                this.boardingCooldown = Math.max(0, this.boardingCooldown - extra);
            }
        }
    }
}
