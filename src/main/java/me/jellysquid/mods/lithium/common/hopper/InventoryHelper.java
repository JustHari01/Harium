package me.jellysquid.mods.lithium.common.hopper;

import me.jellysquid.mods.lithium.api.inventory.LithiumInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class InventoryHelper {
    public static LithiumStackList getLithiumStackList(LithiumInventory inventory) {
        DefaultedList<ItemStack> stackList = inventory.getInventoryLithium();
        if (stackList instanceof LithiumStackList lithiumStackList) {
            return lithiumStackList;
        }
        return upgradeToLithiumStackList(inventory);
    }

    public static LithiumStackList getLithiumStackListOrNull(LithiumInventory inventory) {
        DefaultedList<ItemStack> stackList = inventory.getInventoryLithium();
        if (stackList instanceof LithiumStackList lithiumStackList) {
            return lithiumStackList;
        }
        return null;
    }

    private static LithiumStackList upgradeToLithiumStackList(LithiumInventory inventory) {

        inventory.generateLootLithium();

        DefaultedList<ItemStack> stackList = inventory.getInventoryLithium();
        LithiumStackList lithiumStackList = new LithiumStackList(stackList, inventory.getMaxCountPerStack());
        inventory.setInventoryLithium(lithiumStackList);
        return lithiumStackList;
    }
}
