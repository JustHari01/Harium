package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_sensor_shrieker;

import me.jellysquid.mods.lithium.common.block.entity.sleeping_sculk.GameEventListenerWithCallback;
import net.minecraft.world.event.Vibrations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Vibrations.VibrationListener.class)
public class VibrationListenerMixin implements GameEventListenerWithCallback {
    @Unique
    private Runnable lithium$callback;

    @Override
    public void lithium$setGameEventCallback(Runnable callback) {
        this.lithium$callback = callback;
    }

    @Inject(
            method = "listen(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/event/Vibrations$ListenerData;Lnet/minecraft/world/event/GameEvent;Lnet/minecraft/world/event/GameEvent$Emitter;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/event/listener/VibrationSelector;tryAccept(Lnet/minecraft/world/event/listener/Vibration;J)V")
    )
    private void onVibrationAccepted(CallbackInfo ci) {
        if (this.lithium$callback != null) {
            this.lithium$callback.run();
        }
    }
}
