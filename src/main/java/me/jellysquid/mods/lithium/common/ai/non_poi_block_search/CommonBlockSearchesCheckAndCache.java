package me.jellysquid.mods.lithium.common.ai.non_poi_block_search;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.block.BlockState;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Uses CheckAndCacheBlockChecker to improve common block searches.
 * Optimizes BlockPos iteration by pre-checking chunk sections.
 */
public class CommonBlockSearchesCheckAndCache {

    public static Optional<BlockPos> blockPosFindClosestMatch(WorldView levelReader, LivingEntity livingEntity,
                                                              int horizontalRange, int verticalRange,
                                                              Predicate<BlockState> blockStatePredicate,
                                                              boolean shouldChunkLoad) {
        BlockPos mobPos = livingEntity.getBlockPos();
        CheckAndCacheBlockChecker checker = new CheckAndCacheBlockChecker(
                mobPos, horizontalRange, verticalRange, levelReader, blockStatePredicate, shouldChunkLoad);
        checker.initializeChunks();
        if (checker.shouldStop()) {
            return Optional.empty();
        }

        // Iterate in Manhattan-distance spiral from center
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int centerX = mobPos.getX();
        int centerY = mobPos.getY();
        int centerZ = mobPos.getZ();

        for (int ring = 0; ring <= Math.max(horizontalRange, verticalRange); ring++) {
            // Vertical range (dy)
            for (int dy = -Math.min(ring, verticalRange); dy <= Math.min(ring, verticalRange); dy++) {
                int horizontalRing = ring - Math.abs(dy);
                if (horizontalRing < 0) continue;

                // Horizontal ring edges
                for (int dx = -horizontalRing; dx <= horizontalRing; dx++) {
                    for (int dz = -horizontalRing; dz <= horizontalRing; dz++) {
                        // Only check positions on the ring edge
                        if (Math.abs(dx) != horizontalRing && Math.abs(dz) != horizontalRing) continue;

                        mutable.set(centerX + dx, centerY + dy, centerZ + dz);
                        if (checker.checkPosition(mutable)) {
                            return Optional.of(mutable.toImmutable());
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }
}
