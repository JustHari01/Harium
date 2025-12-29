package me.jellysquid.mods.lithium.common.world.chunk;

import net.minecraft.world.chunk.Palette;

public interface CompactingPackedIntegerArray {
    
    <T> void compact(Palette<T> srcPalette, Palette<T> dstPalette, short[] out);
}
