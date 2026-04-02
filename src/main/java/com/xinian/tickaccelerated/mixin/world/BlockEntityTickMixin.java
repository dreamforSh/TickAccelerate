package com.xinian.tickaccelerated.mixin.world;

import com.xinian.tickaccelerated.config.TickAccelerateConfig;
import com.xinian.tickaccelerated.util.TpsHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Block entity tick TPS compensation.
 *
 * <h3>Vanilla logic (Level.tickBlockEntities)</h3>
 * <pre>
 * while (iterator.hasNext()) {
 *     TickingBlockEntity tickingblockentity = iterator.next();
 *     ...
 *     tickingblockentity.tick();   // called once per game tick
 * }
 * </pre>
 * <p>Block entities (furnaces, hoppers, brewing stands, etc.) are ticked once
 * per game tick. At low TPS, they process slower than intended.
 * We call {@code tick()} extra times so they run at the correct real-time rate.</p>
 */
@Mixin(Level.class)
public abstract class BlockEntityTickMixin {

    @Redirect(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V")
    )
    private void tickaccelerate$compensateBlockEntityTick(TickingBlockEntity ticker) {
        // Always run vanilla's original tick
        ticker.tick();

        Level self = (Level) (Object) this;
        if (self.isClientSide()) return;

        try {
            if (!TickAccelerateConfig.INSTANCE.enableBlockEntityTick.get()) return;
        } catch (Exception e) {
            return;
        }

        MinecraftServer server = self.getServer();
        if (server == null) return;

        float multiplier = TpsHelper.getSpeedMultiplier(server);
        int extra = TpsHelper.computeExtraTicks(multiplier, self.getRandom().nextFloat());
        for (int i = 0; i < extra; i++) {
            if (ticker.isRemoved()) break;
            ticker.tick();
        }
    }
}

