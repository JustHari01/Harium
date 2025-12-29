package me.jellysquid.mods.lithium.common.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ChunkRandomSource {
    
    void getRandomPosInChunk(int x, int y, int z, int mask, BlockPos.Mutable out);
}
