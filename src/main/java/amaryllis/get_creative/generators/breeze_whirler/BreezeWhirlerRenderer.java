package amaryllis.get_creative.generators.breeze_whirler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BreezeWhirlerRenderer<T extends BreezeWhirlerBlockEntity> extends KineticBlockEntityRenderer<T> {

    public BreezeWhirlerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(BreezeWhirlerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        // Render the wind via the Renderer even when Visualization is enabled, because I don't know how to do the uv scrolling otherwise

        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        final var axis = be.getBlockState().getValue(BreezeWhirlerBlock.AXIS);
        final var positiveAxis = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        Vector3f headPos = getHeadPos(be, axis, time);

        if (!BreezeWhirlerBlockEntity.supportsVisualization() || !VisualizationManager.supportsVisualization(be.getLevel())) {
            // Draw gear
            SuperByteBuffer gear = CachedBuffers.partialFacingVertical(BreezeWhirlerBlock.GEAR_MODEL, be.getBlockState(), positiveAxis);
            standardKineticRotationTransform(gear, be, light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

            // Draw Breeze
            float facingAngle = AngleHelper.rad(be.headAngle.getValue(partialTicks));

            ms.pushPose();

            SuperByteBuffer breezeHead = CachedBuffers.partial(BreezeWhirlerBlock.BREEZE_MODEL, be.getBlockState());
            breezeHead.translate(headPos.x, headPos.y, headPos.z);

            if (axis == Direction.Axis.Y) {
                breezeHead.translate(0, be.isPlayerAbove ? -0.0625f : 0.0625f, 0);
                breezeHead.rotateCentered(new Quaternionf().rotationYXZ(facingAngle, AngleHelper.rad(be.isPlayerAbove ? 45 : -45), 0));
            } else {
                breezeHead.rotateYCentered(facingAngle);
            }

            breezeHead.light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

            ms.popPose();
        }

        // Draw wind
        double uScroll = (1 - ((time * 0.01f) % 1f)) / 16f;

        ms.pushPose();

        SuperByteBuffer wind = CachedBuffers.partialFacing(BreezeWhirlerBlock.BREEZE_WIND, be.getBlockState(), positiveAxis);
        float windT = time * (float)Math.PI * -0.1f;
        float windX = Mth.cos(windT) * 0.0375f;
        float windZ = Mth.sin(windT) * 0.0375f;
        wind.translate(windX, headPos.y, windZ);
        wind.light(LightTexture.FULL_BRIGHT)
                .shiftUVScrolling(BreezeWhirlerBlock.WIND_SPRITE_SHIFT, (float)uScroll, 0)
                .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

        ms.popPose();
    }

    public static Vector3f getHeadPos(BreezeWhirlerBlockEntity be, Direction.Axis axis, float time) {
        float renderTick = time + (be.hashCode() % 13) * 16f;
        float headX = 0, headZ = 0;
        float headY = Mth.sin((float) ((renderTick / 8f) % (2 * Math.PI))) / 16f;
        if (axis == Direction.Axis.Y) headY *= 0.25f;

        // Offset head backwards by a pixel, so it doesn't clip through the bars
        if (axis == Direction.Axis.X) {
            headX -= 0.0625f * Math.signum((float) Minecraft.getInstance().player.getX() - be.getBlockPos().getX());
        } else if (axis == Direction.Axis.Z) {
            headZ -= 0.0625f * Math.signum((float) Minecraft.getInstance().player.getZ() - be.getBlockPos().getZ());
        }

        // Oscillate
        float animationTime = time * (float)Math.PI * -0.1f;
        headX += Mth.cos(animationTime) * 0.0375f;
        headZ += Mth.sin(animationTime) * 0.0375f;

        return new Vector3f(headX, headY, headZ);
    }
}
