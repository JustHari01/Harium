package me.jellysquid.mods.lithium.mixin.ai.pathing;

import me.jellysquid.mods.lithium.api.pathing.BlockPathing;
import me.jellysquid.mods.lithium.common.ai.pathing.PathNodeCache;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = LandPathNodeMaker.class, priority = 990)
public abstract class LandPathNodeMakerMixin {
    
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "getCommonNodeType",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/BlockView;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void getLithiumCachedCommonNodeType(BlockView world, BlockPos pos, CallbackInfoReturnable<PathNodeType> cir, BlockState blockState) {
        PathNodeType type;
        if (((BlockPathing) blockState.getBlock()).needsDynamicNodeTypeCheck()) {
            type = blockState.getBlockPathType(world, pos, null);

            if (type == null) {
                type = PathNodeCache.getPathNodeType(blockState);
            }
        } else {
            type = PathNodeCache.getPathNodeType(blockState);

            if (type != PathNodeType.LAVA && type != PathNodeType.DANGER_FIRE && ((BlockPathing) blockState.getBlock()).needsDynamicBurningCheck() && blockState.isBurning(world, pos)) {
                type = PathNodeType.DANGER_FIRE;
            }
        }

        if (type != null) {
            cir.setReturnValue(type);
        }
    }

    @Inject(
            method = "getNodeTypeFromNeighbors", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(
                    value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/util/math/BlockPos$Mutable;set(III)Lnet/minecraft/util/math/BlockPos$Mutable;"
            ),
            cancellable = true
    )
    private static void doNotChangePositionIfLithiumSinglePosCall(BlockView world, BlockPos.Mutable pos, PathNodeType nodeType, CallbackInfoReturnable<PathNodeType> cir, int posX, int posY, int posZ, int dX, int dY, int dZ) {
        if (nodeType == null) {
            if (dX == -1 && dY == -1 && dZ == -1) {
                pos.set(posX, posY, posZ);
            } else {
                cir.setReturnValue(null);
            }
        }
    }

    @Redirect(method = "getLandNodeType", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/pathing/LandPathNodeMaker;getNodeTypeFromNeighbors(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos$Mutable;Lnet/minecraft/entity/ai/pathing/PathNodeType;)Lnet/minecraft/entity/ai/pathing/PathNodeType;"))
    private static PathNodeType getNodeTypeFromNeighbors(BlockView world, BlockPos.Mutable pos, PathNodeType type) {
        return PathNodeCache.getNodeTypeFromNeighbors(world, pos, type);
    }
}
