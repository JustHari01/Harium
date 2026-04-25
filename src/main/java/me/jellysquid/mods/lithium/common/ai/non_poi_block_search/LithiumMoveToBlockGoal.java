package me.jellysquid.mods.lithium.common.ai.non_poi_block_search;

import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.Chunk;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public interface LithiumMoveToBlockGoal {
    boolean lithium$findNearestBlock(Predicate<BlockState> requiredBlock,
                                     BiPredicate<Chunk, BlockPos.Mutable> lithium$isValidTarget,
                                     final boolean shouldChunkLoad);
}
