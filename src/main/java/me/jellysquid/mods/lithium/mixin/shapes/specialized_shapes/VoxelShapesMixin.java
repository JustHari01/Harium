package me.jellysquid.mods.lithium.mixin.shapes.specialized_shapes;

import me.jellysquid.mods.lithium.common.shapes.VoxelShapeAlignedCuboid;
import me.jellysquid.mods.lithium.common.shapes.VoxelShapeEmpty;
import me.jellysquid.mods.lithium.common.shapes.VoxelShapeSimpleCube;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.*;

@Mixin(VoxelShapes.class)
public abstract class VoxelShapesMixin {
    @Mutable
    @Shadow
    @Final
    public static final VoxelShape UNBOUNDED;

    @Mutable
    @Shadow
    @Final
    private static final VoxelShape FULL_CUBE;

    @Mutable
    @Shadow
    @Final
    private static final VoxelShape EMPTY;

    private static final VoxelSet FULL_CUBE_VOXELS;

    static {

        FULL_CUBE_VOXELS = new BitSetVoxelSet(1, 1, 1);
        FULL_CUBE_VOXELS.set(0, 0, 0);

        UNBOUNDED = new VoxelShapeSimpleCube(FULL_CUBE_VOXELS, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

        FULL_CUBE = new VoxelShapeSimpleCube(FULL_CUBE_VOXELS, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

        EMPTY = new VoxelShapeEmpty(new BitSetVoxelSet(0, 0, 0));
    }

    @Overwrite
    public static VoxelShape cuboidUnchecked(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (maxX - minX < 1.0E-7D || maxY - minY < 1.0E-7D || maxZ - minZ < 1.0E-7D) {
            return EMPTY;
        }

        int xRes;
        int yRes;
        int zRes;

        if ((xRes = VoxelShapes.findRequiredBitResolution(minX, maxX)) < 0 ||
                (yRes = VoxelShapes.findRequiredBitResolution(minY, maxY)) < 0 ||
                (zRes = VoxelShapes.findRequiredBitResolution(minZ, maxZ)) < 0) {

            return new VoxelShapeSimpleCube(FULL_CUBE_VOXELS, minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            if (xRes == 0 && yRes == 0 && zRes == 0) {
                return FULL_CUBE;
            }

            return new VoxelShapeAlignedCuboid(Math.round(minX * 8D) / 8D, Math.round(minY * 8D) / 8D, Math.round(minZ * 8D) / 8D,
                    Math.round(maxX * 8D) / 8D, Math.round(maxY * 8D) / 8D, Math.round(maxZ * 8D) / 8D, xRes, yRes, zRes);
        }
    }
}
