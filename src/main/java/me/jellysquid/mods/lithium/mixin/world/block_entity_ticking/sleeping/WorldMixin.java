package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(World.class)
public class WorldMixin {

    @WrapOperation(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;shouldTickBlockPos(Lnet/minecraft/util/math/BlockPos;)Z"),
            require = 0
    )
    private boolean shouldTickBlockPosFilterNull(World instance, BlockPos pos, Operation<Boolean> original) {
        if (pos == null) {
            return false;
        }
        return original.call(instance, pos);
    }
}
