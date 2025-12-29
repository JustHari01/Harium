package me.jellysquid.mods.lithium.common.world.interests;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Optional;
import java.util.function.Predicate;

public interface PointOfInterestStorageExtended {
    
    Optional<PointOfInterest> findNearestForPortalLogic(BlockPos pos, int radius, RegistryEntry<PointOfInterestType> type, PointOfInterestStorage.OccupationStatus status,
                                                        Predicate<PointOfInterest> afterSortPredicate, WorldBorder worldBorder);
}
