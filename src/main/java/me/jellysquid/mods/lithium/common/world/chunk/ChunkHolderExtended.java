package me.jellysquid.mods.lithium.common.world.chunk;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import java.util.concurrent.CompletableFuture;

public interface ChunkHolderExtended {
    
    CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getFutureByStatus(int index);

    void setFutureForStatus(int index, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> future);

    boolean updateLastAccessTime(long time);

    WorldChunk getCurrentlyLoading();
}
