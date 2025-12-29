
package me.jellysquid.mods.lithium.common.world.interests.iterator;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import me.jellysquid.mods.lithium.common.util.Distances;
import me.jellysquid.mods.lithium.common.util.tuples.SortedPointOfInterest;
import me.jellysquid.mods.lithium.common.world.interests.PointOfInterestSetExtended;
import me.jellysquid.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestSet;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class NearbyPointOfInterestStream extends Spliterators.AbstractSpliterator<PointOfInterest> {
    private final RegionBasedStorageSectionExtended<PointOfInterestSet> storage;

    private final Predicate<RegistryEntry<PointOfInterestType>> typeSelector;
    private final PointOfInterestStorage.OccupationStatus occupationStatus;

    private final LongArrayList chunksSortedByMinDistance;
    private final ArrayList<SortedPointOfInterest> points;
    private final Predicate<PointOfInterest> afterSortingPredicate;
    private final Consumer<PointOfInterest> collector;
    private final BlockPos origin;

    private int chunkIndex;
    private double currChunkMinDistanceSq;
    private int pointIndex;
    private final Comparator<? super SortedPointOfInterest> pointComparator;

    public NearbyPointOfInterestStream(Predicate<RegistryEntry<PointOfInterestType>> typeSelector,
                                       PointOfInterestStorage.OccupationStatus status,
                                       boolean useSquareDistanceLimit,
                                       boolean preferNegativeY,
                                       @Nullable Predicate<PointOfInterest> afterSortingPredicate,
                                       BlockPos origin, int radius,
                                       RegionBasedStorageSectionExtended<PointOfInterestSet> storage) {
        super(Long.MAX_VALUE, Spliterator.ORDERED);

        this.storage = storage;

        this.chunkIndex = 0;
        this.pointIndex = 0;

        this.points = new ArrayList<>();
        this.occupationStatus = status;
        this.typeSelector = typeSelector;

        this.origin = origin;
        if (useSquareDistanceLimit) {
            this.collector = (point) -> {
                if (Distances.isWithinSquareRadius(this.origin, radius, point.getPos())) {
                    this.points.add(new SortedPointOfInterest(point, this.origin));
                }
            };
        } else {
            double radiusSq = radius * radius;
            this.collector = (point) -> {
                if (Distances.isWithinCircleRadius(this.origin, radiusSq, point.getPos())) {
                    this.points.add(new SortedPointOfInterest(point, this.origin));
                }
            };
        }

        double distanceLimitL2Sq = useSquareDistanceLimit ? radius * radius * 2 : radius * radius;
        this.chunksSortedByMinDistance = initChunkPositions(origin, radius, distanceLimitL2Sq);
        this.afterSortingPredicate = afterSortingPredicate;
        this.pointComparator = preferNegativeY ? (o1, o2) -> {

            int cmp = Double.compare(o1.distanceSq(), o2.distanceSq());

            if (cmp != 0) {
                return cmp;
            }

            int negativeY = Integer.compare(o1.getY(), o2.getY());
            if (negativeY != 0) {
                return negativeY;
            }

            int cmp3 = Integer.compare(ChunkSectionPos.getSectionCoord(o1.getX()), ChunkSectionPos.getSectionCoord(o2.getX()));
            if (cmp3 != 0) {
                return cmp3;
            }
            return Integer.compare(ChunkSectionPos.getSectionCoord(o1.getZ()), ChunkSectionPos.getSectionCoord(o2.getZ()));

        } : (o1, o2) -> {

            int cmp = Double.compare(o1.distanceSq(), o2.distanceSq());

            if (cmp != 0) {
                return cmp;
            }

            int cmp2 = Integer.compare(ChunkSectionPos.getSectionCoord(o1.getX()), ChunkSectionPos.getSectionCoord(o2.getX()));
            if (cmp2 != 0) {
                return cmp2;
            }
            int cmp3 = Integer.compare(ChunkSectionPos.getSectionCoord(o1.getZ()), ChunkSectionPos.getSectionCoord(o2.getZ()));
            if (cmp3 != 0) {
                return cmp3;
            }
            return Integer.compare(ChunkSectionPos.getSectionCoord(o1.getY()), ChunkSectionPos.getSectionCoord(o2.getY()));

        };
    }

    private static LongArrayList initChunkPositions(BlockPos origin, int radius, double distanceLimitL2Sq) {
        int minChunkX = (origin.getX() - radius - 1) >> 4;
        int minChunkZ = (origin.getZ() - radius - 1) >> 4;

        int maxChunkX = (origin.getX() + radius + 1) >> 4;
        int maxChunkZ = (origin.getZ() + radius + 1) >> 4;

        LongArrayList chunkPositions = new LongArrayList();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (distanceLimitL2Sq >= Distances.getMinChunkToBlockDistanceL2Sq(origin, chunkX, chunkZ)) {
                    chunkPositions.add(ChunkPos.toLong(chunkX, chunkZ));
                }
            }
        }

        chunkPositions.sort(
                (long c1, long c2) -> Double.compare(
                        Distances.getMinChunkToBlockDistanceL2Sq(origin, ChunkPos.getPackedX(c1), ChunkPos.getPackedZ(c1)),
                        Distances.getMinChunkToBlockDistanceL2Sq(origin, ChunkPos.getPackedX(c2), ChunkPos.getPackedZ(c2))));

        return chunkPositions;
    }

    @Override
    public boolean tryAdvance(Consumer<? super PointOfInterest> action) {

        if (this.pointIndex < this.points.size()) {
            if (this.tryAdvancePoint(action)) {
                return true;
            }
        }

        while (this.chunkIndex < this.chunksSortedByMinDistance.size()) {
            long chunkPos = this.chunksSortedByMinDistance.getLong(this.chunkIndex);
            int chunkPosX = ChunkPos.getPackedX(chunkPos);
            int chunkPosZ = ChunkPos.getPackedZ(chunkPos);

            this.currChunkMinDistanceSq = Distances.getMinChunkToBlockDistanceL2Sq(this.origin, chunkPosX, chunkPosZ);
            this.chunkIndex++;
            if (this.chunkIndex == this.chunksSortedByMinDistance.size()) {
                this.currChunkMinDistanceSq = Double.POSITIVE_INFINITY;
            }

            int previousSize = this.points.size();

            for (PointOfInterestSet set : this.storage.getInChunkColumn(chunkPosX, chunkPosZ)) {
                ((PointOfInterestSetExtended) set).collectMatchingPoints(this.typeSelector, this.occupationStatus, this.collector);
            }

            if (this.points.size() == previousSize) {
                continue;
            }

            this.points.subList(this.pointIndex, this.points.size()).sort(this.pointComparator);

            if (this.tryAdvancePoint(action)) {
                return true; //Returns true when progress was made by consuming an element
            }
        }

        return this.tryAdvancePoint(action);

    }

    private boolean tryAdvancePoint(Consumer<? super PointOfInterest> action) {
        while (this.pointIndex < this.points.size()) {
            SortedPointOfInterest next = this.points.get(this.pointIndex);

            if (next.distanceSq() >= this.currChunkMinDistanceSq) {
                return false;
            }
            this.pointIndex++;

            if (this.afterSortingPredicate == null || this.afterSortingPredicate.test(next.poi())) {
                action.accept(next.poi());
                return true; //Progress was made
            }

        }
        return false; //No more points. Scan more chunks
    }

}