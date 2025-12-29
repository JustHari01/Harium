package me.jellysquid.mods.lithium.mixin.math.fast_util;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Direction.class)
public class DirectionMixin {
    @Shadow
    @Final
    private static Direction[] ALL;

    @Shadow
    @Final
    private int idOpposite;

    @Overwrite
    public Direction getOpposite() {
        return ALL[this.idOpposite];
    }

    @Overwrite
    public static Direction random(Random rand) {
        return ALL[rand.nextInt(ALL.length)];
    }
}
