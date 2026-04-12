package amaryllis.get_creative.precision_assembly;

import amaryllis.get_creative.recipes.CustomCreateRecipeTypes;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Optional;

public class ArmAssembly {

    public static boolean canProcessInBulk() {
        return false;
    }

    public static boolean trySimulate(ArmBlockEntity mechanicalArm, TransportedItemStack input) {
        return tryProcess(mechanicalArm, input, null, true);
    }
    public static boolean tryProcess(ArmBlockEntity mechanicalArm, TransportedItemStack input, ArrayList<ItemStack> results) {
        return tryProcess(mechanicalArm, input, results, false);
    }
    public static boolean tryProcess(ArmBlockEntity mechanicalArm, TransportedItemStack input, ArrayList<ItemStack> results, boolean simulate) {
        final var recipe = getRecipe(mechanicalArm.getLevel(), input.stack);
        if (recipe.isEmpty()) return false;
        if (simulate) return true;

        final var inputStack = canProcessInBulk() ? input.stack : input.stack.copyWithCount(1);
        results.addAll(RecipeApplier.applyRecipeOn(mechanicalArm.getLevel(), inputStack, recipe.get().value(), false));
        return true;
    }

    public static Optional<RecipeHolder<ArmAssemblyRecipe>> getRecipe(Level level, ItemStack item) {
        final var assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level, item, CustomCreateRecipeTypes.ARM_ASSEMBLY.getType(), ArmAssemblyRecipe.class);
        return assemblyRecipe.isPresent() ? assemblyRecipe
                : CustomCreateRecipeTypes.ARM_ASSEMBLY.find(new SingleRecipeInput(item), level);
    }

}
