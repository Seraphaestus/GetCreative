package amaryllis.get_creative.appliances.gramophone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.turntable.TurntableBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;

public class GramophoneRenderer extends SafeBlockEntityRenderer<GramophoneBlockEntity> {

    public GramophoneRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(GramophoneBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        // TODO if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        // Render static jukebox (on rotations)
        float rotation = be.getBlockState().getValue(GramophoneBlock.ROTATION_8) * -45;
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        SuperByteBuffer model = CachedBuffers.partial(GramophoneBlock.MODEL, be.getBlockState());
        model.light(light)
            .rotateCenteredDegrees(rotation, Direction.Axis.Y)
            .renderInto(ms, vb);


        // Render record disc
        TurntableBlockEntity turnTable = be.getTurntable();
        if (turnTable == null) return;

        float angle = KineticBlockEntityRenderer.getAngleForBe(turnTable, turnTable.getBlockPos(), Direction.Axis.Y);

        var msr = TransformStack.of(ms);
        ms.pushPose();
        ms.translate(0.5, -0.475, 0.5);
        ms.scale(0.8f, 1f, 0.8f);
        msr.rotateY(angle);
        msr.rotateXDegrees(90);

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel bakedModel = itemRenderer.getModel(be.record, null, null, 0);
        itemRenderer.render(be.record, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, bakedModel);

        ms.popPose();
    }

}
