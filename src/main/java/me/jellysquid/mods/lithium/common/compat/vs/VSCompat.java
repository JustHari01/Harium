package me.jellysquid.mods.lithium.common.compat.vs;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Soft compatibility layer for Valkyrien Skies 2.
 * Uses reflection to avoid hard dependency on VS.
 */
public class VSCompat {
    private static boolean initialized = false;
    private static boolean vsLoaded = false;
    
    private static Method toWorldCoordinatesMethod = null;
    private static Method getShipObjectWorldMethod = null;
    private static Method getLoadedShipsMethod = null;
    private static Method getIntersectingMethod = null;
    private static Method getDimensionIdMethod = null;
    private static Method toJOMLMethod = null;
    
    /**
     * Check if Valkyrien Skies is loaded
     */
    public static boolean isVSLoaded() {
        if (!initialized) {
            init();
        }
        return vsLoaded;
    }
    
    private static void init() {
        initialized = true;
        try {
            // Check if VS is loaded by trying to load its main class
            Class<?> vsGameUtilsClass = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
            
            // Cache the toWorldCoordinates method
            toWorldCoordinatesMethod = vsGameUtilsClass.getMethod("toWorldCoordinates", World.class, Vec3d.class);
            getShipObjectWorldMethod = vsGameUtilsClass.getMethod("getShipObjectWorld", Object.class);
            getDimensionIdMethod = vsGameUtilsClass.getMethod("getDimensionId", Object.class);
            
            // Cache AABB to JOML conversion
            Class<?> vectorConversionsClass = Class.forName("org.valkyrienskies.mod.common.util.VectorConversionsMCKt");
            Class<?> aabbClass = Class.forName("net.minecraft.util.math.Box");
            toJOMLMethod = vectorConversionsClass.getMethod("toJOML", aabbClass);
            
            vsLoaded = true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            vsLoaded = false;
        }
    }
    
    /**
     * Transform a BlockPos from ship-space to world-space if it's on a ship.
     * Returns the original position if VS is not loaded or the pos is not on a ship.
     */
    public static BlockPos toWorldPos(@Nullable World world, BlockPos pos) {
        if (!isVSLoaded() || world == null) {
            return pos;
        }
        
        try {
            Vec3d vec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            Vec3d worldVec = (Vec3d) toWorldCoordinatesMethod.invoke(null, world, vec);
            
            // If the position changed, it was on a ship
            if (worldVec != vec) {
                return new BlockPos((int) Math.floor(worldVec.x), (int) Math.floor(worldVec.y), (int) Math.floor(worldVec.z));
            }
            return pos;
        } catch (Exception e) {
            return pos;
        }
    }
    
    /**
     * Get the squared distance from origin to a POI position, considering ship transforms.
     */
    public static double getDistanceSqToWorldPos(@Nullable World world, BlockPos origin, BlockPos poiPos) {
        if (!isVSLoaded() || world == null) {
            return origin.getSquaredDistance(poiPos);
        }
        
        try {
            Vec3d poiVec = new Vec3d(poiPos.getX() + 0.5, poiPos.getY() + 0.5, poiPos.getZ() + 0.5);
            Vec3d worldVec = (Vec3d) toWorldCoordinatesMethod.invoke(null, world, poiVec);
            
            double dx = worldVec.x - origin.getX();
            double dy = worldVec.y - origin.getY();
            double dz = worldVec.z - origin.getZ();
            
            return dx * dx + dy * dy + dz * dz;
        } catch (Exception e) {
            return origin.getSquaredDistance(poiPos);
        }
    }
    
    /**
     * Check if a POI position is within sphere radius from origin, considering ship transforms.
     */
    public static boolean isWithinSphereRadius(@Nullable World world, BlockPos origin, double radiusSq, BlockPos poiPos) {
        return getDistanceSqToWorldPos(world, origin, poiPos) <= radiusSq;
    }
}
