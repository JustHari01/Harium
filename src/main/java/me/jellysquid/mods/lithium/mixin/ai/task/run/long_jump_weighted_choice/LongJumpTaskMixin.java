package me.jellysquid.mods.lithium.mixin.ai.task.run.long_jump_weighted_choice;

import me.jellysquid.mods.lithium.common.util.collections.LongJumpChoiceList;
import net.minecraft.entity.ai.brain.task.LongJumpTask;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(LongJumpTask.class)
public class LongJumpTaskMixin<E extends MobEntity> {

    @Shadow
    protected List<LongJumpTask.Target> targets;

    @Shadow
    @Final
    protected int horizontalRange;

    @Shadow
    @Final
    protected int verticalRange;

    @Redirect(
            method = "run(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/mob/MobEntity;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;stream(IIIIII)Ljava/util/stream/Stream;")
    )
    private Stream<BlockPos> skipStreamGeneration(int x1, int y1, int z1, int x2, int y2, int z2) {
        if (this.horizontalRange < 128 && this.verticalRange < 128) {
            BlockPos center = new BlockPos(x1 + this.horizontalRange, y1 + this.verticalRange, z1 + this.horizontalRange);
            this.targets = LongJumpChoiceList.forCenter(center, (byte) this.horizontalRange, (byte) this.verticalRange);
            return Stream.empty();
        }
        return BlockPos.stream(x1, y1, z1, x2, y2, z2);
    }

    @Redirect(
            method = "getTarget(Lnet/minecraft/server/world/ServerWorld;)Ljava/util/Optional;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/Weighting;getRandom(Lnet/minecraft/util/math/random/Random;Ljava/util/List;)Ljava/util/Optional;")
    )
    private <T extends net.minecraft.util.collection.Weighted> Optional<T> getTargetFast(Random random, List<T> pool) {
        if (this.targets instanceof LongJumpChoiceList choiceList) {
            //noinspection unchecked
            return (Optional<T>) Optional.ofNullable(choiceList.removeRandomWeightedByDistanceSq(random));
        }
        return net.minecraft.util.collection.Weighting.getRandom(random, pool);
    }

    @Redirect(
            method = "getTarget(Lnet/minecraft/server/world/ServerWorld;)Ljava/util/Optional;",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V")
    )
    private void skipRemoveIfAlreadyRemoved(Optional<LongJumpTask.Target> result, Consumer<? super LongJumpTask.Target> removeAction) {
        if (!(this.targets instanceof LongJumpChoiceList)) {
            result.ifPresent(removeAction);
        }
    }
}
