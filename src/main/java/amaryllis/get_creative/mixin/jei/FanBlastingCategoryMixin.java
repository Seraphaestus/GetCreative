package amaryllis.get_creative.mixin.jei;

import amaryllis.get_creative.Config;
import com.simibubi.create.compat.jei.category.FanBlastingCategory;
import com.simibubi.create.compat.jei.category.ProcessingViaFanCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FanBlastingCategory.class)
public class FanBlastingCategoryMixin extends ProcessingViaFanCategory<BlastingRecipe> {

    public FanBlastingCategoryMixin(Info<BlastingRecipe> info) { super(info); }

    @Overwrite
    protected AllGuiTextures getBlockShadow() {
        return Config.getBlockStateOrDefault(Config.JEI_FAN_BLASTING_CATALYST, Blocks.LAVA).getLightEmission() > 0
                ? AllGuiTextures.JEI_LIGHT
                : AllGuiTextures.JEI_SHADOW;
    }

    @Overwrite
    protected void renderAttachedBlock(GuiGraphics graphics) {
        GuiGameElement.of(Config.getBlockStateOrDefault(Config.JEI_FAN_BLASTING_CATALYST, Blocks.LAVA))
                .scale(SCALE)
                .atLocal(0, 0, 2)
                .lighting(AnimatedKinetics.DEFAULT_LIGHTING)
                .render(graphics);
    }
}
