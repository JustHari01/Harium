package me.jellysquid.mods.lithium.mixin.ai.pathing;

import me.jellysquid.mods.lithium.common.util.Pos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkCache;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkCache.class)
public abstract class ChunkCacheMixin implements BlockView {
    private static final BlockState DEFAULT_BLOCK = Blocks.AIR.getDefaultState();

    @Shadow
    @Final
    protected Chunk[][] chunks;

    @Shadow
    @Final
    protected int minX;

    @Shadow
    @Final
    protected int minZ;

    @Shadow
    @Final
    protected World world;

    private Chunk[] chunksFlat;

    private int xLen, zLen;

    private int bottomY, topY;

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)V", at = @At("RETURN"))
    private void init(World world, BlockPos minPos, BlockPos maxPos, CallbackInfo ci) {
        this.xLen = 1 + (Pos.ChunkCoord.fromBlockCoord(maxPos.getX())) - (Pos.ChunkCoord.fromBlockCoord(minPos.getX()));
        this.zLen = 1 + (Pos.ChunkCoord.fromBlockCoord(maxPos.getZ())) - (Pos.ChunkCoord.fromBlockCoord(minPos.getZ()));

        this.chunksFlat = new Chunk[this.xLen * this.zLen];

        for (int x = 0; x < this.xLen; x++) {
            System.arraycopy(this.chunks[x], 0, this.chunksFlat, x * this.zLen, this.zLen);
        }

        this.bottomY = this.getBottomY();
        this.topY = this.getTopY();
    }

    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        int y = pos.getY();

        if (!(y < this.bottomY || y >= this.topY)) {
            int x = pos.getX();
            int z = pos.getZ();

            int chunkX = (Pos.ChunkCoord.fromBlockCoord(x)) - this.minX;
            int chunkZ = (Pos.ChunkCoord.fromBlockCoord(z)) - this.minZ;

            if (chunkX >= 0 && chunkX < this.xLen && chunkZ >= 0 && chunkZ < this.zLen) {
                Chunk chunk = this.chunksFlat[(chunkX * this.zLen) + chunkZ];

                if (chunk != null) {
                    ChunkSection section = chunk.getSectionArray()[Pos.SectionYIndex.fromBlockCoord(this, y)];

                    if (section != null) {
                        return section.getBlockState(x & 15, y & 15, z & 15);
                    }
                }
            }
        }

        return DEFAULT_BLOCK;
    }

    @Overwrite
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }
}

