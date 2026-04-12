package amaryllis.get_creative.precision_assembly;

import amaryllis.get_creative.industrial_fan.IndustrialFanBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class ArmAssemblyCategory extends CreateRecipeCategory<ArmAssemblyRecipe> {

    private final AnimatedMechanicalArm arm = new AnimatedMechanicalArm();

    public ArmAssemblyCategory(Info<ArmAssemblyRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArmAssemblyRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 51)
               .setBackground(getRenderedSlot(), -1, -1)
               .addIngredients(recipe.getIngredients().getFirst());

        List<ProcessingOutput> results = recipe.getRollableResults();
        for (int i = 0; i < results.size(); i++) {
            var output = results.get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 131 + 19 * i, 50)
                    .setBackground(getRenderedSlot(output), -1, -1)
                    .addItemStack(output.getStack())
                    .addRichTooltipCallback(addStochasticTooltip(output));
        }
    }

    @Override
    public void draw(ArmAssemblyRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        var matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(56, 53, 0);
        matrixStack.scale(1.15f, 1.35f, 0);
        AllGuiTextures.JEI_SHADOW.render(graphics, 0, 0);
        matrixStack.popPose();
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29 + (recipe.getRollableResults().size() > 2 ? -19 : 0));
        arm.draw(graphics, getBackground().getWidth() / 2 - 13, 22);
    }

    // Recipe Viewer for Sequenced Assembly
    public static class RecipeViewerCategory extends SequencedAssemblySubCategory {

        AnimatedMechanicalArm arm = new AnimatedMechanicalArm();

        public RecipeViewerCategory() { super(25); }

        @Override
        public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
            PoseStack ms = graphics.pose();
            arm.offset = index;
            ms.pushPose();
            ms.translate(-7, 50, 0);
            ms.scale(.75f, .75f, .75f);
            arm.draw(graphics, getWidth() / 2, 0);
            ms.popPose();
        }

    }

    // Animated Mechanical Arm for Recipe Viewers
    public static class AnimatedMechanicalArm extends AnimatedKinetics {

        @Override
        public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
            PoseStack matrixStack = graphics.pose();
            matrixStack.pushPose();
            matrixStack.translate(xOffset, yOffset, 100);
            matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
            matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
            int scale = 20;

            blockElement(AllPartialModels.ARM_COG)
                    .rotateBlock(0, getCurrentAngle(), 0)
                    .atLocal(0, 2, -1)
                    .scale(scale)
                    .render(graphics);

            blockElement(AllBlocks.MECHANICAL_ARM.getDefaultState())
                    .rotateBlock(0, 180, 0)
                    .atLocal(0, 2, -1)
                    .scale(scale)
                    .render(graphics);

            final float time = AnimationTickHolder.getRenderTime();
            double t = smoothstep2(pSin(time / 16f));
            t = 0.5 + (t - 0.5) * (t - 0.5) * Math.signum(t - 0.5); // Sharpen mid-pause
            final double rotation = -45 + 75 * t; // Slightly offset to account for viewing angle

            blockElement(IndustrialFanBlock.MECHANICAL_ARM_MODEL)
                    .rotateBlock(0, 180 + rotation, 0)
                    .atLocal(0, 1, -1)
                    .scale(scale)
                    .render(graphics);

            blockElement(AllBlocks.DEPOT.getDefaultState())
                    .atLocal(0, 2, 0)
                    .scale(scale)
                    .render(graphics);

            matrixStack.popPose();
        }

        protected double pSin(double x) {
            return Math.sin(x) * 0.5 + 0.5;
        }
        protected double smoothstep2(double x) {
            return (x < 0.5)
                    ? smoothstep(0, 0.5, x) * 0.5
                    : smoothstep(0.5, 1, x) * 0.5 + 0.5;
        }
        protected double smoothstep(double edge0, double edge1, double x) {
            x = Math.clamp((x - edge0) / (edge1 - edge0), 0, 1);
            return x * x * (3.0f - 2.0f * x);
        }

    }


}