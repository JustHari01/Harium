package me.jellysquid.mods.lithium.mixin.entity.projectile_projectile_collisions;

import me.jellysquid.mods.lithium.common.entity.projectile.ProjectileEntityClassGroup;
import me.jellysquid.mods.lithium.common.world.WorldHelper;
import me.jellysquid.mods.lithium.common.world.chunk.ClassGroupFilterableList;
import me.jellysquid.mods.lithium.mixin.chunk.entity_class_groups.EntityTrackingSectionAccessor;
import me.jellysquid.mods.lithium.mixin.chunk.entity_class_groups.ServerEntityManagerAccessor;
import me.jellysquid.mods.lithium.mixin.chunk.entity_class_groups.ServerWorldAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.world.entity.SectionedEntityCache;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

    @Redirect(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;")
    )
    private static List<Entity> getEntitiesForCollision(World world, Entity searchingEntity, Box box, Predicate<? super Entity> entityFilter) {
        if (!WorldHelper.CUSTOM_TYPE_FILTERABLE_LIST_DISABLED && searchingEntity != null
                && ProjectileEntityClassGroup.OPTIMIZED_PROJECTILES.contains(searchingEntity.getClass())
                && world instanceof ServerWorldAccessor) {
            ServerEntityManager<Entity> entityManager = ((ServerWorldAccessor) world).getEntityManager();
            //noinspection unchecked
            SectionedEntityCache<Entity> cache = ((ServerEntityManagerAccessor<Entity>) entityManager).getCache();
            if (cache != null) {
                ArrayList<Entity> entities = new ArrayList<>();
                cache.forEachInBox(box, section -> {
                    //noinspection unchecked
                    TypeFilterableList<Entity> allEntities = ((EntityTrackingSectionAccessor<Entity>) section).getCollection();
                    //noinspection unchecked
                    Collection<Entity> hittableEntities = ((ClassGroupFilterableList<Entity>) allEntities)
                            .getAllOfGroupType(ProjectileEntityClassGroup.CAN_MAYBE_BE_HIT_BY_OPTIMIZED_PROJECTILE);
                    if (!hittableEntities.isEmpty()) {
                        for (Entity entity : hittableEntities) {
                            if (entity.getBoundingBox().intersects(box) && !entity.isSpectator()
                                    && entity != searchingEntity && entityFilter.test(entity)) {
                                entities.add(entity);
                            }
                        }
                    }
                    return LazyIterationConsumer.NextIteration.CONTINUE;
                });
                return entities;
            }
        }
        return world.getOtherEntities(searchingEntity, box, entityFilter);
    }
}
