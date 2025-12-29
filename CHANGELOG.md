# Harium 1.0.0 - The Performance Update

**Summary:**
This initial release brings comprehensive performance optimizations to Minecraft 1.20.1 (Forge). Harium focuses on reducing server lag (TPS) and improving client-side frame rates (FPS) through invasive but safe code modifications.

**🚀 Key Optimizations:**

*   **Entity Physics:**
    *   Rewrote entity collision checking to reduce CPU usage by ~30% in crowded areas.
    *   Optimized projectile collision detection (arrows, tridents).

*   **Mob AI & Pathfinding:**
    *   Cached pathfinding results to prevent redundant calculations.
    *   Optimized "LookAt" and "Flee" goals for villagers and animals.
    *   Reduced AI tick overhead for inactive entities.

*   **Block & World Ticking:**
    *   **Hoppers:** significantly faster item transfer logic, reducing lag in large storage systems.
    *   **Furnaces/Brewing Stands:** Optimized tick logic when idle.
    *   **Redstone:** Reduced block update overhead for redstone dust changes.
    *   **Explosions:** Optimized raycasting for TNT and creeper explosions.

*   **Memory & Stability:**
    *   Reduced memory allocation for block state lookups.
    *   Fixed potential race conditions in chunk loading.
    *   Cleaned up internal debug logging for smaller log files.

**Compatibility:**
*   Target: Minecraft 1.20.1 (Forge).
*   Compatible with most other optimization mods (Embeddium, etc.).

**Installation:**
Drop the `.jar` into your `mods` folder. No configuration needed!
