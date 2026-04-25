package me.jellysquid.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.bytes.ByteBytePair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.entity.ai.brain.task.LongJumpTask;
import net.minecraft.util.math.BlockPos;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized list for LongJumpTask weighted random choice.
 * Keeps an up-to-date total weight, allows random choice with fewer addition operations.
 * Removal is done quickly by swapping the removed element to the end.
 */
public class LongJumpChoiceList extends AbstractList<LongJumpTask.Target> {

    private static final ConcurrentHashMap<ByteBytePair, LongJumpChoiceList> CHOICE_LISTS = new ConcurrentHashMap<>();
    private static final LongJumpChoiceList FROG_JUMP = new LongJumpChoiceList((byte) 4, (byte) 2);
    private static final LongJumpChoiceList GOAT_JUMP = new LongJumpChoiceList((byte) 5, (byte) 5);

    private final BlockPos origin;
    private final IntArrayList[] packedOffsetsByDistanceSq;
    private final int[] weightByDistanceSq;
    private int totalWeight;

    public LongJumpChoiceList(byte horizontalRange, byte verticalRange) {
        if (horizontalRange < 0 || verticalRange < 0) {
            throw new IllegalArgumentException("Ranges must be within 0..127!");
        }

        this.origin = BlockPos.ORIGIN;
        int maxSqDistance = horizontalRange * horizontalRange * 2 + verticalRange * verticalRange;
        this.packedOffsetsByDistanceSq = new IntArrayList[maxSqDistance];
        this.weightByDistanceSq = new int[maxSqDistance];

        for (int x = -horizontalRange; x <= horizontalRange; x++) {
            for (int y = -verticalRange; y <= verticalRange; y++) {
                for (int z = -horizontalRange; z <= horizontalRange; z++) {
                    int squaredDistance = x * x + y * y + z * z;
                    int index = squaredDistance - 1;
                    if (index >= 0) {
                        int packedOffset = this.packOffset(x, y, z);
                        IntArrayList offsets = this.packedOffsetsByDistanceSq[index];
                        if (offsets == null) {
                            this.packedOffsetsByDistanceSq[index] = offsets = new IntArrayList();
                        }
                        offsets.add(packedOffset);
                        this.weightByDistanceSq[index] += squaredDistance;
                        this.totalWeight += squaredDistance;
                    }
                }
            }
        }
    }

    private LongJumpChoiceList(BlockPos origin, IntArrayList[] packedOffsetsByDistanceSq, int[] weightByDistanceSq, int totalWeight) {
        this.origin = origin;
        this.packedOffsetsByDistanceSq = packedOffsetsByDistanceSq;
        this.weightByDistanceSq = weightByDistanceSq;
        this.totalWeight = totalWeight;
    }

    private int packOffset(int x, int y, int z) {
        return (x + 128) | ((y + 128) << 8) | ((z + 128) << 16);
    }

    private int unpackX(int packedOffset) {
        return (packedOffset & 0xFF) - 128;
    }

    private int unpackY(int packedOffset) {
        return ((packedOffset >>> 8) & 0xFF) - 128;
    }

    private int unpackZ(int packedOffset) {
        return ((packedOffset >>> 16) & 0xFF) - 128;
    }

    public static LongJumpChoiceList forCenter(BlockPos centerPos, byte horizontalRange, byte verticalRange) {
        if (horizontalRange < 0 || verticalRange < 0) {
            throw new IllegalArgumentException("Ranges must be within 0..127!");
        }

        LongJumpChoiceList template;
        short range = (short) ((horizontalRange << 8) | verticalRange);
        if (range == ((4 << 8) | 2)) {
            template = FROG_JUMP;
        } else if (range == ((5 << 8) | 5)) {
            template = GOAT_JUMP;
        } else {
            template = CHOICE_LISTS.computeIfAbsent(
                    ByteBytePair.of(horizontalRange, verticalRange),
                    key -> new LongJumpChoiceList(key.leftByte(), key.rightByte())
            );
        }

        return template.offsetCopy(centerPos);
    }

    private LongJumpChoiceList offsetCopy(BlockPos offset) {
        IntArrayList[] newOffsets = new IntArrayList[this.packedOffsetsByDistanceSq.length];
        for (int i = 0; i < newOffsets.length; i++) {
            IntArrayList packedOffsets = this.packedOffsetsByDistanceSq[i];
            if (packedOffsets != null) {
                newOffsets[i] = packedOffsets.clone();
            }
        }
        return new LongJumpChoiceList(
                this.origin.add(offset),
                newOffsets,
                Arrays.copyOf(this.weightByDistanceSq, this.weightByDistanceSq.length),
                this.totalWeight);
    }

    public LongJumpTask.Target removeRandomWeightedByDistanceSq(net.minecraft.util.math.random.Random random) {
        if (this.totalWeight == 0) return null;
        int targetWeight = random.nextInt(this.totalWeight);
        for (int index = 0; targetWeight >= 0 && index < this.weightByDistanceSq.length; index++) {
            targetWeight -= this.weightByDistanceSq[index];
            if (targetWeight < 0) {
                int distanceSq = index + 1;
                IntArrayList elementsOfDistance = this.packedOffsetsByDistanceSq[index];
                int elementIndex = random.nextInt(elementsOfDistance.size());

                elementsOfDistance.set(elementIndex, elementsOfDistance.set(elementsOfDistance.size() - 1, elementsOfDistance.getInt(elementIndex)));
                int packedOffset = elementsOfDistance.removeInt(elementsOfDistance.size() - 1);
                this.weightByDistanceSq[index] -= distanceSq;
                this.totalWeight -= distanceSq;

                return new LongJumpTask.Target(
                        this.origin.add(this.unpackX(packedOffset), this.unpackY(packedOffset), this.unpackZ(packedOffset)),
                        distanceSq);
            }
        }
        return null;
    }

    @Override
    public LongJumpTask.Target get(int index) {
        int elementIndex = index;
        for (int distanceSq = 0; distanceSq < this.packedOffsetsByDistanceSq.length; distanceSq++) {
            IntArrayList packedOffsets = this.packedOffsetsByDistanceSq[distanceSq];
            if (packedOffsets != null) {
                if (elementIndex < packedOffsets.size()) {
                    int packedOffset = packedOffsets.getInt(elementIndex);
                    return new LongJumpTask.Target(
                            this.origin.add(this.unpackX(packedOffset), this.unpackY(packedOffset), this.unpackZ(packedOffset)),
                            distanceSq);
                }
                elementIndex -= packedOffsets.size();
            }
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public boolean isEmpty() {
        return this.totalWeight == 0;
    }

    @Override
    public int size() {
        int size = 0;
        for (IntArrayList packedOffsets : this.packedOffsetsByDistanceSq) {
            if (packedOffsets != null) {
                size += packedOffsets.size();
            }
        }
        return size;
    }

    @Override
    public LongJumpTask.Target remove(int index) {
        int elementIndex = index;
        for (int distanceSq = 0; distanceSq < this.packedOffsetsByDistanceSq.length; distanceSq++) {
            IntArrayList packedOffsets = this.packedOffsetsByDistanceSq[distanceSq];
            if (packedOffsets != null) {
                if (elementIndex < packedOffsets.size()) {
                    int packedOffset = packedOffsets.getInt(elementIndex);
                    packedOffsets.set(elementIndex, packedOffsets.set(packedOffsets.size() - 1, packedOffsets.getInt(elementIndex)));
                    packedOffsets.removeInt(packedOffsets.size() - 1);
                    this.weightByDistanceSq[distanceSq] -= distanceSq;
                    this.totalWeight -= distanceSq;
                    return new LongJumpTask.Target(
                            this.origin.add(this.unpackX(packedOffset), this.unpackY(packedOffset), this.unpackZ(packedOffset)),
                            distanceSq);
                }
                elementIndex -= packedOffsets.size();
            }
        }
        throw new IndexOutOfBoundsException();
    }
}
