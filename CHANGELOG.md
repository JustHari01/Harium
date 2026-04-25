# Harium Changelog

## [1.2.1] - 2026-04-22
### Bug Fixes & Robustness

**CRITICAL**
- **Sculk Catalyst sleeping deadlock** — Added missing `SculkSpreaderMixin` and wired `GameEventListenerWithCallback` into the catalyst constructor. Without this, catalysts that went to sleep never woke up because no callback was registered to detect new cursor arrivals.
- **Catalyst constructor callback wiring** — The sleeping sculk catalyst constructor now correctly passes the wakeup callback through `SleepingBlockEntity.setSleeping()`, ensuring the block entity re-enters the tick loop when spreading resumes.

**MODERATE**
- **CalibratedSculkSensor sleeping** — Calibrated sculk sensor block entities are now properly put to sleep alongside regular sculk sensors and shriekers when they have no pending vibrations.
- **`@Redirect` → `@WrapOperation` migration** — Replaced `@Redirect` annotations with MixinExtras `@WrapOperation` for better mod compatibility. `@Redirect` breaks when multiple mods target the same injection point; `@WrapOperation` allows cooperative chaining.
- **`lithium$` method prefix (17 files)** — All `SleepingBlockEntity` override methods across sleeping block entity mixins now use the `lithium$` prefix convention, preventing name collisions with other mods that override the same methods.
- **`VibrationListenerMixin` injection point** — Changed injection from fragile `@At("RETURN")` to a more robust injection strategy on `listen()`, reducing the risk of the mixin failing silently if the method descriptor shifts between mapping versions.

**LOW-MODERATE**
- **`WorldChunkMixin` local capture** — Replaced `CAPTURE_FAILHARD` with positional `@Local` captures using MixinExtras `@Local(ordinal=N)`, eliminating reliance on fragile implicit local variable ordering.
- **`WrappedBlockEntityTickInvokerAccessor` explicit targets** — Added explicit `target` names to `@Invoker` and `@Accessor` annotations instead of relying on implicit name-based resolution, improving reliability across mapping changes.

#### Files Changed
- 18 files modified (core sleeping system, all sleeping BE mixins, mixin config)
- `harium.mixins.json` updated with new/adjusted mixin entries
- `harium-mixin-config-default.properties` updated with new config defaults
- `harium-mixin-config.md` and `lithium-mixin-config.md` documentation updated

## [1.2.0] - 2026-04-21
### Backported from Lithium 0.24.2 (MC 1.21.x / NeoForge+Fabric) to Forge 1.20.1

#### New Optimizations

**AI & Brain**
- **Non-POI Block Search** - Optimize mob block searches that don't use the POI system (MoveToTargetPosGoal, HoglinSpecificSensor, PiglinSpecificSensor, StepAndDestroyBlockGoal). Uses `ChunkSection.hasAny()` to pre-check sections, caches `Chunk` objects to avoid repeated lookups, and provides chunk-aware search ordering. Most impactful in portal gold farms where many mobs search for blocks simultaneously.
  - `MoveToTargetPosGoal` (zombies, turtles, etc.) - chunk-aware search skips empty sections, reduces lag from large search ranges (e.g. 47x7x47 for zombies)
  - `HoglinSpecificSensor` - optimized hoglin repellent block search using pre-cached chunk sections
  - `PiglinSpecificSensor` - optimized piglin repellent block search with soul campfire lit-state awareness
  - `StepAndDestroyBlockGoal` (zombies trampling turtle eggs) - cached chunk access with early-exit

**World**
- **Sculk Sensor Sleeping** - Put sculk sensor and sculk shrieker block entities to sleep when they have no pending vibrations. Sensors automatically wake up when a vibration event is detected via the `VibrationListener` callback, and on NBT load if saved data contains pending vibrations.

#### New Utility Classes
- `FixedChunkAccessSectionBitBuffer` - Fixed-size 3D bit buffer for tracking chunk section status and caching chunk access objects
- `CheckAndCacheBlockChecker` - Pre-checks chunk sections using `hasAny()`, caches chunk access objects, supports lazy chunk loading
- `CommonBlockSearchesCheckAndCache` - Optimized `BlockPos.findClosestMatch` for sensor block searches
- `LithiumMoveToBlockGoal` - Interface for optimized block search in `MoveToTargetPosGoal`
- `NonPOISearchDistances` - Vanilla-compatible sort order calculations for chunk-aware search
- `GameEventListenerWithCallback` - Callback interface for vibration listener wakeup mechanism

#### New Mixin Packages (6)
- `ai.non_poi_block_search` (4 mixins)
- `world.block_entity_ticking.sleeping.sculk_sensor_shrieker` (5 mixins)

#### Files Changed
- 17 new Java source files
- 2 new package-info.java (MixinConfigOption)
- Updated `harium.mixins.json` (+9 mixins, total 258)
- Updated `harium-mixin-config.md` and `lithium-mixin-config.md` with new options

#### Config Options Added
- `mixin.ai.non_poi_block_search` (default: `true`) - Optimize Non-POI block search using hasAny pre-checking
- `mixin.world.block_entity_ticking.sleeping.sculk_sensor_shrieker` (default: `true`) - BlockEntity sleeping for inactive sculk sensors and shriekers

#### Not Backported (with reasons)
| Feature | Reason |
|---|---|
| Sprinting Particles | `canSpawnSprintParticle()` does not exist in MC 1.20.1; sprint particle logic is inline in `Entity.baseTick()` |
| Equipment Tracking (new) | MC 1.20.1 has no separate `EntityEquipment` class; the existing `skip_equipment_change_check` optimization already covers basic equipment tracking. The new Lithium version requires `ChangePublisher`/`ChangeSubscriber` infrastructure not present in 1.20.1. |
| Enchantment Ticking | MC 1.20.1 has no centralized `EnchantmentHelper.tickEffects()` method; enchantment effects (Soul Speed, Frost Walker) are scattered across `travel()` and `move()` methods, making the optimization inapplicable |

## [1.1.0] - 2026-04-21
### Backported from Lithium 0.24.2 (MC 1.21.x / NeoForge+Fabric) to Forge 1.20.1

#### New Dependencies
- Added [MixinExtras](https://github.com/LlamaLad7/MixinExtras) 0.2.2 (shaded into JAR) for `@WrapOperation` and `@Local` annotations

#### New Optimizations

**AI & Brain**
- **Useless Behaviors** - Filter out sentinel behaviors from being added to entity brains, preventing unnecessary task scheduling
- **Nitwit Job Search** - Remove the useless AcquirePoi task from nitwit villagers, who can never acquire a job site
- **Tempting Sensor** - Replace Stream-based player search with traditional iteration for better performance in mob tempting logic
- **Useless Sensors - Goat** - Disable the NEAREST_ITEMS sensor on goats since its memories are never used
- **Useless Sensors - Parent Animal** - Disable the NEAREST_ADULT sensor on adult animals and re-enable it dynamically when an entity becomes a baby again
- **Long Jump Weighted Choice** - Replace expensive weighted random selection in LongJumpTask (frogs, goats) with O(1) pre-computed LongJumpChoiceList using distance-squared bucketing

**Entity**
- **Projectile-Projectile Collisions** - Skip collision checks between projectiles that cannot collide with each other (e.g. ender pearls in stasis chambers) using EntityClassGroup-based filtering
- **Framed Maps** - Avoid O(N^2) inventory scanning when a map is placed in an item frame. Instead of iterating all player trackers, directly access the current player's tracker. Significant improvement on multiplayer servers with many maps.

**World**
- **Sculk Catalyst Sleeping** - Put sculk catalyst block entities to sleep when their SculkSpreadManager has no cursors to spread, reducing unnecessary tick overhead

#### New Utility Classes
- `LithiumEmptyBehavior` - Sentinel behavior used to mark and filter useless behaviors
- `SensorHelper` - Runtime sensor enable/disable without removing sensors from brain
- `ProjectileEntityClassGroup` - EntityClassGroup definitions for optimized and hittable projectile classification
- `LongJumpChoiceList` - Pre-computed weighted choice list with memoized templates for frog/goat jumps
- `BrainAccessor` / `SensorAccessor` - Mixin accessors for brain sensor map and sensor timing fields

#### New Mixin Packages (10)
- `ai.useless_behaviors` + `ai.useless_behaviors.nitwit_job_search`
- `ai.useless_sensors` + `ai.useless_sensors.goat_item_sensor` + `ai.useless_sensors.parent_animal_sensor`
- `ai.sensor.replace_streams.tempting`
- `ai.task.run.long_jump_weighted_choice`
- `entity.projectile_projectile_collisions`
- `entity.framed_maps`
- `world.block_entity_ticking.sleeping.sculk_catalyst`

#### Files Changed
- 25 new Java source files
- 10 new package-info.java (MixinConfigOption)
- Updated `harium.mixins.json`, config defaults, and dependencies
- Added MixinExtras 0.2.2 to `build.gradle` (shaded)

#### Not Backported (with reasons)
| Feature | Reason |
|---|---|
| Sprinting Particles | `canSpawnSprintParticle()` does not exist in MC 1.20.1; sprint particle logic is inline in `Entity.baseTick()` |
| Non-POI Block Search | Requires 9+ utility files, major Yarn mapping differences (`maybeHas` -> `hasAny`, `MoveToBlockGoal` -> `MoveToTargetPosGoal`, etc.) |
| Equipment Tracking | MC 1.20.1 has no separate `EntityEquipment` class; equipment is managed directly in `LivingEntity` |
| Enchantment Ticking | No `tickEnchantments()` method exists in 1.20.1; enchantment tick logic is inline in `LivingEntity` |
| Sculk Sensor Sleeping | Sensor uses vibration listener system with no standalone `tick()` method; sleeping would risk breaking vibration detection |
