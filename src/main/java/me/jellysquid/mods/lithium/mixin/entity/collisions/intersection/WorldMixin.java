package me.jellysquid.mods.lithium.mixin.entity.collisions.intersection;

import me.jellysquid.mods.lithium.common.entity.LithiumEntityCollisions;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(World.class)
public abstract class WorldMixin implements WorldAccess {

    @Override
    public boolean isSpaceEmpty(@Nullable Entity entity, Box box) {
        boolean ret = !LithiumEntityCollisions.doesBoxCollideWithBlocks((World) (Object) this, entity, box);

        if (ret && this instanceof EntityView) {

            ret = !LithiumEntityCollisions.doesBoxCollideWithHardEntities(this, entity, box);
        }

        if (ret && entity != null) {
            ret = !LithiumEntityCollisions.doesEntityCollideWithWorldBorder(this, entity);
        }

        return ret;
    }
}