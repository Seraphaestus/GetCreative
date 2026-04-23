package amaryllis.get_creative.recipes;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.precision_assembly.ArmAssemblyCategory;
import amaryllis.get_creative.precision_assembly.ArmAssemblyRecipe;
import com.google.common.base.Preconditions;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    private static final ResourceLocation MOD_ID = GetCreative.ID("jei_plugin");
    private final List<CreateRecipeCategory<?>> categories = new ArrayList<>();

    @Override public ResourceLocation getPluginUid() { return MOD_ID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        this.categories.clear();

        this.categories.add(
                new CreateRecipeCategory.Builder<>(ArmAssemblyRecipe.class)
                    .addTypedRecipes(CustomCreateRecipeTypes.ARM_ASSEMBLY)
                    .catalyst(AllBlocks.MECHANICAL_ARM::get)
                    .itemIcon(AllBlocks.MECHANICAL_ARM.get())
                    .emptyBackground(177, 70)
                    .build(GetCreative.ID("arm_assembly"), ArmAssemblyCategory::new)
        );
        registration.addRecipeCategories(categories.toArray(IRecipeCategory[]::new));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        categories.forEach(category -> category.registerRecipes(registration));
        MysteriousConversionRecipes.register();
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        categories.forEach(category -> category.registerCatalysts(registration));
    }

    @ApiStatus.Internal
    public static Level getLevel() {
        if (FMLLoader.getDist() != Dist.CLIENT)
            throw new IllegalStateException("Retreiving client level is only supported for client");
        var minecraft = Minecraft.getInstance();
        Preconditions.checkNotNull(minecraft, "minecraft must not be null");
        var level = minecraft.level;
        Preconditions.checkNotNull(level, "level must not be null");
        return level;
    }

    @ApiStatus.Internal
    public static RecipeManager getRecipeManager() {
        if (FMLLoader.getDist() != Dist.CLIENT)
            throw new IllegalStateException("Retreiving recipe manager from client level is only supported for client");
        return getLevel().getRecipeManager();
    }

}