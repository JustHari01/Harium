package me.jellysquid.mods.lithium.mixin.ai.non_poi_block_search;

import me.jellysquid.mods.lithium.common.ai.non_poi_block_search.CommonBlockSearchesCheckAndCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.entity.ai.brain.sensor.HoglinSpecificSensor;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(HoglinSpecificSensor.class)
public abstract class HoglinSpecificSensorMixin {
    @Unique
    private static final Predicate<BlockState> IS_VALID_REPELLENT_PREDICATE =
            HoglinSpecificSensorMixin::lithium$isValidRepellent;

    @Redirect(method = "sense(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/mob/HoglinEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/sensing/HoglinSpecificSensor;m_26664_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/hoglin/Hoglin;)Ljava/util/Optional;",
                    remap = false))
    private Optional<BlockPos> redirectFindNearestRepellent(HoglinSpecificSensor instance, ServerWorld serverLevel,
                                                            HoglinEntity hoglin) {
        return CommonBlockSearchesCheckAndCache.blockPosFindClosestMatch(serverLevel, hoglin, 8, 4,
                IS_VALID_REPELLENT_PREDICATE, true);
    }

    @Unique
    private static boolean lithium$isValidRepellent(BlockState blockState) {
        return blockState.isIn(BlockTags.HOGLIN_REPELLENTS);
    }
}
