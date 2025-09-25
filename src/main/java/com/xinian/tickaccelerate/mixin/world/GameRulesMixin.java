package com.xinian.tickaccelerate.mixin.world;

import com.xinian.tickaccelerate.TickAccelerate;
import com.xinian.tickaccelerate.util.TPSCalculator;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRules.class)
public class GameRulesMixin {

    @Inject(method = "getInt", at = @At("RETURN"), cancellable = true)
    private void randomTickSpeedAcceleration(GameRules.Key<GameRules.IntegerValue> rule, CallbackInfoReturnable<Integer> cir) {
        if (!TickAccelerate.config.enabled() || !TickAccelerate.config.randomTickSpeedAcceleration()) return;
        if (rule != GameRules.RULE_RANDOMTICKING) return;

        int original = cir.getReturnValue();
        cir.setReturnValue((int) (original * TPSCalculator.tpsMultiplier));
    }
}