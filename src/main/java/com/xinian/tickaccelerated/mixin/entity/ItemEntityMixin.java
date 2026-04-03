package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dropped-item TPS compensation for both pickup delay and age/despawn timer.
 *
 * <h3>pickupDelay</h3>
 * <p>Vanilla: {@code pickupDelay--} per tick. We decrement extra so items become
 * pickable in the same real time.</p>
 *
 * <h3>age</h3>
 * <p>Vanilla: {@code age++} per tick, despawn at 6000. We increment extra so items
 * despawn in the correct real time (prevents item buildup at low TPS).</p>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow
    private int pickupDelay;

    @Shadow
    private int age;

    @Unique
    private static final int INFINITE_PICKUP_DELAY = 32767;

    @Unique
    private static final int INFINITE_AGE = -32768;

    @Unique
    private final float[] tickaccelerate$pickupAccum = new float[1];

    @Unique
    private final float[] tickaccelerate$ageAccum = new float[1];

    /**
     * After vanilla decrements pickupDelay by 1, we decrement it further.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensateItemEntity(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide()) return;

        MinecraftServer server = self.level().getServer();
        if (server == null) return;

        ConfigSnapshot config = ConfigSnapshot.get(server);
        float multiplier = TpsHelper.getSpeedMultiplier(server);
        if (multiplier <= 1.0F) return;

        if (config.enableItemPickupDelay
                && this.pickupDelay > 0 && this.pickupDelay != INFINITE_PICKUP_DELAY) {
            int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$pickupAccum);
            if (extra > 0) {
                this.pickupDelay = Math.max(0, this.pickupDelay - extra);
            }
        }

        if (config.enableItemDespawn
                && this.age != INFINITE_AGE && this.age > 0) {
            int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$ageAccum);
            if (extra > 0) {
                this.age += extra;
            }
        }
    }
}
