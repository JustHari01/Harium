package me.jellysquid.mods.lithium.mixin.block.moving_block_shapes;

import me.jellysquid.mods.lithium.common.shapes.OffsetVoxelShapeCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PistonBlockEntity.class)
public abstract class PistonBlockEntityMixin {
    private static final VoxelShape[] PISTON_BASE_WITH_MOVING_HEAD_SHAPES = precomputePistonBaseWithMovingHeadShapes();

    @Shadow
    private Direction facing;
    @Shadow
    private boolean extending;
    @Shadow
    private boolean source;

    @Shadow
    private BlockState pushedBlock;

    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/Direction;getOffsetX()I",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void skipVoxelShapeUnion(BlockView world, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir, VoxelShape voxelShape, Direction direction, BlockState blockState, float f) {
        if (this.extending || !this.source || !(this.pushedBlock.getBlock() instanceof PistonBlock)) {

            VoxelShape blockShape = blockState.getCollisionShape(world, pos);

            VoxelShape offsetAndSimplified = getOffsetAndSimplified(blockShape, Math.abs(f), f < 0f ? this.facing.getOpposite() : this.facing);
            cir.setReturnValue(offsetAndSimplified);
        } else {

            int index = getIndexForMergedShape(f, this.facing);
            cir.setReturnValue(PISTON_BASE_WITH_MOVING_HEAD_SHAPES[index]);
        }
    }

    private static VoxelShape getOffsetAndSimplified(VoxelShape blockShape, float offset, Direction direction) {
        VoxelShape offsetSimplifiedShape = ((OffsetVoxelShapeCache) blockShape).getOffsetSimplifiedShape(offset, direction);
        if (offsetSimplifiedShape == null) {

            offsetSimplifiedShape = blockShape.offset(direction.getOffsetX() * offset, direction.getOffsetY() * offset, direction.getOffsetZ() * offset).simplify();
            ((OffsetVoxelShapeCache) blockShape).setShape(offset, direction, offsetSimplifiedShape);
        }
        return offsetSimplifiedShape;
    }

    private static VoxelShape[] precomputePistonBaseWithMovingHeadShapes() {
        float[] offsets = {0f, 0.5f, 1f};
        Direction[] directions = Direction.values();

        VoxelShape[] mergedShapes = new VoxelShape[offsets.length * directions.length];

        for (Direction facing : directions) {
            VoxelShape baseShape = Blocks.PISTON.getDefaultState().with(PistonBlock.EXTENDED, true)
                    .with(PistonBlock.FACING, facing).getCollisionShape(null, null);
            for (float offset : offsets) {

                boolean isShort = offset < 0.25f;

                VoxelShape headShape = (Blocks.PISTON_HEAD.getDefaultState().with(PistonHeadBlock.FACING, facing))
                        .with(PistonHeadBlock.SHORT, isShort).getCollisionShape(null, null);

                VoxelShape offsetHead = headShape.offset(facing.getOffsetX() * offset,
                        facing.getOffsetY() * offset,
                        facing.getOffsetZ() * offset);
                mergedShapes[getIndexForMergedShape(offset, facing)] = VoxelShapes.union(baseShape, offsetHead);
            }

        }

        return mergedShapes;
    }

    private static int getIndexForMergedShape(float offset, Direction direction) {
        if (offset != 0f && offset != 0.5f && offset != 1f) {
            return -1;
        }

        return (int) (2 * offset) + (3 * direction.getId());
    }
}
