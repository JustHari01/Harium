package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_catalyst;

import me.jellysquid.mods.lithium.common.block.entity.SleepingBlockEntity;
import me.jellysquid.mods.lithium.common.block.entity.sleeping_sculk.GameEventListenerWithCallback;
import me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.WrappedBlockEntityTickInvokerAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SculkCatalystBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkCatalystBlockEntity.class)
public abstract class SculkCatalystBlockEntityMixin extends BlockEntity implements SleepingBlockEntity {

    private WrappedBlockEntityTickInvokerAccessor tickWrapper = null;
    private BlockEntityTickInvoker sleepingTicker = null;

    public SculkCatalystBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "<init>(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", at = @At("RETURN"))
    private void setWakeCallback(BlockPos pos, BlockState state, CallbackInfo ci) {
        SculkCatalystBlockEntity self = (SculkCatalystBlockEntity) (Object) this;
        ((GameEventListenerWithCallback) self.getEventListener().getSpreadManager()).lithium$setGameEventCallback(this::wakeUpNow);
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

    @Inject(method = "tick", at = @At("RETURN"))
    private static void checkSleep(World world, BlockPos pos, BlockState state, SculkCatalystBlockEntity blockEntity, CallbackInfo ci) {
        if (!world.isClient) {
            SculkCatalystBlockEntity self = blockEntity;
            if (self.getEventListener().getSpreadManager().getCursors().isEmpty()) {
                ((SleepingBlockEntity) self).lithium$startSleeping();
            }
        }
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    private void wakeUpAfterFromTag(CallbackInfo ci) {
        if (this.isSleeping() && this.world != null && !this.world.isClient) {
            this.wakeUpNow();
        }
    }
}
