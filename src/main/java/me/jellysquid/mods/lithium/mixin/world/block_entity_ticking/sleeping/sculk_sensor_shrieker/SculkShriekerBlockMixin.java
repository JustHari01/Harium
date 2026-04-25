package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.sculk_sensor_shrieker;

import me.jellysquid.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkShriekerBlock;
import net.minecraft.block.entity.SculkShriekerBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.Vibrations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkShriekerBlock.class)
public abstract class SculkShriekerBlockMixin {
    @Inject(method = "method_41366", at = @At(value = "RETURN"))
    private static void checkSleep(World world, BlockPos blockPos, BlockState blockState, SculkShriekerBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClient()) return;
        Vibrations.ListenerData vibrationData = blockEntity.getVibrationListenerData();
        if (vibrationData.getVibration() == null &&
                vibrationData.getSelector().getVibrationToTick(Long.MAX_VALUE).isEmpty()) {
            ((SleepingBlockEntity) blockEntity).lithium$startSleeping();
        }
    }
}
