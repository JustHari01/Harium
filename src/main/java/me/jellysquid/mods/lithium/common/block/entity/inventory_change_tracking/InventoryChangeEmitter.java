package me.jellysquid.mods.lithium.common.block.entity.inventory_change_tracking;

import me.jellysquid.mods.lithium.common.hopper.LithiumStackList;

public interface InventoryChangeEmitter {
    void emitStackListReplaced();

    void emitRemoved();

    void emitContentModified();

    void emitFirstComparatorAdded();

    void forwardContentChangeOnce(InventoryChangeListener inventoryChangeListener, LithiumStackList stackList, InventoryChangeTracker thisTracker);

    void forwardMajorInventoryChanges(InventoryChangeListener inventoryChangeListener);

    void stopForwardingMajorInventoryChanges(InventoryChangeListener inventoryChangeListener);

    default void emitCallbackReplaced() {
        this.emitRemoved();
    }
}
