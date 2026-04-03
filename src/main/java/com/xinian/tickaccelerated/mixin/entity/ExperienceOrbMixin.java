package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Experience orb age TPS compensation.
 *
 * <h3>Vanilla logic (ExperienceOrb.tick)</h3>
 * <pre>
 * this.age++;
 * if (this.age >= 6000) {
 *     this.discard();
 * }
 * </pre>
 * <p>At low TPS, orbs persist longer in real time, causing accumulation.
 * We increment age by extra ticks so orbs despawn in the correct real time.</p>
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @Shadow
    private int age;

    @Unique
    private final float[] tickaccelerate$ageAccum = new float[1];

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensateXpAge(CallbackInfo ci) {
        ExperienceOrb self = (ExperienceOrb) (Object) this;
        if (self.level().isClientSide()) return;

        MinecraftServer server = self.level().getServer();
        if (server == null) return;

        ConfigSnapshot config = ConfigSnapshot.get(server);
        if (!config.enableXpOrbAge) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$ageAccum);
        if (extra > 0) {
            this.age += extra;
        }
    }
}
