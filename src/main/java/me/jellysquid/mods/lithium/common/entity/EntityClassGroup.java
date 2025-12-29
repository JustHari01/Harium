package me.jellysquid.mods.lithium.common.entity;

import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import me.jellysquid.mods.lithium.common.reflection.ReflectionUtil;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.vehicle.MinecartEntity;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class EntityClassGroup {
    public static final EntityClassGroup MINECART_BOAT_LIKE_COLLISION; //aka entities that will attempt to collide with all other entities when moving

    static {
        String remapped_collidesWith = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_7337_");
        MINECART_BOAT_LIKE_COLLISION = new EntityClassGroup(
                (Class<?> entityClass) -> ReflectionUtil.hasMethodOverride(entityClass, Entity.class, true, remapped_collidesWith, Entity.class));

        if ((!MINECART_BOAT_LIKE_COLLISION.contains(MinecartEntity.class))) {
            throw new AssertionError();
        }
        if ((MINECART_BOAT_LIKE_COLLISION.contains(ShulkerEntity.class))) {

            Logger.getLogger("Lithium EntityClassGroup").warning("Either Lithium EntityClassGroup is broken or something else gave Shulkers the minecart-like collision behavior.");
        }
        MINECART_BOAT_LIKE_COLLISION.clear();
    }

    private final Predicate<Class<?>> classFitEvaluator;
    private volatile Reference2ByteOpenHashMap<Class<?>> class2GroupContains;

    public EntityClassGroup(Predicate<Class<?>> classFitEvaluator) {
        this.class2GroupContains = new Reference2ByteOpenHashMap<>();
        Objects.requireNonNull(classFitEvaluator);
        this.classFitEvaluator = classFitEvaluator;
    }

    public void clear() {
        this.class2GroupContains = new Reference2ByteOpenHashMap<>();
    }

    public boolean contains(Class<?> entityClass) {
        byte contains = this.class2GroupContains.getOrDefault(entityClass, (byte) 2);
        if (contains != 2) {
            return contains == 1;
        } else {
            return this.testAndAddClass(entityClass);
        }
    }

    boolean testAndAddClass(Class<?> entityClass) {
        byte contains;

        synchronized (this) {

            contains = this.class2GroupContains.getOrDefault(entityClass, (byte) 2);
            if (contains != 2) {
                return contains == 1;
            }

            Reference2ByteOpenHashMap<Class<?>> newMap = this.class2GroupContains.clone();
            contains = this.classFitEvaluator.test(entityClass) ? (byte) 1 : (byte) 0;
            newMap.put(entityClass, contains);

            this.class2GroupContains = newMap;
        }
        return contains == 1;
    }

    public static class NoDragonClassGroup extends EntityClassGroup {
        public static final NoDragonClassGroup BOAT_SHULKER_LIKE_COLLISION; //aka entities that other entities will do block-like collisions with when moving

        static {
            String remapped_isCollidable = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_5829_");
            BOAT_SHULKER_LIKE_COLLISION = new NoDragonClassGroup(
                    (Class<?> entityClass) -> ReflectionUtil.hasMethodOverride(entityClass, Entity.class, true, remapped_isCollidable));

            if ((!BOAT_SHULKER_LIKE_COLLISION.contains(ShulkerEntity.class))) {
                throw new AssertionError();
            }
            BOAT_SHULKER_LIKE_COLLISION.clear();
        }

        public NoDragonClassGroup(Predicate<Class<?>> classFitEvaluator) {
            super(classFitEvaluator);
            if (classFitEvaluator.test(EnderDragonEntity.class)) {
                throw new IllegalArgumentException("EntityClassGroup.NoDragonClassGroup cannot be initialized: Must exclude EnderDragonEntity!");
            }
        }
    }
}