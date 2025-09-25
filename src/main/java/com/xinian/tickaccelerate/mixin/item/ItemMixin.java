package com.xinian.tickaccelerate.mixin.item;

import com.xinian.tickaccelerate.TickAccelerate;
import com.xinian.tickaccelerate.util.TPSUtil;
import com.xinian.tickaccelerate.util.TPSCalculator; // Added import
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true)
    private void onGetMaxUseTime(ItemStack p_41454_, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        if (!TickAccelerate.config.enabled() || !TickAccelerate.config.eatingAcceleration() || original == 0) return;
        cir.setReturnValue(TPSUtil.tt20(original, true, TPSCalculator.tpsMultiplier)); // Added tpsMultiplier
    }
}