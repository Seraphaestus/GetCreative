package amaryllis.get_creative.mixin.precision_assembly;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.recipes.precision_assembly.ArmAssemblyCategory;
import amaryllis.get_creative.recipes.precision_assembly.ArmAssemblyRecipe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.SequencedAssemblyCategory;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SequencedAssemblyCategory.class)
public class SequencedAssemblyCategoryMixin extends CreateRecipeCategory<SequencedAssemblyRecipe> {

    public SequencedAssemblyCategoryMixin(Info<SequencedAssemblyRecipe> info) {
        super(info);
    }

    @ModifyReturnValue(method = "getTooltipStrings", at = @At(value = "RETURN", ordinal = 2))
    private List<Component> insertExtraTooltip(List<Component> tooltip,
                SequencedAssemblyRecipe recipe, IRecipeSlotsView iRecipeSlotsView, double mouseX, double mouseY)
    {
        var step = getCreative$getSequencedRecipe(recipe, mouseX);
        var category = (step != null) ? getSubCategory(step) : null;
        final int extraColor = 0x007700;
        // Append "with Deployer or Mech. Arm" to Deploying steps (if Mech. Arm can handle)
        if (category instanceof SequencedAssemblySubCategory.AssemblyDeploying && Config.MECHANICAL_ARMS_HANDLE_SEQUENCED_DEPLOYING_RECIPES.isTrue()) {
            tooltip.add(Component.translatable("get_creative.recipe.assembly.with_deployer_or_arm").withColor(extraColor));
        }
        // Append "with Mechanical Arm" to Precision Assembly steps (with a held item)
        else if (category instanceof ArmAssemblyCategory.SequencedAssembly
                && step.getAsAssemblyRecipe() instanceof ArmAssemblyRecipe armRecipe
                && !armRecipe.getRequiredHeldItem().isEmpty())
        {
            tooltip.add(Component.translatable("get_creative.recipe.assembly.with_arm").withColor(extraColor));
        }
        return tooltip;
    }

    private SequencedRecipe<?> getCreative$getSequencedRecipe(SequencedAssemblyRecipe recipe, double mouseX) {
        final int margin = 3;

        int width = -margin;
        for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence())
            width += getSubCategory(sequencedRecipe).getWidth() + margin;

        final int xOffset = width / 2 + getBackground().getWidth() / -2;
        double relativeX = mouseX + xOffset;

        for (SequencedRecipe<?> sequencedRecipe: recipe.getSequence()) {
            SequencedAssemblySubCategory subCategory = getSubCategory(sequencedRecipe);
            if (relativeX >= 0 && relativeX < subCategory.getWidth()) return sequencedRecipe;
            relativeX -= subCategory.getWidth() + margin;
        }
        return null;
    }

    @Shadow SequencedAssemblySubCategory getSubCategory(SequencedRecipe<?> sequencedRecipe) { return null; }
    @Shadow protected void setRecipe(IRecipeLayoutBuilder builder, SequencedAssemblyRecipe recipe, IFocusGroup focuses) {}
    @Shadow protected void draw(SequencedAssemblyRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY) {}
}
