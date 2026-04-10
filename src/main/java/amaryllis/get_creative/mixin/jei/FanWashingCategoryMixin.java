package amaryllis.get_creative.mixin.jei;

import amaryllis.get_creative.Config;
import com.simibubi.create.compat.jei.category.FanWashingCategory;
import com.simibubi.create.compat.jei.category.ProcessingViaFanCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FanWashingCategory.class)
public class FanWashingCategoryMixin extends ProcessingViaFanCategory.MultiOutput<SplashingRecipe> {

    public FanWashingCategoryMixin(Info<SplashingRecipe> info) { super(info); }

    @Override
    protected AllGuiTextures getBlockShadow() {
        return Config.getBlockStateOrDefault(Config.JEI_FAN_WASHING_CATALYST, Blocks.WATER).getLightEmission() > 0
                ? AllGuiTextures.JEI_LIGHT
                : AllGuiTextures.JEI_SHADOW;
    }

    @Overwrite
    protected void renderAttachedBlock(GuiGraphics graphics) {
        GuiGameElement.of(Config.getBlockStateOrDefault(Config.JEI_FAN_WASHING_CATALYST, Blocks.WATER))
                .scale(SCALE)
                .atLocal(0, 0, 2)
                .lighting(AnimatedKinetics.DEFAULT_LIGHTING)
                .render(graphics);
    }
}
