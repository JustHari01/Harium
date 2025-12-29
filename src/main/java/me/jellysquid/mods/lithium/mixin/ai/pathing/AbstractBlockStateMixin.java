package me.jellysquid.mods.lithium.mixin.ai.pathing;

import me.jellysquid.mods.lithium.common.ai.pathing.BlockStatePathingCache;
import me.jellysquid.mods.lithium.common.world.blockview.SingleBlockBlockView;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin implements BlockStatePathingCache {
    private PathNodeType pathNodeType = null;
    private PathNodeType pathNodeTypeNeighbor = null;

    @Inject(method = "initShapeCache()V", at = @At("RETURN"))
    private void init(CallbackInfo ci) {

        this.pathNodeType = null;
        this.pathNodeTypeNeighbor = null;

        BlockState state = this.asBlockState();

        SingleBlockBlockView blockView = SingleBlockBlockView.of(state, BlockPos.ORIGIN);
        try {
            this.pathNodeType = Validate.notNull(LandPathNodeMaker.getCommonNodeType(blockView, BlockPos.ORIGIN));
        } catch (SingleBlockBlockView.SingleBlockViewException | ClassCastException e) {

            this.pathNodeType = null;
        }
        try {

            this.pathNodeTypeNeighbor = (LandPathNodeMaker.getNodeTypeFromNeighbors(blockView, BlockPos.ORIGIN.mutableCopy(), null));
            if (this.pathNodeTypeNeighbor == null) {
                this.pathNodeTypeNeighbor = PathNodeType.OPEN;
            }
        } catch (SingleBlockBlockView.SingleBlockViewException | ClassCastException e) {
            this.pathNodeTypeNeighbor = null;
        }
    }

    @Override
    public PathNodeType getPathNodeType() {
        return this.pathNodeType;
    }

    @Override
    public PathNodeType getNeighborPathNodeType() {
        return this.pathNodeTypeNeighbor;
    }

    @Shadow
    protected abstract BlockState asBlockState();

    @Shadow
    public abstract Block getBlock();
}
