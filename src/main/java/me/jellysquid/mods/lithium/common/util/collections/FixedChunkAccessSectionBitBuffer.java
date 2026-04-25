package me.jellysquid.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;

public class FixedChunkAccessSectionBitBuffer {
    public final int xMin, yMin, zMin;
    public final int xLength, yLength, zLength, numChunks, numSections;

    public final BitSet chunkSectionBits;
    public final ArrayList<Chunk> chunkAccesses;

    public FixedChunkAccessSectionBitBuffer(int x0, int x1, int y0, int y1, int z0, int z1) {
        this.xMin = Math.min(x0, x1);
        this.yMin = Math.min(y0, y1);
        this.zMin = Math.min(z0, z1);

        this.xLength = Math.max(x0, x1) - this.xMin + 1;
        this.yLength = Math.max(y0, y1) - this.yMin + 1;
        this.zLength = Math.max(z0, z1) - this.zMin + 1;

        this.numChunks = xLength * zLength;
        this.numSections = yLength * xLength * zLength;

        this.chunkSectionBits = new BitSet(numSections);
        this.chunkAccesses = new ArrayList<>(Collections.nCopies(xLength * zLength, null));
    }

    public FixedChunkAccessSectionBitBuffer(BlockPos center, int horizontalRangeInclusive, int verticalRangeInclusive) {
        this(ChunkSectionPos.getSectionCoord(center.getX() - horizontalRangeInclusive),
                ChunkSectionPos.getSectionCoord(center.getX() + horizontalRangeInclusive),
                ChunkSectionPos.getSectionCoord(center.getY() - verticalRangeInclusive),
                ChunkSectionPos.getSectionCoord(center.getY() + verticalRangeInclusive),
                ChunkSectionPos.getSectionCoord(center.getZ() - horizontalRangeInclusive),
                ChunkSectionPos.getSectionCoord(center.getZ() + horizontalRangeInclusive)
        );
    }

    public int getSectionIndex(int x, int y, int z) {
        int dx = x - this.xMin;
        int dy = y - this.yMin;
        int dz = z - this.zMin;

        return (dx * this.zLength + dz) * this.yLength + dy;
    }

    public int getSectionIndex(long sectionPos) {
        ChunkSectionPos pos = ChunkSectionPos.from(sectionPos);
        return this.getSectionIndex(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean getChunkSectionBit(BlockPos blockPos) {
        return this.getChunkSectionBit(
                ChunkSectionPos.getSectionCoord(blockPos.getX()),
                ChunkSectionPos.getSectionCoord(blockPos.getY()),
                ChunkSectionPos.getSectionCoord(blockPos.getZ()));
    }

    public boolean getChunkSectionBit(int chunkX, int chunkY, int chunkZ) {
        return this.chunkSectionBits.get(this.getSectionIndex(chunkX, chunkY, chunkZ));
    }

    public void setChunkSectionStatus(long sectionPos, boolean value) {
        this.chunkSectionBits.set(this.getSectionIndex(sectionPos), value);
    }

    public int getChunkIndex(int x, int z) {
        int dx = x - this.xMin;
        int dz = z - this.zMin;

        return dx * this.zLength + dz;
    }

    public int getChunkIndex(long chunkPos) {
        return this.getChunkIndex(ChunkPos.getPackedX(chunkPos), ChunkPos.getPackedZ(chunkPos));
    }

    public Chunk getChunkAccess(long chunkPos) {
        return this.chunkAccesses.get(this.getChunkIndex(chunkPos));
    }

    public Chunk getChunkAccess(BlockPos blockPos) {
        return this.getChunkAccess(ChunkPos.toLong(blockPos));
    }

    public void setChunkAccess(long chunkPos, Chunk chunkAccess) {
        this.chunkAccesses.set(this.getChunkIndex(chunkPos), chunkAccess);
    }

    public void setChunkAccess(BlockPos blockPos, Chunk chunkAccess) {
        this.setChunkAccess(ChunkPos.toLong(blockPos), chunkAccess);
    }

    public boolean hasNoTrueChunkSections() {
        return this.chunkSectionBits.nextSetBit(0) == -1;
    }

    public LongIterable getChunkPosInRange() {
        return new LongIterable() {
            @Override
            public @NotNull LongIterator iterator() {
                return getChunkPosInRangeIterator();
            }
        };
    }

    public LongIterator getChunkPosInRangeIterator() {
        final int xMin = FixedChunkAccessSectionBitBuffer.this.xMin;
        final int xMax = xMin + xLength - 1;
        final int zMin = FixedChunkAccessSectionBitBuffer.this.zMin;
        final int zMax = zMin + zLength - 1;
        return new LongIterator() {
            int x = xMin;
            int z = zMin;

            @Override
            public long nextLong() {
                long result = ChunkPos.toLong(x, z);
                if (z < zMax) {
                    z++;
                } else {
                    z = zMin;
                    x++;
                }
                return result;
            }

            @Override
            public boolean hasNext() {
                return x <= xMax;
            }
        };
    }

    public IntIterable getSectionYInRange() {
        return new IntIterable() {
            @Override
            public @NotNull IntIterator iterator() {
                return getSectionYInRangeIterator();
            }
        };
    }

    public IntIterator getSectionYInRangeIterator() {
        final int yMin = FixedChunkAccessSectionBitBuffer.this.yMin;
        final int yLimit = yMin + yLength;
        return new IntIterator() {
            int y = yMin;

            @Override
            public int nextInt() {
                return y++;
            }

            @Override
            public boolean hasNext() {
                return y < yLimit;
            }
        };
    }
}
