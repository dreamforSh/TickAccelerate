package com.xinian.tickaccelerated.mixin.entity;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TNT fuse timer TPS compensation.
 *
 * <h3>Vanilla logic (PrimedTnt.tick)</h3>
 * <pre>
 * int i = this.getFuse() - 1;
 * this.setFuse(i);
 * if (i &lt;= 0) { this.discard(); this.explode(); }
 * </pre>
 * <p>At low TPS, the fuse (default 80 ticks = 4 seconds) takes longer in
 * real time. We decrement extra ticks so TNT explodes at the correct
 * real-time delay.</p>
 */
@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin {

    @Unique
    private final float[] tickaccelerate$fuseAccum = new float[1];

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensateTntFuse(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        if (self.level().isClientSide()) return;

        MinecraftServer server = self.level().getServer();
        if (server == null) return;

        ConfigSnapshot config = ConfigSnapshot.get(server);
        if (!config.enableTntFuse) return;

        int fuse = self.getFuse();
        if (fuse <= 1) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$fuseAccum);
        if (extra > 0) {
            self.setFuse(Math.max(1, fuse - extra));
        }
    }
}
