package me.jellysquid.mods.lithium.common.shapes;

import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public interface VoxelShapeCaster {
    
    boolean intersects(Box box, double blockX, double blockY, double blockZ);
}
