package me.jellysquid.mods.lithium.common.entity.nearby_tracker;

import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import me.jellysquid.mods.lithium.common.util.tuples.Range6Int;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;

import java.util.List;

public class NearbyEntityTracker<T extends LivingEntity> implements NearbyEntityListener {
    private final Class<T> clazz;
    private final LivingEntity entity;

    private final Reference2LongOpenHashMap<T> nearbyEntities = new Reference2LongOpenHashMap<>(0);
    private long counter;
    private final Range6Int chunkBoxRadius;

    public NearbyEntityTracker(Class<T> clazz, LivingEntity entity, Vec3i boxRadius) {
        this.clazz = clazz;
        this.entity = entity;
        this.chunkBoxRadius = new Range6Int(
                1 + ChunkSectionPos.getSectionCoord(boxRadius.getX()),
                1 + ChunkSectionPos.getSectionCoord(boxRadius.getY()),
                1 + ChunkSectionPos.getSectionCoord(boxRadius.getZ()),
                1 + ChunkSectionPos.getSectionCoord(boxRadius.getX()),
                1 + ChunkSectionPos.getSectionCoord(boxRadius.getY()),
                1 + ChunkSectionPos.getSectionCoord(boxRadius.getZ())
        );
    }

    @Override
    public Class<? extends Entity> getEntityClass() {
        return this.clazz;
    }

    @Override
    public Range6Int getChunkRange() {
        return this.chunkBoxRadius;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEntityEnteredRange(Entity entity) {
        if (!this.clazz.isInstance(entity)) {
            return;
        }

        this.nearbyEntities.put((T) entity, this.counter++);
    }

    @Override
    public void onEntityLeftRange(Entity entity) {
        if (this.nearbyEntities.isEmpty() || !this.clazz.isInstance(entity)) {
            return;
        }

        this.nearbyEntities.removeLong(entity);
    }

    public T getClosestEntity(Box box, TargetPredicate targetPredicate, double x, double y, double z) {
        T nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;

        for (T entity : this.nearbyEntities.keySet()) {
            double distance;
            if (
                    (box == null || box.intersects(entity.getBoundingBox())) &&
                            (distance = entity.squaredDistanceTo(x, y, z)) <= nearestDistance &&
                            targetPredicate.test(this.getEntity(), entity)
            ) {
                if (distance == nearestDistance) {
                    nearest = this.getFirst(nearest, entity);
                } else {
                    nearest = entity;
                }
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private T getFirst(T entity1, T entity2) {
        if (this.getEntityClass() == PlayerEntity.class) {

            List<? extends PlayerEntity> players = this.getEntity().getEntityWorld().getPlayers();
            return players.indexOf((PlayerEntity)entity1) < players.indexOf((PlayerEntity)entity2) ? entity1 : entity2;
        } else {

            long pos1 = ChunkSectionPos.toLong(entity1.getBlockPos());
            long pos2 = ChunkSectionPos.toLong(entity2.getBlockPos());
            if (pos1 < pos2) {
                return entity1;
            } else if (pos2 < pos1) {
                return entity2;
            } else {
                if (this.nearbyEntities.getLong(entity1) < this.nearbyEntities.getLong(entity2)) {
                    return entity1;
                } else {
                    return entity2;
                }
            }
        }

    }

    @Override
    public String toString() {
        return super.toString() + " for entity class: " + this.clazz.getName() + ", around entity: " + this.getEntity().toString() + " with NBT: " + this.getEntity().writeNbt(new NbtCompound());
    }

    LivingEntity getEntity() {
        return entity;
    }
}
