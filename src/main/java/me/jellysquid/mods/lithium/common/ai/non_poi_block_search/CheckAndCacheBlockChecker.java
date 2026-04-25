package me.jellysquid.mods.lithium.common.ai.non_poi_block_search;

import me.jellysquid.mods.lithium.common.util.collections.FixedChunkAccessSectionBitBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Pre-checks ChunkSections using hasAny to determine if they might contain matching blocks.
 * Caches ChunkAccess objects to avoid repeated chunk lookups.
 */
public class CheckAndCacheBlockChecker {
    private final FixedChunkAccessSectionBitBuffer chunkSections2MaybeContainsMatchingBlock;
    private final WorldView levelReader;
    public final boolean shouldChunkLoad;
    public final Predicate<net.minecraft.block.BlockState> blockStatePredicate;
    private int unloadedPossibleChunkSections = 0;
    public final int minSectionY;

    public CheckAndCacheBlockChecker(BlockPos origin, int horizontalRangeInclusive, int verticalRangeInclusive,
                                     WorldView levelReader, Predicate<net.minecraft.block.BlockState> blockStatePredicate,
                                     boolean shouldChunkLoad) {
        this.chunkSections2MaybeContainsMatchingBlock = new FixedChunkAccessSectionBitBuffer(origin, horizontalRangeInclusive, verticalRangeInclusive);
        this.levelReader = levelReader;
        this.shouldChunkLoad = shouldChunkLoad;
        this.blockStatePredicate = blockStatePredicate;
        this.minSectionY = ((HeightLimitView) levelReader).getBottomSectionCoord();
    }

    public void initializeChunks() {
        this.initializeChunks(null);
    }

    public void initializeChunks(Consumer<Long> chunkCollector) {
        final boolean nullChunkCollector = chunkCollector == null;
        for (long chunkPos : this.chunkSections2MaybeContainsMatchingBlock.getChunkPosInRange()) {
            int x = ChunkPos.getPackedX(chunkPos);
            int z = ChunkPos.getPackedZ(chunkPos);
            boolean chunkMaybeHas = false;

            Chunk chunkAccess = levelReader.getChunk(x, z, ChunkStatus.FULL, false);
            if (chunkAccess != null) {
                this.chunkSections2MaybeContainsMatchingBlock.setChunkAccess(chunkPos, chunkAccess);
                for (int y : this.chunkSections2MaybeContainsMatchingBlock.getSectionYInRange()) {
                    chunkMaybeHas = this.checkChunkSection(chunkAccess, x, y, z) || chunkMaybeHas;
                }
            } else if (this.shouldChunkLoad) {
                for (int y : this.chunkSections2MaybeContainsMatchingBlock.getSectionYInRange()) {
                    this.chunkSections2MaybeContainsMatchingBlock.setChunkSectionStatus(ChunkSectionPos.asLong(x, y, z),
                            !levelReader.isOutOfHeightLimit(ChunkSectionPos.getBlockCoord(y)));
                    ++this.unloadedPossibleChunkSections;
                }
                chunkMaybeHas = true;
            }

            if (!nullChunkCollector && chunkMaybeHas) {
                chunkCollector.accept(chunkPos);
            }
        }
    }

    public int getChunkSize() {
        return this.chunkSections2MaybeContainsMatchingBlock.numChunks;
    }

    public boolean hasUnloadedPossibleChunks() {
        return this.unloadedPossibleChunkSections > 0;
    }

    private boolean checkChunkSection(Chunk chunkAccess, int chunkX, int chunkY, int chunkZ) {
        final int chunkSectionYIndex = chunkY - this.minSectionY;
        ChunkSection[] chunkSections = chunkAccess.getSectionArray();
        if (chunkSectionYIndex >= 0
                && chunkSectionYIndex < chunkSections.length
                && chunkSections[chunkSectionYIndex].hasAny(blockStatePredicate)) {
            this.chunkSections2MaybeContainsMatchingBlock.setChunkSectionStatus(
                    ChunkSectionPos.asLong(chunkX, chunkY, chunkZ), true);
            return true;
        }
        return false;
    }

    public boolean checkCachedSection(int chunkX, int chunkY, int chunkZ) {
        return this.chunkSections2MaybeContainsMatchingBlock.getChunkSectionBit(chunkX, chunkY, chunkZ);
    }

    public Chunk getCachedChunkAccess(long chunkPos) {
        return this.chunkSections2MaybeContainsMatchingBlock.getChunkAccess(chunkPos);
    }

    public Chunk getCachedChunkAccess(BlockPos blockPos) {
        return this.chunkSections2MaybeContainsMatchingBlock.getChunkAccess(blockPos);
    }

    public boolean shouldStop() {
        return this.chunkSections2MaybeContainsMatchingBlock.hasNoTrueChunkSections();
    }

    public boolean checkPosition(BlockPos blockPos) {
        if (!this.chunkSections2MaybeContainsMatchingBlock.getChunkSectionBit(blockPos)) return false;
        Chunk chunkAccess = this.chunkSections2MaybeContainsMatchingBlock.getChunkAccess(blockPos);
        if (chunkAccess == null) {
            if (!this.shouldChunkLoad) {
                return false;
            }

            final int chunkX = ChunkSectionPos.getSectionCoord(blockPos.getX());
            final int chunkY = ChunkSectionPos.getSectionCoord(blockPos.getY());
            final int chunkZ = ChunkSectionPos.getSectionCoord(blockPos.getZ());
            chunkAccess = levelReader.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            assert chunkAccess != null;
            this.chunkSections2MaybeContainsMatchingBlock.setChunkAccess(blockPos, chunkAccess);
            if (!checkChunkSection(chunkAccess, chunkX, chunkY, chunkZ)) {
                --this.unloadedPossibleChunkSections;
                return false;
            }
        }

        return blockStatePredicate.test(chunkAccess.getBlockState(blockPos));
    }
}
