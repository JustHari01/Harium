package me.jellysquid.mods.lithium.mixin.world.explosions;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.lithium.common.util.Pos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow
    @Final
    private float power;

    @Shadow
    @Final
    private double x;

    @Shadow
    @Final
    private double y;

    @Shadow
    @Final
    private double z;

    @Shadow
    @Final
    private World world;

    @Shadow
    @Final
    private ExplosionBehavior behavior;
    @Shadow
    @Final
    private boolean createFire;
    private final BlockPos.Mutable cachedPos = new BlockPos.Mutable();

    private int prevChunkX = Integer.MIN_VALUE;
    private int prevChunkZ = Integer.MIN_VALUE;

    private Chunk prevChunk;

    private boolean explodeAirBlocks;

    private int minY, maxY;

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;DDDFZLnet/minecraft/world/explosion/Explosion$DestructionType;)V", at = @At("TAIL"))
    private void init(World world, Entity entity, DamageSource damageSource, ExplosionBehavior explosionBehavior,
            double d, double e, double f, float g, boolean bl, Explosion.DestructionType destructionType,
            CallbackInfo ci) {
        this.minY = this.world.getBottomY();
        this.maxY = this.world.getTopY();

        boolean explodeAir = this.createFire;
        if (!explodeAir && this.world.getRegistryKey() == World.END
                && this.world.getDimensionEntry().matchesKey(DimensionTypes.THE_END)) {
            float overestimatedExplosionRange = (8 + (int) (6f * this.power));
            int endPortalX = 0;
            int endPortalZ = 0;
            if (overestimatedExplosionRange > Math.abs(this.x - endPortalX)
                    && overestimatedExplosionRange > Math.abs(this.z - endPortalZ)) {
                explodeAir = true;
            }
        }
        this.explodeAirBlocks = explodeAir;
    }

    @Redirect(method = "collectBlocksAndDamageEntities()V", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;", remap = false))
    public HashSet<BlockPos> skipNewHashSet() {
        return null;
    }

    @ModifyConstant(method = "collectBlocksAndDamageEntities()V", constant = @Constant(intValue = 16, ordinal = 1))
    public int skipLoop(int prevValue) {
        return 0;
    }

    @Redirect(method = "collectBlocksAndDamageEntities()V", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;addAll(Ljava/util/Collection;)Z", remap = false))
    public boolean collectBlocks(ObjectArrayList<BlockPos> affectedBlocks, Collection<BlockPos> collection) {

        final LongOpenHashSet touched = new LongOpenHashSet(0);

        final Random random = this.world.random;

        for (int rayX = 0; rayX < 16; ++rayX) {
            boolean xPlane = rayX == 0 || rayX == 15;
            double vecX = (((float) rayX / 15.0F) * 2.0F) - 1.0F;

            for (int rayY = 0; rayY < 16; ++rayY) {
                boolean yPlane = rayY == 0 || rayY == 15;
                double vecY = (((float) rayY / 15.0F) * 2.0F) - 1.0F;

                for (int rayZ = 0; rayZ < 16; ++rayZ) {
                    boolean zPlane = rayZ == 0 || rayZ == 15;

                    if (xPlane || yPlane || zPlane) {
                        double vecZ = (((float) rayZ / 15.0F) * 2.0F) - 1.0F;

                        this.performRayCast(random, vecX, vecY, vecZ, touched);
                    }
                }
            }
        }

        LongIterator it = touched.iterator();

        boolean added = false;
        while (it.hasNext()) {
            added |= affectedBlocks.add(BlockPos.fromLong(it.nextLong()));
        }
        return added;
    }

    private void performRayCast(Random random, double vecX, double vecY, double vecZ, LongOpenHashSet touched) {
        double dist = Math.sqrt((vecX * vecX) + (vecY * vecY) + (vecZ * vecZ));

        double normX = (vecX / dist) * 0.3D;
        double normY = (vecY / dist) * 0.3D;
        double normZ = (vecZ / dist) * 0.3D;

        float strength = this.power * (0.7F + (random.nextFloat() * 0.6F));

        double stepX = this.x;
        double stepY = this.y;
        double stepZ = this.z;

        int prevX = Integer.MIN_VALUE;
        int prevY = Integer.MIN_VALUE;
        int prevZ = Integer.MIN_VALUE;

        float prevResistance = 0.0F;

        int boundMinY = this.minY;
        int boundMaxY = this.maxY;

        while (strength > 0.0F) {
            int blockX = MathHelper.floor(stepX);
            int blockY = MathHelper.floor(stepY);
            int blockZ = MathHelper.floor(stepZ);

            float resistance;

            if (prevX != blockX || prevY != blockY || prevZ != blockZ) {
                if (blockY < boundMinY || blockY >= boundMaxY || blockX < -30000000 || blockZ < -30000000
                        || blockX >= 30000000 || blockZ >= 30000000) {
                    return;
                }
                resistance = this.traverseBlock(strength, blockX, blockY, blockZ, touched);

                prevX = blockX;
                prevY = blockY;
                prevZ = blockZ;

                prevResistance = resistance;
            } else {
                resistance = prevResistance;
            }

            strength -= resistance;

            strength -= 0.22500001F;

            stepX += normX;
            stepY += normY;
            stepZ += normZ;
        }
    }

    private float traverseBlock(float strength, int blockX, int blockY, int blockZ, LongOpenHashSet touched) {
        BlockPos pos = this.cachedPos.set(blockX, blockY, blockZ);

        if (this.world.isOutOfHeightLimit(blockY)) {
            Optional<Float> blastResistance = this.behavior.getBlastResistance((Explosion) (Object) this, this.world,
                    pos, Blocks.AIR.getDefaultState(), Fluids.EMPTY.getDefaultState());

            if (blastResistance.isPresent()) {
                return (blastResistance.get() + 0.3F) * 0.3F;
            }
            return 0.0F;
        }

        int chunkX = Pos.ChunkCoord.fromBlockCoord(blockX);
        int chunkZ = Pos.ChunkCoord.fromBlockCoord(blockZ);

        if (this.prevChunkX != chunkX || this.prevChunkZ != chunkZ) {
            this.prevChunk = this.world.getChunk(chunkX, chunkZ);

            this.prevChunkX = chunkX;
            this.prevChunkZ = chunkZ;
        }

        final Chunk chunk = this.prevChunk;

        BlockState blockState = Blocks.AIR.getDefaultState();
        float totalResistance = 0.0F;
        Optional<Float> blastResistance;

        labelGetBlastResistance: {

            if (chunk != null) {

                ChunkSection section = chunk.getSectionArray()[Pos.SectionYIndex.fromBlockCoord(chunk, blockY)];

                if (section != null && !section.isEmpty()) {

                    blockState = section.getBlockState(blockX & 15, blockY & 15, blockZ & 15);

                    if (blockState.getBlock() != Blocks.AIR) {

                        FluidState fluidState = blockState.getFluidState();

                        blastResistance = this.behavior.getBlastResistance((Explosion) (Object) this, this.world, pos,
                                blockState, fluidState);
                        break labelGetBlastResistance;
                    }
                }
            }
            blastResistance = this.behavior.getBlastResistance((Explosion) (Object) this, this.world, pos,
                    Blocks.AIR.getDefaultState(), Fluids.EMPTY.getDefaultState());
        }

        if (blastResistance.isPresent()) {
            totalResistance = (blastResistance.get() + 0.3F) * 0.3F;
        }

        float reducedStrength = strength - totalResistance;
        if (reducedStrength > 0.0F && (this.explodeAirBlocks || !blockState.isAir())) {
            if (this.behavior.canDestroyBlock((Explosion) (Object) this, this.world, pos, blockState,
                    reducedStrength)) {
                touched.add(pos.asLong());
            }
        }

        return totalResistance;
    }

}
