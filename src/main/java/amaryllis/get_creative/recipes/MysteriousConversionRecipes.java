package amaryllis.get_creative.recipes;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.utility.ItemStackParser;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.function.Consumer;

import static com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory.RECIPES;

public class MysteriousConversionRecipes {

    protected static int counter = 0;

    public static void register() {
        for (String recipe: Config.MYSTERIOUS_CONVERSIONS.get()) {
            String[] split = recipe.split("->");
            if (split.length < 2) return;

            Consumer<Exception> errorHandler = e ->  GetCreative.LOGGER.error("Could not parse mysterious conversion recipe {}: {}", recipe, e.getLocalizedMessage());

            Ingredient input = ItemStackParser.parseIngredient(split[0].trim(), errorHandler);
            ItemStack output = ItemStackParser.parse(split[1].trim(), errorHandler);

            if (!input.isEmpty() && !output.isEmpty()) RECIPES.add(create(input, output));
        }
        ItemStackParser.clean();
    }

    protected static RecipeHolder<ConversionRecipe> create(Ingredient input, ItemStack output) {
        ResourceLocation recipeID = GetCreative.ID("conversion_" + counter++);
        ConversionRecipe recipe = new StandardProcessingRecipe.Builder<>(ConversionRecipe::new, recipeID)
                .withItemIngredients(input)
                .withSingleItemOutput(output)
                .build();
        return new RecipeHolder<>(recipeID, recipe);
    }
}
