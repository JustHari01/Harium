package me.jellysquid.mods.lithium.mixin.entity.data_tracker.use_arrays;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;

@Mixin(DataTracker.class)
public abstract class DataTrackerMixin {
    private static final int DEFAULT_ENTRY_COUNT = 10, GROW_FACTOR = 8;

    @Shadow
    @Final
    private ReadWriteLock lock;

    @Mutable
    @Shadow
    @Final
    private Int2ObjectMap<DataTracker.Entry<?>> entries;
    
    private DataTracker.Entry<?>[] entriesArray = new DataTracker.Entry<?>[DEFAULT_ENTRY_COUNT];

    @Redirect(
            method = "addTrackedData(Lnet/minecraft/entity/data/TrackedData;Ljava/lang/Object;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;put(ILjava/lang/Object;)Ljava/lang/Object;",
                    remap = false
            )
    )
    private Object onAddTrackedDataInsertMap(Int2ObjectMap<?> int2ObjectMap, int k, Object valueRaw) {
        DataTracker.Entry<?> v = (DataTracker.Entry<?>) valueRaw;

        DataTracker.Entry<?>[] storage = this.entriesArray;

        if (storage.length <= k) {

            int newSize = Math.min(k + GROW_FACTOR, 256);

            this.entriesArray = storage = Arrays.copyOf(storage, newSize);
        }

        storage[k] = v;

        return this.entries.put(k, v);
    }

    @Overwrite
    private <T> DataTracker.Entry<T> getEntry(TrackedData<T> data) {
        this.lock.readLock().lock();

        try {
            DataTracker.Entry<?>[] array = this.entriesArray;

            int id = data.getId();

            if (id < 0 || id >= array.length) {
                return null;
            }

            return (DataTracker.Entry<T>) array[id];
        } catch (Throwable cause) {

            throw onGetException(cause, data);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private static <T> CrashException onGetException(Throwable cause, TrackedData<T> data) {
        CrashReport report = CrashReport.create(cause, "Getting synced entity data");

        CrashReportSection section = report.addElement("Synced entity data");
        section.add("Data ID", data);

        return new CrashException(report);
    }
}
