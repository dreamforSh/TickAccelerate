package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Falling block entity time counter TPS compensation.
 *
 * <h3>Vanilla logic (FallingBlockEntity.tick)</h3>
 * <pre>
 * this.time++;
 * // ... landing / conversion checks use this.time
 * </pre>
 * <p>The {@code time} field tracks how long the entity has been falling.
 * At low TPS it increments slower (in real time), delaying landing
 * conversion. We increment extra ticks to keep real-time behavior.</p>
 */
@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockMixin {

    @Shadow
    public int time;

    @Unique
    private final float[] tickaccelerate$timeAccum = new float[1];

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensateFallingBlock(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (self.level().isClientSide()) return;

        MinecraftServer server = self.level().getServer();
        if (server == null) return;

        ConfigSnapshot config = ConfigSnapshot.get(server);
        if (!config.enableFallingBlock) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$timeAccum);
        if (extra > 0) {
            this.time += extra;
        }
    }
}
