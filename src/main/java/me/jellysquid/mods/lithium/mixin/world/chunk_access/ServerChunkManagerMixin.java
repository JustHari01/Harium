package me.jellysquid.mods.lithium.mixin.world.chunk_access;

import com.mojang.datafixers.util.Either;
import me.jellysquid.mods.lithium.common.world.chunk.ChunkHolderExtended;
import net.minecraft.server.world.*;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("OverwriteModifiers")
@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin {
    @Shadow
    @Final
    private ServerChunkManager.MainThreadExecutor mainThreadExecutor;

    @Shadow
    @Final
    private ChunkTicketManager ticketManager;

    @Shadow
    @Final
    public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

    @Shadow
    protected abstract ChunkHolder getChunkHolder(long pos);

    @Shadow
    @Final
    Thread serverThread;

    @Shadow
    protected abstract boolean isMissingForLevel(ChunkHolder holder, int maxLevel);

    @Shadow
    abstract boolean tick();
    private long time;

    @Inject(method = "tick()Z", at = @At("HEAD"))
    private void preTick(CallbackInfoReturnable<Boolean> cir) {
        this.time++;
    }

    @Overwrite
    public Chunk getChunk(int x, int z, ChunkStatus status, boolean create) {
        if (Thread.currentThread() != this.serverThread) {
            return this.getChunkOffThread(x, z, status, create);
        }

        long[] cacheKeys = this.cacheKeys;

        long key = createCacheKey(x, z, status);

        for (int i = 0; i < 4; ++i) {

            if (key == cacheKeys[i]) {
                Chunk chunk = this.cacheChunks[i];

                if (chunk != null || !create) {
                    return chunk;
                }
            }
        }

        Chunk chunk = this.getChunkBlocking(x, z, status, create);

        if (chunk != null) {
            this.addToCache(key, chunk);
        } else if (create) {
            throw new IllegalStateException("Chunk not there when requested");
        }

        return chunk;
    }

    private Chunk getChunkOffThread(int x, int z, ChunkStatus status, boolean create) {
        return CompletableFuture.supplyAsync(() -> this.getChunk(x, z, status, create), this.mainThreadExecutor).join();
    }

    private Chunk getChunkBlocking(int x, int z, ChunkStatus status, boolean create) {
        final long key = ChunkPos.toLong(x, z);
        final int level = ChunkLevels.getLevelFromStatus(status);

        ChunkHolder holder = this.getChunkHolder(key);

        if (holder != null && ((ChunkHolderExtended)holder).getCurrentlyLoading() != null)
            return ((ChunkHolderExtended)holder).getCurrentlyLoading();

        if (this.isMissingForLevel(holder, level)) {
            if (create) {

                this.createChunkLoadTicket(x, z, level);

                this.tick();

                holder = this.getChunkHolder(key);

                if (this.isMissingForLevel(holder, level)) {
                    throw Util.throwOrPause(new IllegalStateException("No chunk holder after ticket has been added"));
                }
            } else {

                return null;
            }
        } else if (create && ((ChunkHolderExtended) holder).updateLastAccessTime(this.time)) {

            this.createChunkLoadTicket(x, z, level);
        }

        CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> loadFuture = null;
        CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> statusFuture = ((ChunkHolderExtended) holder).getFutureByStatus(status.getIndex());

        if (statusFuture != null) {
            Either<Chunk, ChunkHolder.Unloaded> immediate = statusFuture.getNow(null);

            if (immediate != null) {
                Optional<Chunk> chunk = immediate.left();

                if (chunk.isPresent()) {

                    return chunk.get();
                }
            } else {

                loadFuture = statusFuture;
            }
        }

        if (loadFuture == null) {
            if (ChunkLevels.getStatus(holder.getLevel()).isAtLeast(status)) {

                CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> mergedFuture = this.threadedAnvilChunkStorage.getChunk(holder, status);

                holder.combineSavingFuture(mergedFuture, "schedule chunk status");
                ((ChunkHolderExtended) holder).setFutureForStatus(status.getIndex(), mergedFuture);

                loadFuture = mergedFuture;
            } else {
                if (statusFuture == null) {
                    return null;
                }

                loadFuture = statusFuture;
            }
        }

        if (!loadFuture.isDone()) {

            this.mainThreadExecutor.runTasks(loadFuture::isDone);
        }

        return loadFuture.join().left().orElse(null);
    }

    private void createChunkLoadTicket(int x, int z, int level) {
        ChunkPos chunkPos = new ChunkPos(x, z);

        this.ticketManager.addTicketWithLevel(ChunkTicketType.UNKNOWN, chunkPos, level, chunkPos);
    }

    private final long[] cacheKeys = new long[4];

    private final Chunk[] cacheChunks = new Chunk[4];

    private static long createCacheKey(int chunkX, int chunkZ, ChunkStatus status) {
        return ((long) chunkX & 0xfffffffL) | (((long) chunkZ & 0xfffffffL) << 28) | ((long) status.getIndex() << 56);
    }

    private void addToCache(long key, Chunk chunk) {
        for (int i = 3; i > 0; --i) {
            this.cacheKeys[i] = this.cacheKeys[i - 1];
            this.cacheChunks[i] = this.cacheChunks[i - 1];
        }

        this.cacheKeys[0] = key;
        this.cacheChunks[0] = chunk;
    }

    @Inject(method = "initChunkCaches()V", at = @At("HEAD"))
    private void onCachesCleared(CallbackInfo ci) {
        Arrays.fill(this.cacheKeys, Long.MAX_VALUE);
        Arrays.fill(this.cacheChunks, null);
    }
}
