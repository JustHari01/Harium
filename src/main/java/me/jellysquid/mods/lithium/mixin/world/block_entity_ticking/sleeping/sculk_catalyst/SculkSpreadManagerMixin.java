package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_catalyst;

import me.jellysquid.mods.lithium.common.block.entity.sleeping_sculk.GameEventListenerWithCallback;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkSpreadManager.class)
public class SculkSpreadManagerMixin implements GameEventListenerWithCallback {
    @Unique
    private Runnable lithium$callback;

    @Override
    public void lithium$setGameEventCallback(Runnable callback) {
        this.lithium$callback = callback;
    }

    @Inject(method = "spread", at = @At("RETURN"))
    private void onSpread(BlockPos pos, int charge, CallbackInfo ci) {
        if (this.lithium$callback != null) {
            this.lithium$callback.run();
        }
    }
}
