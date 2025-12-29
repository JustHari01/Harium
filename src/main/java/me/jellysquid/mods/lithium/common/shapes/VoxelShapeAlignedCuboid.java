package me.jellysquid.mods.lithium.common.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.FractionalDoubleList;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;

public class VoxelShapeAlignedCuboid extends VoxelShapeSimpleCube {

    static final double LARGE_EPSILON = 10 * EPSILON;

    protected final int xSegments;
    protected final int ySegments;
    protected final int zSegments;

    public VoxelShapeAlignedCuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int xRes, int yRes, int zRes) {
        super(new CuboidVoxelSet(1 << xRes, 1 << yRes, 1 << zRes, minX, minY, minZ, maxX, maxY, maxZ), minX, minY, minZ, maxX, maxY, maxZ);

        this.xSegments = 1 << xRes;
        this.ySegments = 1 << yRes;
        this.zSegments = 1 << zRes;
    }

    public VoxelShapeAlignedCuboid(VoxelSet voxels, int xSegments, int ySegments, int zSegments, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(voxels, minX, minY, minZ, maxX, maxY, maxZ);

        this.xSegments = xSegments;
        this.ySegments = ySegments;
        this.zSegments = zSegments;
    }

    @Override
    public VoxelShape offset(double x, double y, double z) {
        return new VoxelShapeAlignedCuboidOffset(this, this.voxels, x, y, z);
    }

    @Override
    public double calculateMaxDistance(AxisCycleDirection cycleDirection, Box box, double maxDist) {
        if (Math.abs(maxDist) < EPSILON) {
            return 0.0D;
        }

        double penetration = this.calculatePenetration(cycleDirection, box, maxDist);

        if ((penetration != maxDist) && this.intersects(cycleDirection, box)) {
            return penetration;
        }

        return maxDist;
    }

    private double calculatePenetration(AxisCycleDirection dir, Box box, double maxDist) {
        switch (dir) {
            case NONE:
                return VoxelShapeAlignedCuboid.calculatePenetration(this.minX, this.maxX, this.xSegments, box.minX, box.maxX, maxDist);
            case FORWARD:
                return VoxelShapeAlignedCuboid.calculatePenetration(this.minZ, this.maxZ, this.zSegments, box.minZ, box.maxZ, maxDist);
            case BACKWARD:
                return VoxelShapeAlignedCuboid.calculatePenetration(this.minY, this.maxY, this.ySegments, box.minY, box.maxY, maxDist);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static double calculatePenetration(double aMin, double aMax, final int segmentsPerUnit, double bMin, double bMax, double maxDist) {
        double gap;

        if (maxDist > 0.0D) {
            gap = aMin - bMax;

            if (gap >= -EPSILON) {

                return Math.min(gap, maxDist);
            } else {

                if (segmentsPerUnit == 1) {

                    return maxDist;
                }

                double wallPos = MathHelper.ceil((bMax - EPSILON) * segmentsPerUnit) / (double) segmentsPerUnit;

                if (wallPos < aMax - LARGE_EPSILON) {
                    return Math.min(maxDist, wallPos - bMax);
                }
                return maxDist;
            }
        } else {

            gap = aMax - bMin;

            if (gap <= EPSILON) {

                return Math.max(gap, maxDist);
            } else {

                if (segmentsPerUnit == 1) {

                    return maxDist;
                }

                double wallPos = MathHelper.floor((bMin + EPSILON) * segmentsPerUnit) / (double) segmentsPerUnit;

                if (wallPos > aMin + LARGE_EPSILON) {
                    return Math.max(maxDist, wallPos - bMin);
                }
                return maxDist;
            }
        }
    }

    @Override
    public DoubleList getPointPositions(Direction.Axis axis) {
        return new FractionalDoubleList(axis.choose(this.xSegments, this.ySegments, this.zSegments));
    }

    @Override
    protected double getPointPosition(Direction.Axis axis, int index) {
        return (double) index / (double) axis.choose(this.xSegments, this.ySegments, this.zSegments);
    }

    @Override
    protected int getCoordIndex(Direction.Axis axis, double coord) {
        int i = axis.choose(this.xSegments, this.ySegments, this.zSegments);
        return MathHelper.clamp(MathHelper.floor(coord * (double) i), -1, i);
    }
}
