package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Animal breeding "in love" timer TPS compensation.
 *
 * <h3>Vanilla logic (Animal.aiStep)</h3>
 * <pre>
 * if (this.inLove > 0) {
 *     this.inLove--;
 *     // heart particles every 10 ticks
 * }
 * </pre>
 * <p>At low TPS, the breeding window ({@code inLove = 600}, 30 seconds) lasts
 * much longer in real time. We decrement extra ticks so the love mode
 * expires in the correct real time.</p>
 */
@Mixin(Animal.class)
public abstract class AnimalMixin {

    @Shadow
    private int inLove;

    @Unique
    private final float[] tickaccelerate$loveAccum = new float[1];

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void tickaccelerate$compensateBreedingTimer(CallbackInfo ci) {
        Animal self = (Animal) (Object) this;
        if (self.level().isClientSide()) return;
        if (this.inLove <= 0) return;

        MinecraftServer server = self.level().getServer();
        if (server == null) return;

        ConfigSnapshot config = ConfigSnapshot.get(server);
        if (!config.enableBreedingTimer) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$loveAccum);
        if (extra > 0) {
            this.inLove = Math.max(0, this.inLove - extra);
        }
    }
}
