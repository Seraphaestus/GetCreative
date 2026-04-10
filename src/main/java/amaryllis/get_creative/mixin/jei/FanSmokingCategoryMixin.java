package amaryllis.get_creative.mixin.jei;

import amaryllis.get_creative.Config;
import com.simibubi.create.compat.jei.category.FanSmokingCategory;
import com.simibubi.create.compat.jei.category.ProcessingViaFanCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FanSmokingCategory.class)
public class FanSmokingCategoryMixin extends ProcessingViaFanCategory<SmokingRecipe> {

    public FanSmokingCategoryMixin(Info<SmokingRecipe> info) { super(info); }

    @Overwrite
    protected AllGuiTextures getBlockShadow() {
        return Config.getBlockStateOrDefault(Config.JEI_FAN_SMOKING_CATALYST, Blocks.FIRE).getLightEmission() > 0
                ? AllGuiTextures.JEI_LIGHT
                : AllGuiTextures.JEI_SHADOW;
    }

    @Overwrite
    protected void renderAttachedBlock(GuiGraphics graphics) {
        GuiGameElement.of(Config.getBlockStateOrDefault(Config.JEI_FAN_SMOKING_CATALYST, Blocks.FIRE))
                .scale(SCALE)
                .atLocal(0, 0, 2)
                .lighting(AnimatedKinetics.DEFAULT_LIGHTING)
                .render(graphics);
    }
}
