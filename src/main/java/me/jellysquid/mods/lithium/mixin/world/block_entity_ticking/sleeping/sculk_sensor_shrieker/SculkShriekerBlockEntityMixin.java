package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_sensor_shrieker;

import me.jellysquid.mods.lithium.common.block.entity.SleepingBlockEntity;
import me.jellysquid.mods.lithium.common.block.entity.sleeping_sculk.GameEventListenerWithCallback;
import me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.WrappedBlockEntityTickInvokerAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SculkShriekerBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.event.Vibrations;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekerBlockEntityMixin extends BlockEntity implements SleepingBlockEntity, Vibrations {
    @Shadow
    @Final
    private Vibrations.VibrationListener listener;

    @Shadow
    private Vibrations.ListenerData listenerData;

    private WrappedBlockEntityTickInvokerAccessor tickWrapper = null;
    private BlockEntityTickInvoker sleepingTicker = null;

    public SculkShriekerBlockEntityMixin(BlockPos pos, BlockState state) {
        super(null, pos, state);
    }

    @Override
    public WrappedBlockEntityTickInvokerAccessor lithium$getTickWrapper() {
        return tickWrapper;
    }

    @Override
    public void lithium$setTickWrapper(WrappedBlockEntityTickInvokerAccessor tickWrapper) {
        this.tickWrapper = tickWrapper;
        this.lithium$setSleepingTicker(null);
    }

    @Override
    public BlockEntityTickInvoker lithium$getSleepingTicker() {
        return sleepingTicker;
    }

    @Override
    public void lithium$setSleepingTicker(BlockEntityTickInvoker sleepingTicker) {
        this.sleepingTicker = sleepingTicker;
    }

    @Inject(method = "<init>(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", at = @At("RETURN"))
    private void setVibrationListenerCallback(BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        ((GameEventListenerWithCallback) this.listener).lithium$setGameEventCallback(this::wakeUpNow);
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    private void wakeupIfLoadedWithData(CallbackInfo ci) {
        if (this.listenerData != null && this.listenerData.getSelector() != null
                && this.listenerData.getSelector().getVibrationToTick(Long.MAX_VALUE).isPresent()) {
            this.wakeUpNow();
        }
    }
}
