package me.jellysquid.mods.lithium.mixin.entity.fast_retrieval;

import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SectionedEntityCache.class)
public abstract class SectionedEntityCacheMixin<T extends EntityLike> {
    @Shadow
    @Nullable
    public abstract EntityTrackingSection<T> findTrackingSection(long sectionPos);

    @Inject(
            method = "forEachInBox",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/util/math/ChunkSectionPos;getSectionCoord(D)I",
                    ordinal = 5
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    public void forEachInBox(Box box, LazyIterationConsumer<EntityTrackingSection<T>> action, CallbackInfo ci, int i, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (maxX >= minX + 4 || maxZ >= minZ + 4) {
            return; // Vanilla is likely more optimized when shooting entities with TNT cannons over huge distances.

        }
        ci.cancel();

        for (int x = minX; x <= maxX; x++) {
            for (int z = Math.max(minZ, 0); z <= maxZ; z++) {
                if (this.forEachInColumn(x, minY, maxY, z, action).shouldAbort()) {
                    return;
                }
            }

            int bound = Math.min(-1, maxZ);
            for (int z = minZ; z <= bound; z++) {
                if (this.forEachInColumn(x, minY, maxY, z, action).shouldAbort()) {
                    return;
                }
            }
        }
    }

    private LazyIterationConsumer.NextIteration forEachInColumn(int x, int minY, int maxY, int z, LazyIterationConsumer<EntityTrackingSection<T>> action) {
        LazyIterationConsumer.NextIteration ret = LazyIterationConsumer.NextIteration.CONTINUE;

        for (int y = Math.max(minY, 0); y <= maxY; y++) {
            if ((ret = this.consumeSection(ChunkSectionPos.asLong(x, y, z), action)).shouldAbort()) {
                return ret;
            }
        }
        int bound = Math.min(-1, maxY);
        for (int y = minY; y <= bound; y++) {
            if ((ret = this.consumeSection(ChunkSectionPos.asLong(x, y, z), action)).shouldAbort()) {
                return ret;
            }
        }
        return ret;
    }

    private LazyIterationConsumer.NextIteration consumeSection(long pos, LazyIterationConsumer<EntityTrackingSection<T>> action) {
        EntityTrackingSection<T> section = this.findTrackingSection(pos);
        if (section != null && 0 != section.size() && section.getStatus().shouldTrack()) {
            return action.accept(section);
        }
        return LazyIterationConsumer.NextIteration.CONTINUE;
    }
}
