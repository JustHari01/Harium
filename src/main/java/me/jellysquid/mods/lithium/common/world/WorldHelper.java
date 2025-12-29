package me.jellysquid.mods.lithium.common.world;

import me.jellysquid.mods.lithium.common.client.ClientWorldAccessor;
import me.jellysquid.mods.lithium.common.entity.EntityClassGroup;
import me.jellysquid.mods.lithium.common.entity.pushable.EntityPushablePredicate;
import me.jellysquid.mods.lithium.common.world.chunk.ClassGroupFilterableList;
import me.jellysquid.mods.lithium.mixin.chunk.entity_class_groups.ClientEntityManagerAccessor;
import me.jellysquid.mods.lithium.mixin.chunk.entity_class_groups.EntityTrackingSectionAccessor;
import me.jellysquid.mods.lithium.mixin.chunk.entity_class_groups.ServerEntityManagerAccessor;
import me.jellysquid.mods.lithium.mixin.chunk.entity_class_groups.ServerWorldAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.entity.SectionedEntityCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WorldHelper {
    public static final boolean CUSTOM_TYPE_FILTERABLE_LIST_DISABLED = !ClassGroupFilterableList.class.isAssignableFrom(TypeFilterableList.class);

    public static List<Entity> getEntitiesForCollision(EntityView entityView, Box box, Entity collidingEntity) {
        if (!CUSTOM_TYPE_FILTERABLE_LIST_DISABLED && entityView instanceof World world && (collidingEntity == null || !EntityClassGroup.MINECART_BOAT_LIKE_COLLISION.contains(collidingEntity.getClass()))) {
            SectionedEntityCache<Entity> cache = getEntityCacheOrNull(world);
            if (cache != null) {
                world.getProfiler().visit("getEntities");
                return getEntitiesOfClassGroup(cache, collidingEntity, EntityClassGroup.NoDragonClassGroup.BOAT_SHULKER_LIKE_COLLISION, box);
            }
        }

        return entityView.getOtherEntities(collidingEntity, box);
    }

    public static SectionedEntityCache<Entity> getEntityCacheOrNull(World world) {
        if (world instanceof ClientWorldAccessor) {

            return ((ClientEntityManagerAccessor<Entity>) ((ClientWorldAccessor) world).getEntityManager()).getCache();
        } else if (world instanceof ServerWorldAccessor) {

            return ((ServerEntityManagerAccessor<Entity>) ((ServerWorldAccessor) world).getEntityManager()).getCache();
        }
        return null;
    }

    public static List<Entity> getEntitiesOfClassGroup(SectionedEntityCache<Entity> cache, Entity collidingEntity, EntityClassGroup.NoDragonClassGroup entityClassGroup, Box box) {
        ArrayList<Entity> entities = new ArrayList<>();
        cache.forEachInBox(box, section -> {

            TypeFilterableList<Entity> allEntities = ((EntityTrackingSectionAccessor<Entity>) section).getCollection();

            Collection<Entity> entitiesOfType = ((ClassGroupFilterableList<Entity>) allEntities).getAllOfGroupType(entityClassGroup);
            if (!entitiesOfType.isEmpty()) {
                for (Entity entity : entitiesOfType) {
                    if (entity.getBoundingBox().intersects(box) && !entity.isSpectator() && entity != collidingEntity) {

                        entities.add(entity);
                    }
                }
            }
            return LazyIterationConsumer.NextIteration.CONTINUE;
        });
        return entities;
    }

    public static List<Entity> getPushableEntities(World world, SectionedEntityCache<Entity> cache, Entity except, Box box, EntityPushablePredicate<? super Entity> entityPushablePredicate) {
        ArrayList<Entity> entities = new ArrayList<>();
        cache.forEachInBox(box, section -> ((ClimbingMobCachingSection) section).collectPushableEntities(world, except, box, entityPushablePredicate, entities));
        return entities;
    }

    public static boolean areNeighborsWithinSameChunk(BlockPos pos) {
        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;

        return localX > 0 && localZ > 0 && localX < 15 && localZ < 15;
    }

    public static boolean areNeighborsWithinSameChunkSection(BlockPos pos) {
        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;

        return localX > 0 && localY > 0 && localZ > 0 && localX < 15 && localY < 15 && localZ < 15;
    }

    public static boolean arePosWithinSameChunk(BlockPos pos1, BlockPos pos2) {
        return pos1.getX() >> 4 == pos2.getX() >> 4 && pos1.getZ() >> 4 == pos2.getZ() >> 4;
    }
}
