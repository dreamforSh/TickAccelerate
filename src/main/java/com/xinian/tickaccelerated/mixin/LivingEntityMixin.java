package com.xinian.tickaccelerated.mixin;

import com.xinian.tickaccelerated.TickAccelerateConfig;
import com.xinian.tickaccelerated.TpsHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Item use duration TPS compensation (eating, drinking, bow, crossbow, etc).
 * <p>Each tick reduces {@code useItemRemaining} by {@code 20 / tps} instead of 1.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected int useItemRemaining;

    @Shadow
    public abstract boolean isUsingItem();

    /**
     * After vanilla decrements {@code useItemRemaining} by 1, apply extra reduction.
     */
    @Inject(method = "updateUsingItem", at = @At("TAIL"))
    private void tickaccelerate$compensateItemUse(ItemStack usingItem, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer)) return;
        if (!this.isUsingItem()) return;
        if (!TickAccelerateConfig.INSTANCE.enableItemUse.get()) return;

        float multiplier = TpsHelper.getSpeedMultiplier(self);
        if (multiplier <= 1.0F) return;

        int extraTicks = (int) (multiplier - 1.0F);
        float fraction = multiplier - 1.0F - extraTicks;
        if (fraction > 0 && self.getRandom().nextFloat() < fraction) {
            extraTicks++;
        }
        if (extraTicks > 0 && this.useItemRemaining > 0) {
            this.useItemRemaining = Math.max(0, this.useItemRemaining - extraTicks);
        }
    }
}
