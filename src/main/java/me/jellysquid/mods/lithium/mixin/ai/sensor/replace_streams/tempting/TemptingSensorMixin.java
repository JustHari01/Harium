package me.jellysquid.mods.lithium.mixin.ai.sensor.replace_streams.tempting;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.TemptationsSensor;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TemptationsSensor.class)
public abstract class TemptingSensorMixin extends Sensor<PathAwareEntity> {

    @Shadow
    @Final
    private static TargetPredicate TEMPTER_PREDICATE;

    @Shadow
    public abstract boolean test(PlayerEntity player);

    /**
     * @author 2No2Name
     * @reason Replace Stream code with traditional iteration for better performance
     */
    @Overwrite
    protected void sense(ServerWorld serverWorld, PathAwareEntity entity) {
        Brain<?> brain = entity.getBrain();
        TargetPredicate targetingConditions = TEMPTER_PREDICATE.copy().setBaseMaxDistance(10.0);
        ServerPlayerEntity closestPlayer = null;
        double minDist = Double.MAX_VALUE;

        for (ServerPlayerEntity serverPlayer : serverWorld.getPlayers()) {
            if (EntityPredicates.VALID_ENTITY.test(serverPlayer)) {
                if (targetingConditions.test(entity, serverPlayer)) {
                    if (this.test(serverPlayer)) {
                        if (!entity.hasPassenger(serverPlayer)) {
                            double dist = entity.squaredDistanceTo(serverPlayer);
                            if (dist < minDist) {
                                minDist = dist;
                                closestPlayer = serverPlayer;
                            }
                        }
                    }
                }
            }
        }
        if (closestPlayer != null) {
            brain.remember(MemoryModuleType.TEMPTING_PLAYER, closestPlayer);
        } else {
            brain.forget(MemoryModuleType.TEMPTING_PLAYER);
        }
    }
}
