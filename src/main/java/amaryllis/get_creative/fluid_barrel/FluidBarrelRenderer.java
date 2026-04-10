package amaryllis.get_creative.fluid_barrel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.platform.services.ModFluidHelper;
import net.createmod.catnip.render.FluidRenderHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import static net.createmod.catnip.render.FluidRenderHelper.renderStillTiledFace;
import static amaryllis.get_creative.fluid_barrel.FluidBarrelBlockEntity.FLOOR_HEIGHT;
import static amaryllis.get_creative.fluid_barrel.FluidBarrelBlockEntity.TOP_MARGIN;
import static amaryllis.get_creative.fluid_barrel.FluidBarrelBlockEntity.HULL_WIDTH;
import static amaryllis.get_creative.fluid_barrel.FluidBarrelBlockEntity.MIN_PUDDLE_HEIGHT;

public class FluidBarrelRenderer extends SafeBlockEntityRenderer<FluidBarrelBlockEntity> {

    public FluidBarrelRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(FluidBarrelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!be.isController()) return;

        float[] yBounds = be.getFluidRenderY(partialTicks, true);
        if (yBounds == null) return;
        FluidStack fluidStack = be.tankInventory.getFluid();
        float totalHeight = be.height - FLOOR_HEIGHT - TOP_MARGIN - MIN_PUDDLE_HEIGHT;
        float fluidHeight = yBounds[1] - yBounds[0];

        float xMin = HULL_WIDTH, xMax = xMin + be.width - 2 * HULL_WIDTH;
        float zMin = HULL_WIDTH, zMax = zMin + be.width - 2 * HULL_WIDTH;

        ms.pushPose();
        ms.translate(0, fluidHeight - totalHeight, 0);

        // Render fluid manually
        ModFluidHelper<FluidStack> helper = (ModFluidHelper<FluidStack>) CatnipServices.FLUID_HELPER;
        VertexConsumer builder = FluidRenderHelper.getFluidBuilder(buffer);
        TextureAtlasSprite fluidTexture = helper.getStillTextureOrMissing(fluidStack);

        int color = helper.getColor(fluidStack, null, null);
        int blockLightIn = (light >> 4) & 0xF;
        int luminosity = Math.max(blockLightIn, helper.getLuminosity(fluidStack));
        light = (light & 0xF00000) | luminosity << 4;

        if (fluidStack.getFluid().getFluidType().isLighterThanAir()) {
            Vec3 center = new Vec3(xMin + (xMax - xMin) / 2, yBounds[0] + fluidHeight / 2, zMin + (zMax - zMin) / 2);
            ms.translate(center.x, center.y, center.z);
            ms.mulPose(Axis.XP.rotationDegrees(180));
            ms.translate(-center.x, -center.y, -center.z);
        }

        // Render upwards-facing top face
        renderStillTiledFace(Direction.UP, xMin, zMin, xMax, zMax, yBounds[1], builder, ms, light, color, fluidTexture);
        // Render backfaces only if the camera position is below the surface level
        var player = Minecraft.getInstance().player;
        float topY = be.getBlockPos().getY() + be.height - yBounds[0] + FLOOR_HEIGHT;
        if (player != null && player.getEyeY() < topY) {
            int transColor = new Color(color).setAlpha(0.5f).getRGB();
            // Render downwards-facing top backface
            renderStillTiledFace(Direction.DOWN, xMin, zMin, xMax, zMax, yBounds[1], builder, ms, light, color, fluidTexture);
            // Render upwards-facing bottom backface
            renderStillTiledFace(Direction.UP, xMin, zMin, xMax, zMax, yBounds[0], builder, ms, light, transColor, fluidTexture);
            // Render inwards-facing side backfaces
            renderStillTiledFace(Direction.EAST, zMin, yBounds[0], zMax, yBounds[1], xMin, builder, ms, light, transColor, fluidTexture);
            renderStillTiledFace(Direction.WEST, zMin, yBounds[0], zMax, yBounds[1], xMax, builder, ms, light, transColor, fluidTexture);
            renderStillTiledFace(Direction.SOUTH, xMin, yBounds[0], xMax, yBounds[1], zMin, builder, ms, light, transColor, fluidTexture);
            renderStillTiledFace(Direction.NORTH, xMin, yBounds[0], xMax, yBounds[1], zMax, builder, ms, light, transColor, fluidTexture);
        }

        ms.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(FluidBarrelBlockEntity be) {
        return be.isController();
    }
}
