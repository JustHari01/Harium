package me.jellysquid.mods.lithium.mixin.ai.useless_behaviors.nitwit_job_search;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.jellysquid.mods.lithium.common.ai.useless_behaviors.LithiumEmptyBehavior;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.brain.task.VillagerTaskListProvider;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Nitwits still run AcquirePoi task for job sites even though it will always fail.
 * This removes that goal if the villager profession is NITWIT.
 *
 * In MC 1.20.1, the AcquirePoi call for job site acquisition was moved from
 * createWorkTasks to createCoreTasks, and createAcquirePointOfInterestTask was
 * removed entirely. The call now goes directly to FindPointOfInterestTask.create
 * (the 5-arg overload for job site + potential job site memory modules).
 *
 * @author jcw780
 */
@Mixin(VillagerTaskListProvider.class)
public abstract class VillagerTaskListProviderMixin {

    @WrapOperation(
            method = {"createCoreTasks", "method_19020"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/brain/task/FindPointOfInterestTask;create(Ljava/util/function/Predicate;Lnet/minecraft/entity/ai/brain/MemoryModuleType;Lnet/minecraft/entity/ai/brain/MemoryModuleType;ZLjava/util/Optional;)Lnet/minecraft/entity/ai/brain/task/Task;",
                    ordinal = 0)
    )
    private static Task<?> returnNullIfAcquirePoiIsUseless(
            Predicate<?> predicate, MemoryModuleType<?> memType1, MemoryModuleType<?> memType2,
            boolean scheduled, Optional<?> optional,
            @Local(argsOnly = true, ordinal = 0) VillagerProfession profession,
            Operation<Task<?>> original) {
        if (profession == VillagerProfession.NITWIT) {
            return (Task<?>) (Object) LithiumEmptyBehavior.EMPTY_BEHAVIOR_SENTINEL;
        }
        return original.call(predicate, memType1, memType2, scheduled, optional);
    }
}
