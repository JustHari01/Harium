package me.jellysquid.mods.lithium.mixin.ai.non_poi_block_search;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import me.jellysquid.mods.lithium.common.ai.non_poi_block_search.CheckAndCacheBlockChecker;
import me.jellysquid.mods.lithium.common.ai.non_poi_block_search.LithiumMoveToBlockGoal;
import me.jellysquid.mods.lithium.common.ai.non_poi_block_search.NonPOISearchDistances.MoveToBlockGoalDistances;
import me.jellysquid.mods.lithium.common.util.Pos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.world.WorldView;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Optimizes MoveToTargetPosGoal's block search by caching ChunkAccesses and
 * using ChunkSection.hasAny() to skip empty sections.
 */
@Mixin(MoveToTargetPosGoal.class)
public abstract class MoveToTargetPosGoalMixin implements LithiumMoveToBlockGoal {
    @Shadow
    @Final
    protected PathAwareEntity mob;
    @Shadow
    @Final
    private int range;
    @Shadow
    @Final
    private int maxYDifference;
    @Shadow
    protected int lowestY;
    @Shadow
    protected BlockPos targetPos;

    @Override
    public boolean lithium$findNearestBlock(Predicate<BlockState> requiredBlock, BiPredicate<Chunk,
            BlockPos.Mutable> lithium$isValidTarget, final boolean shouldChunkLoad) {
        BlockPos center = this.mob.getBlockPos().add(0, -1, 0);

        final WorldView levelReader = this.mob.getWorld();
        CheckAndCacheBlockChecker checker = new CheckAndCacheBlockChecker(center,
                this.range - 1, this.maxYDifference,
                levelReader, requiredBlock, shouldChunkLoad);
        LongArrayList sortedChunksMaybeWithBlock = new LongArrayList(checker.getChunkSize());
        checker.initializeChunks(sortedChunksMaybeWithBlock::add);

        if (checker.shouldStop()) {
            return false;
        }

        final int minY = Pos.BlockCoord.getMinY(levelReader);
        final int maxY = Pos.BlockCoord.getMaxYInclusive(levelReader);

        if (!checker.hasUnloadedPossibleChunks()) {
            return this.lithium$chunkAwareSearch(center, lithium$isValidTarget, checker, sortedChunksMaybeWithBlock, minY, maxY);
        }

        return this.lithium$vanillaOrderSearch(center, lithium$isValidTarget, checker, minY, maxY);
    }

    @Unique
    private boolean lithium$vanillaOrderSearch(BlockPos center,
                                               BiPredicate<Chunk, BlockPos.Mutable> lithium$isValidTarget,
                                               CheckAndCacheBlockChecker checker, final int minY, final int maxY) {
        BlockPos.Mutable currentPos = new BlockPos.Mutable();
        final int centerY = center.getY();

        for (int layer = this.lowestY; layer <= this.maxYDifference; layer = layer > 0 ? -layer : 1 - layer) {
            final int y = centerY + layer;

            if (y < minY || y > maxY) {
                continue;
            }

            for (int ring = 0; ring < this.range; ring++) {
                for (int dX = 0; dX <= ring; dX = dX > 0 ? -dX : 1 - dX) {
                    for (int dZ = dX < ring && dX > -ring ? ring : 0; dZ <= ring; dZ = dZ > 0 ? -dZ : 1 - dZ) {
                        currentPos.set(center.getX() + dX, y, center.getZ() + dZ);
                        if (this.mob.isInWalkTargetRange(currentPos) && checker.checkPosition(currentPos)) {
                            Chunk chunkAccess = checker.getCachedChunkAccess(currentPos);
                            if (lithium$isValidTarget.test(chunkAccess, currentPos)) {
                                this.targetPos = currentPos.toImmutable();
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    @Unique
    private boolean lithium$chunkAwareSearch(BlockPos center,
                                             BiPredicate<Chunk, BlockPos.Mutable> lithium$isValidTarget,
                                             CheckAndCacheBlockChecker checker, LongArrayList sortedChunksMaybeWithBlock,
                                             final int minY, final int maxY) {
        sortedChunksMaybeWithBlock.sort((chunkLong0, chunkLong1) ->
                MoveToBlockGoalDistances.getMinimumSortOrderOfChunk(center, chunkLong0)
                        - MoveToBlockGoalDistances.getMinimumSortOrderOfChunk(center, chunkLong1)
        );

        Predicate<BlockState> requiredBlock = checker.blockStatePredicate;
        final int minSectionY = checker.minSectionY;

        BlockPos.Mutable foundPos = new BlockPos.Mutable();
        BlockPos.Mutable currentPos = new BlockPos.Mutable();

        for (int layer = this.lowestY; layer <= this.maxYDifference; layer = layer > 0 ? -layer : 1 - layer) {
            final int y = center.getY() + layer;

            if (y < minY || y > maxY) {
                continue;
            }

            final int chunkY = ChunkSectionPos.getSectionCoord(y);
            final int ySectionIndex = chunkY - minSectionY;

            int closestFound = Integer.MAX_VALUE;
            int ringMax = this.range - 1;

            for (long chunkPos : sortedChunksMaybeWithBlock) {
                final int chunkX = ChunkPos.getPackedX(chunkPos);
                final int chunkZ = ChunkPos.getPackedZ(chunkPos);

                if (closestFound < MoveToBlockGoalDistances.getMinimumSortOrderOfChunk(center, chunkX, chunkZ)) {
                    break;
                }

                if (!checker.checkCachedSection(chunkX, chunkY, chunkZ)) {
                    continue;
                }

                Chunk chunkAccess = checker.getCachedChunkAccess(chunkPos);
                final int chunkBlockX = ChunkSectionPos.getBlockCoord(chunkX);
                int xMin = Math.max(center.getX() - ringMax, chunkBlockX);
                int xMax = Math.min(center.getX() + ringMax, chunkBlockX + 15);
                final int chunkBlockZ = ChunkSectionPos.getBlockCoord(chunkZ);
                int zMin = Math.max(center.getZ() - ringMax, chunkBlockZ);
                int zMax = Math.min(center.getZ() + ringMax, chunkBlockZ + 15);
                ChunkSection levelChunkSection = chunkAccess.getSectionArray()[ySectionIndex];
                for (int z = zMin; z <= zMax; z++) {
                    for (int x = xMin; x <= xMax; x++) {
                        int dX = x - center.getX();
                        int dZ = z - center.getZ();
                        int ring = MoveToBlockGoalDistances.getRing(dX, dZ);
                        int currentDistance = MoveToBlockGoalDistances.getVanillaSortOrderInt(ring, dX, dZ);
                        if (currentDistance < closestFound
                                && this.mob.isInWalkTargetRange(currentPos.set(x, y, z))
                                && requiredBlock.test(levelChunkSection.getBlockState(x & 15, y & 15, z & 15))
                                && lithium$isValidTarget.test(chunkAccess, currentPos)) {
                            ringMax = ring;
                            xMin = Math.max(center.getX() - ringMax, chunkBlockX);
                            xMax = Math.min(center.getX() + ringMax, chunkBlockX + 15);
                            zMax = Math.min(center.getZ() + ringMax, chunkBlockZ + 15);
                            foundPos.set(x, y, z);
                            closestFound = currentDistance;
                        }
                    }
                }
            }

            if (closestFound < Integer.MAX_VALUE) {
                this.targetPos = foundPos.toImmutable();
                return true;
            }
        }

        return false;
    }
}
