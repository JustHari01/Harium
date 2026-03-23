package me.jellysquid.mods.lithium.common.compat.vs;

import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Interface to hold a World reference.
 * This allows VS to inject the Level into POI storage mixins.
 * The interface is named differently from VS's OfLevel to avoid conflicts,
 * but serves the same purpose.
 */
public interface LevelHolder {
    @Nullable
    World lithium$getLevel();

    void lithium$setLevel(@Nullable World world);
}
