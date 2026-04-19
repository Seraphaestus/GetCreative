package amaryllis.get_creative.generators.clockwork_motor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class ClockworkMotorRenderer extends KineticBlockEntityRenderer<ClockworkMotorBlockEntity> {

    public ClockworkMotorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(ClockworkMotorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        Direction direction = ClockworkMotorBlock.getInputDirection(be.getBlockState());
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        SuperByteBuffer mechanism = CachedBuffers.partialFacingVertical(ClockworkMotorBlock.MODEL, be.getBlockState(), direction);
        SuperByteBuffer halfShaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction.getOpposite());

        final var axis = ((IRotate) be.getBlockState().getBlock()).getRotationAxis(be.getBlockState());
        float angle = getAngleForBe(be, be.getBlockPos(), axis);

        BlockEntity source = be.getLevel().getBlockEntity(be.getBlockPos().relative(direction));
        float mechanismAngle = (source instanceof HandCrankBlockEntity crank) ? crank.getIndependentAngle(partialTicks) : angle;

        kineticRotationTransform(mechanism, be, direction.getAxis(), mechanismAngle, light).renderInto(ms, vb);
        kineticRotationTransform(halfShaft, be, direction.getAxis(), angle, light).renderInto(ms, vb);
    }

}
