package amaryllis.get_creative.recipes;

import amaryllis.get_creative.encapsulation.CapsuleItem;
import amaryllis.get_creative.encapsulation.EncapsulatorBlock;
import amaryllis.get_creative.generators.breeze_whirler.BreezeWhirlerBlock;
import com.simibubi.create.compat.jei.ConversionRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory.RECIPES;

public class MysteriousConversionRecipes {

    public static void register() {
        RECIPES.add(create(BreezeWhirlerBlock.EMPTY_ITEM, BreezeWhirlerBlock.ITEM));
        RECIPES.add(create(EncapsulatorBlock.ITEM, CapsuleItem.ITEM));
    }

    protected static RecipeHolder<ConversionRecipe> create(DeferredItem<? extends Item> input, DeferredItem<? extends Item> output) {
        return ConversionRecipe.create(input.toStack(), output.toStack());
    }

}
