package me.jellysquid.mods.lithium.common.ai.non_poi_block_search;

import me.jellysquid.mods.lithium.common.util.Distances;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class NonPOISearchDistances {
    public static class MoveToBlockGoalDistances {
        public static int getMinimumSortOrderOfChunk(BlockPos center, final long chunkPos) {
            return getMinimumSortOrderOfChunk(center, ChunkPos.getPackedX(chunkPos), ChunkPos.getPackedZ(chunkPos));
        }

        public static int getMinimumSortOrderOfChunk(BlockPos center, final int chunkX, final int chunkZ) {
            final int dX = Distances.getClosestBlockCoordInSection(center.getX(), chunkX) - center.getX();
            final int dZ = Distances.getClosestBlockCoordInSection(center.getZ(), chunkZ) - center.getZ();

            return getVanillaSortOrderInt(getRing(dX, dZ), dX, dZ);
        }

        public static int getRing(final int dX, final int dZ) {
            return Math.max(Math.abs(dX), Math.abs(dZ));
        }

        public static int getVanillaSortOrderInt(final int ring, final int dX, final int dZ) {
            return (ring << 16 | Math.abs(dX) << 9 | Math.abs(dZ) << 1) - ((dX > 0 ? 1 : 0) << 8 | (dZ > 0 ? 1 : 0));
        }
    }
}
