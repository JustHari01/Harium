package me.jellysquid.mods.lithium.common.ai.useless_behaviors;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;

/**
 * Dummy behavior to replace useless behaviors with.
 * These will not be added to the brain of the entity.
 */
public class LithiumEmptyBehavior<E extends LivingEntity> implements Task<E> {
    public static final LithiumEmptyBehavior<?> EMPTY_BEHAVIOR_SENTINEL = new LithiumEmptyBehavior<>();

    @Override
    public MultiTickTask.Status getStatus() {
        return MultiTickTask.Status.STOPPED;
    }

    @Override
    public boolean tryStarting(ServerWorld world, E entity, long time) {
        return false;
    }

    @Override
    public void tick(ServerWorld world, E entity, long time) {
    }

    @Override
    public void stop(ServerWorld world, E entity, long time) {
    }

    @Override
    public String getName() {
        return "Lithium Empty Behavior";
    }
}
