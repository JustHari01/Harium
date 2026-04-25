package me.jellysquid.mods.lithium.mixin.ai.non_poi_block_search;

import me.jellysquid.mods.lithium.common.ai.non_poi_block_search.LithiumMoveToBlockGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.entity.ai.goal.StepAndDestroyBlockGoal;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BiPredicate;

@Mixin(StepAndDestroyBlockGoal.class)
public abstract class StepAndDestroyBlockGoalMixin extends MoveToTargetPosGoal implements LithiumMoveToBlockGoal {
    @Shadow
    @Final
    private Block targetBlock;

    @Unique
    private static final BiPredicate<Chunk, BlockPos.Mutable> IS_VALID_TARGET_ABOVE_BIPREDICATE =
            StepAndDestroyBlockGoalMixin::lithium$isValidTargetAbove;

    public StepAndDestroyBlockGoalMixin(PathAwareEntity pathfinderMob, double d, int i) {
        super(pathfinderMob, d, i);
    }

    @Redirect(method = "canStart",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/goal/StepAndDestroyBlockGoal;findTargetPos()Z"))
    protected boolean redirectFindNearestBlock(StepAndDestroyBlockGoal removeBlockGoal) {
        return ((LithiumMoveToBlockGoal) removeBlockGoal).lithium$findNearestBlock(
                this::lithium$isValidTargetBlock, IS_VALID_TARGET_ABOVE_BIPREDICATE, false
        );
    }

    @Unique
    private boolean lithium$isValidTargetBlock(BlockState blockState) {
        return blockState.isOf(this.targetBlock);
    }

    @Unique
    private static boolean lithium$isValidTargetAbove(Chunk chunkAccess, BlockPos.Mutable mutable) {
        return chunkAccess.getBlockState(mutable.move(0, 1, 0)).isAir()
                && chunkAccess.getBlockState(mutable.move(0, 1, 0)).isAir();
    }
}
