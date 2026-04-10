package amaryllis.get_creative.generators.haunted_cogwheel;

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

public class HauntedCogwheelRenderer extends KineticBlockEntityRenderer<HauntedCogwheelBlockEntity>  {

    public HauntedCogwheelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(HauntedCogwheelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        Direction facing = be.getBlockState().getValue(FACING);
        SuperByteBuffer cog = CachedBuffers.partialFacingVertical(HauntedCogwheelBlock.MODEL, be.getBlockState(), facing);
        standardKineticRotationTransform(cog, be, light).renderInto(ms, vb);
    }
}
