package amaryllis.get_creative.generators.clockwork_motor.wind_up_key;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class WindUpKeyRenderer extends KineticBlockEntityRenderer<WindUpKeyBlockEntity> {

    public WindUpKeyRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(WindUpKeyBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        VertexConsumer vb = buffer.getBuffer(RenderType.cutout());

        Direction facing = be.getBlockState().getValue(FACING);
        SuperByteBuffer key = CachedBuffers.partialFacingVertical(WindUpKeyBlock.MODEL, be.getBlockState(), facing);
        kineticRotationTransform(key, be, facing.getAxis(), be.getIndependentAngle(partialTicks), light).renderInto(ms, vb);
    }

}
