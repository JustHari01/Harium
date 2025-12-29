package me.jellysquid.mods.lithium.common.entity.pushable;

import me.jellysquid.mods.lithium.common.entity.EntityClassGroup;
import me.jellysquid.mods.lithium.common.reflection.ReflectionUtil;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;

public class PushableEntityClassGroup {

    public static final EntityClassGroup CACHABLE_UNPUSHABILITY;

    public static final EntityClassGroup MAYBE_PUSHABLE;

    static {
        String remapped_isClimbing = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_6147_");
        String remapped_isPushable = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_6094_");
        CACHABLE_UNPUSHABILITY = new EntityClassGroup(
                (Class<?> entityClass) -> {
                    if (LivingEntity.class.isAssignableFrom(entityClass) && !PlayerEntity.class.isAssignableFrom(entityClass)) {
                        if (!ReflectionUtil.hasMethodOverride(entityClass, LivingEntity.class, true, remapped_isPushable)) {
                            if (!ReflectionUtil.hasMethodOverride(entityClass, LivingEntity.class, true, remapped_isClimbing)) {
                                return true;
                            }
                        }
                    }
                    return false;
                });
        MAYBE_PUSHABLE = new EntityClassGroup(
                (Class<?> entityClass) -> {
                    if (ReflectionUtil.hasMethodOverride(entityClass, Entity.class, true, remapped_isPushable)) {
                        if (EnderDragonEntity.class.isAssignableFrom(entityClass)) {
                            return false;
                        }
                        if (ArmorStandEntity.class.isAssignableFrom(entityClass)) {
                            return ReflectionUtil.hasMethodOverride(entityClass, ArmorStandEntity.class, true, remapped_isPushable);
                        }
                        if (BatEntity.class.isAssignableFrom(entityClass)) {
                            return ReflectionUtil.hasMethodOverride(entityClass, BatEntity.class, true, remapped_isPushable);
                        }
                        return true;
                    }
                    if (PlayerEntity.class.isAssignableFrom(entityClass)) {
                        return true;
                    }
                    return false;
                });
    }
}
