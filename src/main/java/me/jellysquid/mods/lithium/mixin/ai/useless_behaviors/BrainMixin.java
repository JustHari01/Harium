package me.jellysquid.mods.lithium.mixin.ai.useless_behaviors;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import me.jellysquid.mods.lithium.common.ai.useless_behaviors.LithiumEmptyBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.task.Task;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;

@Mixin(Brain.class)
public abstract class BrainMixin<T extends LivingEntity> {

    /**
     * @author jcw780, 2No2Name
     * @reason Prevent EMPTY_BEHAVIOR_SENTINEL from being added - those are what useless behaviors are replaced with.
     */
    @SuppressWarnings("unchecked")
    @Redirect(
            method = "setTaskList(Lnet/minecraft/entity/ai/brain/Activity;Lcom/google/common/collect/ImmutableList;Ljava/util/Set;Ljava/util/Set;)V",
            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;iterator()Lcom/google/common/collect/UnmodifiableIterator;")
    )
    private UnmodifiableIterator<Pair<Integer, ? extends Task<? super T>>> filterSentinels(
            ImmutableList<Pair<Integer, ? extends Task<? super T>>> instance) {
        Iterator<Pair<Integer, ? extends Task<? super T>>> wrapped = instance.iterator();
        return new AbstractIterator<Pair<Integer, ? extends Task<? super T>>>() {
            @Override
            protected @Nullable Pair<Integer, ? extends Task<? super T>> computeNext() {
                while (wrapped.hasNext()) {
                    Pair<Integer, ? extends Task<? super T>> next = wrapped.next();
                    if (next.getSecond() != LithiumEmptyBehavior.EMPTY_BEHAVIOR_SENTINEL) {
                        return next;
                    }
                }
                return this.endOfData();
            }
        };
    }
}
