package me.jellysquid.mods.lithium.mixin.gen.cached_generator_settings;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseChunkGenerator.class)
public class NoiseChunkGeneratorMixin {

    @Shadow
    @Final
    private RegistryEntry<ChunkGeneratorSettings> settings;
    private int cachedSeaLevel;

    @Overwrite
    public int getSeaLevel() {
        return this.cachedSeaLevel;
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/base/Suppliers;memoize(Lcom/google/common/base/Supplier;)Lcom/google/common/base/Supplier;",
                    remap = false,
                    shift = At.Shift.BEFORE
            )
    )
    private void hookConstructor(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings, CallbackInfo ci) {
        this.cachedSeaLevel = this.settings.value().seaLevel(); //TODO FIX Crash due to early access of registry
    }
}
