package me.jellysquid.mods.lithium.mixin.entity.framed_maps;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Avoids O(N²) inventory scanning when a map is placed in an item frame.
 * When in a frame, only iterate once (the current player's tracker) instead
 * of scanning all N trackers for each of N players holding the map.
 */
@Mixin(MapState.class)
public abstract class MapStateMixin {

    @Shadow
    @Final
    private Map<PlayerEntity, MapState.PlayerUpdateTracker> updateTrackersByPlayer;

    @WrapOperation(
            method = "update(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0),
            require = 0
    )
    private int sizeOrOne(List<?> instance, Operation<Integer> original, @Local(argsOnly = true) ItemStack stack) {
        return stack.isInFrame() ? 1 : original.call(instance);
    }

    @WrapOperation(
            method = "update(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;", ordinal = 0),
            require = 0
    )
    private <E> E getOrGetPlayer(List<E> instance, int i, Operation<E> original,
                                  @Local(argsOnly = true) PlayerEntity player,
                                  @Local(argsOnly = true) ItemStack stack) {
        if (stack.isInFrame()) {
            //noinspection unchecked
            return (E) Objects.requireNonNull(this.updateTrackersByPlayer.get(player));
        }
        return original.call(instance, i);
    }
}
