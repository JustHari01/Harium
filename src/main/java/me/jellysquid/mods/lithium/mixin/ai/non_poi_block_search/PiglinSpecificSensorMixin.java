package me.jellysquid.mods.lithium.mixin.ai.non_poi_block_search;

import me.jellysquid.mods.lithium.common.ai.non_poi_block_search.CommonBlockSearchesCheckAndCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.sensor.PiglinSpecificSensor;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(PiglinSpecificSensor.class)
public abstract class PiglinSpecificSensorMixin {
    @Unique
    private static final Predicate<BlockState> IS_VALID_REPELLENT_PREDICATE =
            PiglinSpecificSensorMixin::lithium$isValidRepellent;

    @Redirect(method = "sense(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/sensing/PiglinSpecificSensor;m_26734_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Optional;",
                    remap = false))
    public Optional<BlockPos> redirectFindNearestRepellent(ServerWorld serverLevel, LivingEntity livingEntity) {
        return CommonBlockSearchesCheckAndCache.blockPosFindClosestMatch(serverLevel, livingEntity, 8, 4,
                IS_VALID_REPELLENT_PREDICATE, true);
    }

    @Unique
    private static boolean lithium$isValidRepellent(BlockState blockState) {
        final boolean isPiglinRepellent = blockState.isIn(BlockTags.PIGLIN_REPELLENTS);
        return isPiglinRepellent && blockState.isOf(Blocks.SOUL_CAMPFIRE) ?
                CampfireBlock.isLitCampfire(blockState) : isPiglinRepellent;
    }
}
