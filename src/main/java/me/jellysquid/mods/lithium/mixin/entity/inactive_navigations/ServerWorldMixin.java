package me.jellysquid.mods.lithium.mixin.entity.inactive_navigations;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import me.jellysquid.mods.lithium.common.entity.NavigatingEntity;
import me.jellysquid.mods.lithium.common.world.ServerWorldExtended;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements ServerWorldExtended {
    @Mutable
    @Shadow
    @Final
    Set<MobEntity> loadedMobs;

    private ReferenceOpenHashSet<EntityNavigation> activeNavigations;

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Redirect(
            method = "updateListeners(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"
            )
    )
    private Iterator<MobEntity> getActiveListeners(Set<MobEntity> set) {
        return Collections.emptyIterator();
    }

    @SuppressWarnings("rawtypes")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey worldKey, DimensionOptions dimensionOptions, WorldGenerationProgressListener worldGenerationProgressListener, boolean debugWorld, long seed, List spawners, boolean shouldTickTime, RandomSequencesState randomSequencesState, CallbackInfo ci) {
        this.loadedMobs = new ReferenceOpenHashSet<>(this.loadedMobs);
        this.activeNavigations = new ReferenceOpenHashSet<>();
    }

    @Override
    public void setNavigationActive(MobEntity mobEntity) {
        this.activeNavigations.add(((NavigatingEntity) mobEntity).getRegisteredNavigation());
    }

    @Override
    public void setNavigationInactive(MobEntity mobEntity) {
        this.activeNavigations.remove(((NavigatingEntity) mobEntity).getRegisteredNavigation());
    }

    @Inject(
            method = "updateListeners(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void updateActiveListeners(BlockPos pos, BlockState oldState, BlockState newState, int arg3, CallbackInfo ci, VoxelShape string, VoxelShape voxelShape, List<EntityNavigation> list) {
        for (EntityNavigation entityNavigation : this.activeNavigations) {
            if (entityNavigation.shouldRecalculatePath(pos)) {
                list.add(entityNavigation);
            }
        }
    }

    @SuppressWarnings("unused")
    public boolean isConsistent() {
        int i = 0;
        for (MobEntity mobEntity : this.loadedMobs) {
            EntityNavigation entityNavigation = mobEntity.getNavigation();
            if ((entityNavigation.getCurrentPath() != null && ((NavigatingEntity) mobEntity).isRegisteredToWorld()) != this.activeNavigations.contains(entityNavigation)) {
                return false;
            }
            if (entityNavigation.getCurrentPath() != null) {
                i++;
            }
        }
        return this.activeNavigations.size() == i;
    }
}
