package me.jellysquid.mods.lithium.mixin.entity.data_tracker.no_locks;

import me.jellysquid.mods.lithium.common.util.lock.NullReadWriteLock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.ReadWriteLock;

@Mixin(value = DataTracker.class, priority = 1001)
public abstract class DataTrackerMixin {
    private static final NullReadWriteLock NULL_READ_WRITE_LOCK = new NullReadWriteLock();

    @Mutable
    @Shadow
    @Final
    private ReadWriteLock lock;

    @Inject(method = "<init>(Lnet/minecraft/entity/Entity;)V", at = @At("RETURN"))
    private void init(Entity entity, CallbackInfo ci) {
        this.lock = NULL_READ_WRITE_LOCK;
    }
}
