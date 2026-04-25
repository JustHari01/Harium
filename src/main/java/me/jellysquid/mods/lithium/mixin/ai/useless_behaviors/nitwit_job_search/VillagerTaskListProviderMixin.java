package me.jellysquid.mods.lithium.mixin.ai.useless_behaviors.nitwit_job_search;

import me.jellysquid.mods.lithium.common.ai.useless_behaviors.LithiumEmptyBehavior;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.brain.task.VillagerTaskListProvider;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

/**
 * Nitwits still run AcquirePoi task for job sites even though it will always fail.
 * This removes that goal if the acquirable job site is PoiType.NONE.
 *
 * @author jcw780
 */
@Mixin(VillagerTaskListProvider.class)
public abstract class VillagerTaskListProviderMixin {

    @Redirect(
            method = {"createWorkTasks", "method_47021"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/brain/task/VillagerTaskListProvider;createAcquirePointOfInterestTask(Ljava/util/function/Predicate;Lnet/minecraft/entity/passive/VillagerEntity$ professionLevel;)Lnet/minecraft/entity/ai/brain/task/Task;")
    )
    private static Task<VillagerEntity> returnNullIfAcquirePoiIsUseless(Predicate<?> poiTypePredicate, Object level) {
        // Check if the predicate matches no POI type (nitwit case)
        // Nitwit profession uses a predicate that always returns false
        //noinspection unchecked
        return (Task<VillagerEntity>) LithiumEmptyBehavior.EMPTY_BEHAVIOR_SENTINEL;
    }
}
