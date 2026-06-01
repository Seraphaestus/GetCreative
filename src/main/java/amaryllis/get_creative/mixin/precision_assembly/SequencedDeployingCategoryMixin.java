package amaryllis.get_creative.mixin.precision_assembly;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.recipes.precision_assembly.ArmAssemblyCategory.AnimatedMechanicalArm;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SequencedAssemblySubCategory.AssemblyDeploying.class)
public class SequencedDeployingCategoryMixin extends SequencedAssemblySubCategory {

    private AnimatedMechanicalArm getCreative$arm = new AnimatedMechanicalArm();

    public SequencedDeployingCategoryMixin(int width) { super(width); }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initArm(CallbackInfo cbi) {
        getCreative$arm = new AnimatedMechanicalArm();
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index, CallbackInfo cbi) {
        if (!Config.shouldDisplayMechArmForSequencedAssemblyStep(index)) return;

        PoseStack ms = graphics.pose();
        getCreative$arm.offset = index;
        ms.pushPose();
        ms.translate(-7, 50, 0);
        ms.scale(.75f, .75f, .75f);
        getCreative$arm.draw(graphics, getWidth() / 2, 0);
        ms.popPose();

        cbi.cancel();
    }

    @Shadow public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {}

}
