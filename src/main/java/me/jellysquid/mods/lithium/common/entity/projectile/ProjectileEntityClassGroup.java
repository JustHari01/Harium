package me.jellysquid.mods.lithium.common.entity.projectile;

import me.jellysquid.mods.lithium.common.entity.EntityClassGroup;
import me.jellysquid.mods.lithium.common.reflection.ReflectionUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import cpw.mods.modlauncher.api.INameMappingService;

import java.util.logging.Logger;

/**
 * Groups projectile entity classes for optimized collision detection.
 * Simplified version for 1.20.1 using class-only predicates.
 */
public class ProjectileEntityClassGroup {

    /**
     * Projectiles that do not override canHit(Entity) - they use default collision behavior.
     * These projectiles can skip collision checks with entities that can't be hit.
     */
    public static final EntityClassGroup OPTIMIZED_PROJECTILES;

    /**
     * Entities that can potentially be hit by optimized projectiles.
     * These override canHit() or isCollidable(), making them valid collision targets.
     */
    public static final EntityClassGroup.NoDragonClassGroup CAN_MAYBE_BE_HIT_BY_OPTIMIZED_PROJECTILE;

    static {
        String remapped_canHit = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_5603_");
        OPTIMIZED_PROJECTILES = new EntityClassGroup(
                (Class<?> entityClass) -> {
                    Class<?> parentClass = ProjectileEntity.class;
                    if (PersistentProjectileEntity.class.isAssignableFrom(entityClass)) {
                        parentClass = PersistentProjectileEntity.class;
                    } else if (ShulkerBulletEntity.class.isAssignableFrom(entityClass)) {
                        parentClass = ShulkerBulletEntity.class;
                    }
                    return !ReflectionUtil.hasMethodOverride(entityClass, parentClass, true, remapped_canHit, Entity.class);
                });

        String remapped_canHitEntity = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_5603_");
        String remapped_isCollidable = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_5829_");
        CAN_MAYBE_BE_HIT_BY_OPTIMIZED_PROJECTILE = new EntityClassGroup.NoDragonClassGroup(
                (Class<?> entityClass) -> {
                    // Entities that override canHit() are hittable
                    if (ReflectionUtil.hasMethodOverride(entityClass, Entity.class, true, remapped_canHitEntity)) {
                        return true;
                    }
                    // Entities that override isCollidable() can be collided with
                    return ReflectionUtil.hasMethodOverride(entityClass, Entity.class, true, remapped_isCollidable);
                });

        // Clear initial state - classes will be evaluated lazily
        OPTIMIZED_PROJECTILES.clear();
        CAN_MAYBE_BE_HIT_BY_OPTIMIZED_PROJECTILE.clear();
    }
}
