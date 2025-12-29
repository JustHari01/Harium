package me.jellysquid.mods.lithium.mixin.math.fast_blockpos;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockPos.class)
public abstract class BlockPosMixin extends Vec3i {

    public BlockPosMixin(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    @Overwrite
    public BlockPos up() {
        return new BlockPos(this.getX(), this.getY() + 1, this.getZ());
    }

    @Override
    @Overwrite
    public BlockPos up(int distance) {
        return new BlockPos(this.getX(), this.getY() + distance, this.getZ());
    }

    @Override
    @Overwrite
    public BlockPos down() {
        return new BlockPos(this.getX(), this.getY() - 1, this.getZ());
    }

    @Override
    @Overwrite
    public BlockPos down(int distance) {
        return new BlockPos(this.getX(), this.getY() - distance, this.getZ());
    }

    @Overwrite
    public BlockPos north() {
        return new BlockPos(this.getX(), this.getY(), this.getZ() - 1);
    }

    @Overwrite
    public BlockPos north(int distance) {
        return new BlockPos(this.getX(), this.getY(), this.getZ() - distance);
    }

    @Overwrite
    public BlockPos south() {
        return new BlockPos(this.getX(), this.getY(), this.getZ() + 1);
    }

    @Overwrite
    public BlockPos south(int distance) {
        return new BlockPos(this.getX(), this.getY(), this.getZ() + distance);
    }

    @Overwrite
    public BlockPos west() {
        return new BlockPos(this.getX() - 1, this.getY(), this.getZ());
    }

    @Overwrite
    public BlockPos west(int distance) {
        return new BlockPos(this.getX() - distance, this.getY(), this.getZ());
    }

    @Overwrite
    public BlockPos east() {
        return new BlockPos(this.getX() + 1, this.getY(), this.getZ());
    }

    @Overwrite
    public BlockPos east(int distance) {
        return new BlockPos(this.getX() + distance, this.getY(), this.getZ());
    }
}
