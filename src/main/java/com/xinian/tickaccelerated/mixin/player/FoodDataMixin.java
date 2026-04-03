package com.xinian.tickaccelerated.mixin.player;

import com.xinian.tickaccelerated.config.ConfigSnapshot;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Natural regeneration / starvation timer TPS compensation.
 *
 * <h3>Vanilla logic (FoodData.tick)</h3>
 * <pre>
 * this.tickTimer++;
 * if (this.tickTimer >= threshold) { heal / starve; this.tickTimer = 0; }
 * </pre>
 * <p>We add extra increments to {@code tickTimer} so healing/starvation
 * happens in the same real time regardless of TPS.</p>
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow
    private int tickTimer;

    @Unique
    private final float[] tickaccelerate$foodAccum = new float[1];

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickaccelerate$compensateFoodTimer(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        if (this.tickTimer <= 0) return;

        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        ConfigSnapshot config = ConfigSnapshot.get(server);
        if (!config.enableFoodRegen) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        int extra = TpsHelper.computeExtraTicksDeterministic(multiplier, this.tickaccelerate$foodAccum);
        if (extra > 0) {
            this.tickTimer += extra;
        }
    }
}
