package amaryllis.get_creative.recipes;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.ItemStackParser;
import com.simibubi.create.compat.jei.ConversionRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Consumer;

import static com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory.RECIPES;

public class MysteriousConversionRecipes {

    public static void register() {
        for (String recipe: Config.MYSTERIOUS_CONVERSIONS.get()) {
            String[] split = recipe.split("->");
            if (split.length < 2) return;

            Consumer<Exception> errorHandler = e ->  GetCreative.LOGGER.error("Could not parse mysterious conversion recipe {}: {}", recipe, e.getLocalizedMessage());

            ItemStack input = ItemStackParser.parse(split[0], errorHandler);
            ItemStack output = ItemStackParser.parse(split[1], errorHandler);

            if (!input.isEmpty() && !output.isEmpty()) RECIPES.add(ConversionRecipe.create(input, output));
        }
        ItemStackParser.clean();
    }

    protected static RecipeHolder<ConversionRecipe> create(DeferredItem<? extends Item> input, DeferredItem<? extends Item> output) {
        return ConversionRecipe.create(input.toStack(), output.toStack());
    }
}
