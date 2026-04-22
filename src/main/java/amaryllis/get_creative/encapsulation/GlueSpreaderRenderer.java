package amaryllis.get_creative.encapsulation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import static amaryllis.get_creative.encapsulation.GlueSpreaderBlock.SIDE_MODEL_OPEN;
import static amaryllis.get_creative.encapsulation.GlueSpreaderBlock.SIDE_MODEL_SHUT;

public class GlueSpreaderRenderer extends SafeBlockEntityRenderer<GlueSpreaderBlockEntity> {

    protected static final Direction[] STARTING_SIDE = new Direction[]{
        Direction.SOUTH, Direction.NORTH, // Down/Up
        Direction.UP,  Direction.UP,    // North/South
        Direction.UP, Direction.UP  // West/East
    };

    public GlueSpreaderRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(GlueSpreaderBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        // TODO if (VisualizationManager.supportsVisualization(be.getLevel())) return;
        
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(GlueSpreaderBlock.FACING);
        Direction.Axis facingAxis = facing.getAxis();

        VertexConsumer vb = buffer.getBuffer(RenderType.cutout());

        boolean[] spreaderAdjacency = new boolean[4];
        int adjCount = 0;
        Direction direction = STARTING_SIDE[facing.ordinal()];
        for (int i = 0; i < 4; i++) {
            spreaderAdjacency[i] = be.isConnectedToSpreader(direction);
            if (spreaderAdjacency[i]) adjCount += 1;
            direction = direction.getCounterClockWise(facingAxis);
        }

        boolean linearAcross = (spreaderAdjacency[0] == spreaderAdjacency[2]) && (spreaderAdjacency[1] == spreaderAdjacency[3]) && (spreaderAdjacency[0] != spreaderAdjacency[1]);
        boolean forceOppositeOpen = (adjCount == 1) || linearAcross;

        direction = STARTING_SIDE[facing.ordinal()];
        for (int i = 0; i < 4; i++) {
            boolean hasOpenSide = (adjCount == 0) || spreaderAdjacency[i];
            if (forceOppositeOpen && spreaderAdjacency[(i + 2) % 4]) hasOpenSide = true;

            PartialModel sideModel = hasOpenSide ? SIDE_MODEL_OPEN : SIDE_MODEL_SHUT;
            SuperByteBuffer side = CachedBuffers.partialFacingVertical(sideModel, state, facing);
            if (i > 0) side.rotateCenteredDegrees(i * 90, facingAxis);
            side.light(light).renderInto(ms, vb);
            
            direction = direction.getCounterClockWise(facingAxis);
        }

        SuperByteBuffer head = CachedBuffers.partialFacingVertical(AllPartialModels.STICKER_HEAD, state, facing);
        float offset = be.piston.getValue(AnimationTickHolder.getPartialTicks(be.getLevel()));
        offset *= offset * 4 / 16f;
        head.translate(facing.step().mul(offset));
        head.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

}
