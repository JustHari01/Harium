package me.jellysquid.mods.lithium.common.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.util.math.Direction.Axis.*;

public class VoxelShapeMatchesAnywhere {

    public static void cuboidMatchesAnywhere(VoxelShape shapeA, VoxelShape shapeB, BooleanBiFunction predicate, CallbackInfoReturnable<Boolean> cir) {

        if (shapeA instanceof VoxelShapeSimpleCube && shapeB instanceof VoxelShapeSimpleCube) {
            if (((VoxelShapeSimpleCube) shapeA).isTiny || ((VoxelShapeSimpleCube) shapeB).isTiny) {

                return;
            }

            if (predicate.apply(true, true)) {
                if (intersects((VoxelShapeSimpleCube) shapeA, (VoxelShapeSimpleCube) shapeB)) {
                    cir.setReturnValue(true);
                    return;
                }
                cir.setReturnValue(predicate.apply(true, false) || predicate.apply(false, true));
            } else if (predicate.apply(true, false) &&
                    exceedsShape((VoxelShapeSimpleCube) shapeA, (VoxelShapeSimpleCube) shapeB)) {
                cir.setReturnValue(true);
                return;
            } else if (predicate.apply(false, true) &&
                    exceedsShape((VoxelShapeSimpleCube) shapeB, (VoxelShapeSimpleCube) shapeA)) {
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(false);
        }
        else if (shapeA instanceof VoxelShapeSimpleCube || shapeB instanceof VoxelShapeSimpleCube) {

            VoxelShapeSimpleCube simpleCube = (VoxelShapeSimpleCube) (shapeA instanceof VoxelShapeSimpleCube ? shapeA : shapeB);
            VoxelShape otherShape = simpleCube == shapeA ? shapeB : shapeA;

            if (simpleCube.isTiny || isTiny(otherShape)) {

                return;
            }

            boolean acceptSimpleCubeAlone = predicate.apply(shapeA == simpleCube, shapeB == simpleCube);

            if (acceptSimpleCubeAlone && exceedsCube(simpleCube,
                    otherShape.getMin(X), otherShape.getMin(Y), otherShape.getMin(Z),
                    otherShape.getMax(X), otherShape.getMax(Y), otherShape.getMax(Z))) {
                cir.setReturnValue(true);
                return;
            }
            boolean acceptAnd = predicate.apply(true, true);
            boolean acceptOtherShapeAlone = predicate.apply(shapeA == otherShape, shapeB == otherShape);

            VoxelSet voxelSet = otherShape.voxels;
            DoubleList pointPositionsX = otherShape.getPointPositions(X);
            DoubleList pointPositionsY = otherShape.getPointPositions(Y);
            DoubleList pointPositionsZ = otherShape.getPointPositions(Z);

            int xMax = voxelSet.getMax(X); // xMax <= pointPositionsX.size()
            int yMax = voxelSet.getMax(Y);
            int zMax = voxelSet.getMax(Z);

            double simpleCubeMaxX = simpleCube.getMax(X);
            double simpleCubeMinX = simpleCube.getMin(X);
            double simpleCubeMaxY = simpleCube.getMax(Y);
            double simpleCubeMinY = simpleCube.getMin(Y);
            double simpleCubeMaxZ = simpleCube.getMax(Z);
            double simpleCubeMinZ = simpleCube.getMin(Z);

            for (int x = voxelSet.getMin(X); x < xMax; x++) {

                boolean simpleCubeIntersectsXSlice = (simpleCubeMaxX - 1e-7 > pointPositionsX.getDouble(x) && simpleCubeMinX < pointPositionsX.getDouble(x + 1) - 1e-7);
                if (!acceptOtherShapeAlone && !simpleCubeIntersectsXSlice) {

                    continue;
                }
                boolean xSliceExceedsCube = acceptOtherShapeAlone && !((simpleCubeMaxX >= pointPositionsX.getDouble(x + 1) - 1e-7 && simpleCubeMinX - 1e-7 <= pointPositionsX.getDouble(x)));
                for (int y = voxelSet.getMin(Y); y < yMax; y++) {
                    boolean simpleCubeIntersectsYSlice = (simpleCubeMaxY - 1e-7 > pointPositionsY.getDouble(y) && simpleCubeMinY < pointPositionsY.getDouble(y + 1) - 1e-7);
                    if (!acceptOtherShapeAlone && !simpleCubeIntersectsYSlice) {

                        continue;
                    }
                    boolean ySliceExceedsCube = acceptOtherShapeAlone && !((simpleCubeMaxY >= pointPositionsY.getDouble(y + 1) - 1e-7 && simpleCubeMinY - 1e-7 <= pointPositionsY.getDouble(y)));
                    for (int z = voxelSet.getMin(Z); z < zMax; z++) {
                        boolean simpleCubeIntersectsZSlice = (simpleCubeMaxZ - 1e-7 > pointPositionsZ.getDouble(z) && simpleCubeMinZ < pointPositionsZ.getDouble(z + 1) - 1e-7);
                        if (!acceptOtherShapeAlone && !simpleCubeIntersectsZSlice) {

                            continue;
                        }
                        boolean zSliceExceedsCube = acceptOtherShapeAlone && !((simpleCubeMaxZ >= pointPositionsZ.getDouble(z + 1) - 1e-7 && simpleCubeMinZ - 1e-7 <= pointPositionsZ.getDouble(z)));

                        boolean o = voxelSet.inBoundsAndContains(x, y, z);
                        boolean s = simpleCubeIntersectsXSlice && simpleCubeIntersectsYSlice && simpleCubeIntersectsZSlice;
                        if (acceptAnd && o && s || acceptSimpleCubeAlone && !o && s || acceptOtherShapeAlone && o && (xSliceExceedsCube || ySliceExceedsCube || zSliceExceedsCube)) {
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            }
            cir.setReturnValue(false);
        }
    }

    private static boolean isTiny(VoxelShape shapeA) {

        return shapeA.getMin(X) > shapeA.getMax(X) - 3e-7 ||
                shapeA.getMin(Y) > shapeA.getMax(Y) - 3e-7 ||
                shapeA.getMin(Z) > shapeA.getMax(Z) - 3e-7;
    }

    private static boolean exceedsCube(VoxelShapeSimpleCube a, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return a.getMin(X) < minX - 1e-7 || a.getMax(X) > maxX + 1e-7 ||
                a.getMin(Y) < minY - 1e-7 || a.getMax(Y) > maxY + 1e-7 ||
                a.getMin(Z) < minZ - 1e-7 || a.getMax(Z) > maxZ + 1e-7;
    }

    private static boolean exceedsShape(VoxelShapeSimpleCube a, VoxelShapeSimpleCube b) {
        return a.getMin(X) < b.getMin(X) - 1e-7 || a.getMax(X) > b.getMax(X) + 1e-7 ||
                a.getMin(Y) < b.getMin(Y) - 1e-7 || a.getMax(Y) > b.getMax(Y) + 1e-7 ||
                a.getMin(Z) < b.getMin(Z) - 1e-7 || a.getMax(Z) > b.getMax(Z) + 1e-7;
    }

    private static boolean intersects(VoxelShapeSimpleCube a, VoxelShapeSimpleCube b) {
        return a.getMin(X) < b.getMax(X) - 1e-7 && a.getMax(X) > b.getMin(X) + 1e-7 &&
                a.getMin(Y) < b.getMax(Y) - 1e-7 && a.getMax(Y) > b.getMin(Y) + 1e-7 &&
                a.getMin(Z) < b.getMax(Z) - 1e-7 && a.getMax(Z) > b.getMin(Z) + 1e-7;
    }
}