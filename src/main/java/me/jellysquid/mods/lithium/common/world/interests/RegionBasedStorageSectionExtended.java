package me.jellysquid.mods.lithium.common.world.interests;

import java.util.stream.Stream;

public interface RegionBasedStorageSectionExtended<R> {
    
    Stream<R> getWithinChunkColumn(int chunkX, int chunkZ);

    Iterable<R> getInChunkColumn(int chunkX, int chunkZ);
}
