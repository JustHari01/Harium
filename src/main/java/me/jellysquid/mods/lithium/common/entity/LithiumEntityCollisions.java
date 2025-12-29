package me.jellysquid.mods.lithium.common.entity;

import com.google.common.collect.AbstractIterator;
import me.jellysquid.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper;
import me.jellysquid.mods.lithium.common.util.Pos;
import me.jellysquid.mods.lithium.common.world.WorldHelper;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.CollisionView;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class LithiumEntityCollisions {
    public static final double EPSILON = 1.0E-7D;

    public static List<VoxelShape> getBlockCollisions(World world, Entity entity, Box box) {
        return new ChunkAwareBlockCollisionSweeper(world, entity, box).collectAll();
    }

    public static boolean doesBoxCollideWithBlocks(World world, Entity entity, Box box) {
        final ChunkAwareBlockCollisionSweeper sweeper = new ChunkAwareBlockCollisionSweeper(world, entity, box);

        final VoxelShape shape = sweeper.computeNext();

        return shape != null && !shape.isEmpty();
    }

    public static boolean doesBoxCollideWithHardEntities(EntityView view, Entity entity, Box box) {
        if (isBoxEmpty(box)) {
            return false;
        }

        return getEntityWorldBorderCollisionIterable(view, entity, box.expand(EPSILON), false).iterator().hasNext();
    }

    public static List<VoxelShape> getEntityWorldBorderCollisions(World world, Entity entity, Box box, boolean includeWorldBorder) {
        if (isBoxEmpty(box)) {
            return Collections.emptyList();
        }
        ArrayList<VoxelShape> shapes = new ArrayList<>();
        Iterable<VoxelShape> collisions = getEntityWorldBorderCollisionIterable(world, entity, box.expand(EPSILON), includeWorldBorder);
        for (VoxelShape shape : collisions) {
            shapes.add(shape);
        }
        return shapes;
    }

    public static Iterable<VoxelShape> getEntityWorldBorderCollisionIterable(EntityView view, Entity entity, Box box, boolean includeWorldBorder) {
        assert !includeWorldBorder || entity != null;
        return new Iterable<>() {
            private List<Entity> entityList;
            private int nextFilterIndex;

            @NotNull
            @Override
            public Iterator<VoxelShape> iterator() {
                return new AbstractIterator<>() {
                    int index = 0;
                    boolean consumedWorldBorder = false;

                    @Override
                    protected VoxelShape computeNext() {

                        if (entityList == null) {
                            
                            entityList = WorldHelper.getEntitiesForCollision(view, box, entity);
                            nextFilterIndex = 0;
                        }
                        List<Entity> list = entityList;
                        Entity otherEntity;
                        do {
                            if (this.index >= list.size()) {

                                if (includeWorldBorder && !this.consumedWorldBorder) {
                                    this.consumedWorldBorder = true;
                                    WorldBorder worldBorder = entity.getWorld().getWorldBorder();
                                    if (!isWithinWorldBorder(worldBorder, box) && isWithinWorldBorder(worldBorder, entity.getBoundingBox())) {
                                        return worldBorder.asVoxelShape();
                                    }
                                }
                                return this.endOfData();
                            }

                            otherEntity = list.get(this.index);
                            if (this.index >= nextFilterIndex) {
                                
                                if (entity == null) {
                                    if (!otherEntity.isCollidable()) {
                                        otherEntity = null;
                                    }
                                } else if (!entity.collidesWith(otherEntity)) {
                                    otherEntity = null;
                                }
                                nextFilterIndex++;
                            }
                            this.index++;
                        } while (otherEntity == null);

                        return VoxelShapes.cuboid(otherEntity.getBoundingBox());
                    }
                };
            }
        };
    }

    public static boolean isWithinWorldBorder(WorldBorder border, Box box) {
        double wboxMinX = Math.floor(border.getBoundWest());
        double wboxMinZ = Math.floor(border.getBoundNorth());

        double wboxMaxX = Math.ceil(border.getBoundEast());
        double wboxMaxZ = Math.ceil(border.getBoundSouth());

        return box.minX >= wboxMinX && box.minX <= wboxMaxX && box.minZ >= wboxMinZ && box.minZ <= wboxMaxZ &&
                box.maxX >= wboxMinX && box.maxX <= wboxMaxX && box.maxZ >= wboxMinZ && box.maxZ <= wboxMaxZ;
    }

    private static boolean isBoxEmpty(Box box) {
        return box.getAverageSideLength() <= EPSILON;
    }

    public static boolean doesEntityCollideWithWorldBorder(CollisionView collisionView, Entity entity) {
        if (isWithinWorldBorder(collisionView.getWorldBorder(), entity.getBoundingBox())) {
            return false;
        } else {
            VoxelShape worldBorderShape = getWorldBorderCollision(collisionView, entity);
            return worldBorderShape != null && VoxelShapes.matchesAnywhere(worldBorderShape, VoxelShapes.cuboid(entity.getBoundingBox()), BooleanBiFunction.AND);
        }
    }

    public static VoxelShape getWorldBorderCollision(CollisionView collisionView, Entity entity) {
        Box box = entity.getBoundingBox();
        WorldBorder worldBorder = collisionView.getWorldBorder();
        return worldBorder.canCollide(entity, box) ? worldBorder.asVoxelShape() : null;
    }

    public static VoxelShape getCollisionShapeBelowEntity(World world, @Nullable Entity entity, Box entityBoundingBox) {
        int x = MathHelper.floor(entityBoundingBox.minX + (entityBoundingBox.maxX - entityBoundingBox.minX) / 2);
        int y = MathHelper.floor(entityBoundingBox.minY);
        int z = MathHelper.floor(entityBoundingBox.minZ + (entityBoundingBox.maxZ - entityBoundingBox.minZ) / 2);
        if (world.isOutOfHeightLimit(y)) {
            return null;
        }
        Chunk chunk = world.getChunk(Pos.ChunkCoord.fromBlockCoord(x), Pos.ChunkCoord.fromBlockCoord(z), ChunkStatus.FULL, false);
        if (chunk != null) {
            ChunkSection cachedChunkSection = chunk.getSectionArray()[Pos.SectionYIndex.fromBlockCoord(world, y)];
            return cachedChunkSection.getBlockState(x & 15, y & 15, z & 15).getCollisionShape(world, new BlockPos(x, y, z), entity == null ? ShapeContext.absent() : ShapeContext.of(entity));
        }
        return null;
    }
}
