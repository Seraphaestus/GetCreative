package amaryllis.get_creative.precision_assembly;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.recipes.CustomCreateRecipeTypes;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.ArrayList;
import java.util.Optional;

public class ArmAssembly {

    public static final String LANG_KEY = "get_creative.arm_assembly.target";
    public static final int OUTLINE_COLOR = 0xA864DF;

    public static boolean canProcessInBulk() {
        return Config.PRECISION_ASSEMBLY_BULK_PROCESSING.isTrue();
    }

    public static boolean trySimulate(ArmBlockEntity mechanicalArm, TransportedItemStack input) {
        return tryProcess(mechanicalArm, input, null, true);
    }
    public static boolean tryProcess(ArmBlockEntity mechanicalArm, TransportedItemStack input, ArrayList<ItemStack> results) {
        return tryProcess(mechanicalArm, input, results, false);
    }
    public static boolean tryProcess(ArmBlockEntity mechanicalArm, TransportedItemStack input, ArrayList<ItemStack> results, boolean simulate) {
        final var recipeHolder = getRecipe(mechanicalArm, input.stack);
        if (recipeHolder.isEmpty()) return false;
        if (simulate) return true;

        ArmAssemblyRecipe recipe = recipeHolder.get().value();

        final var inputStack = canProcessInBulk() ? input.stack : input.stack.copyWithCount(1);
        results.addAll(RecipeApplier.applyRecipeOn(mechanicalArm.getLevel(), inputStack, recipe, false));

        if (!recipe.shouldKeepHeldItem()) {
            ((IMechanicalArm)mechanicalArm).getCreative$damageHeldItem();
        }
        return true;
    }

    static ItemStackHandler oneStackHandler = new ItemStackHandler(1);
    static ItemStackHandler twoStacksHandler = new ItemStackHandler(2);

    public static Optional<RecipeHolder<ArmAssemblyRecipe>> getRecipe(ArmBlockEntity mechanicalArm, ItemStack inputItem) {
        ItemStack itemHeldInHand = ((IMechanicalArm)mechanicalArm).getCreative$getHeldItem();
        return getRecipe(mechanicalArm.getLevel(), inputItem, itemHeldInHand);
    }
    public static Optional<RecipeHolder<ArmAssemblyRecipe>> getRecipe(Level level, ItemStack inputItem, ItemStack itemHeldInHand) {
        RecipeWrapper recipeInput = makeRecipeInput(inputItem, itemHeldInHand);

        final var assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level, recipeInput, CustomCreateRecipeTypes.ARM_ASSEMBLY.getType(), ArmAssemblyRecipe.class);
        if (assemblyRecipe.isPresent()) return assemblyRecipe;

        return CustomCreateRecipeTypes.ARM_ASSEMBLY.find(recipeInput, level);
    }

    protected static RecipeWrapper makeRecipeInput(ItemStack inputItem, ItemStack itemHeldInHand) {
        if (itemHeldInHand.isEmpty()) {
            oneStackHandler.setStackInSlot(0, inputItem);
            return new RecipeWrapper(oneStackHandler);
        } else {
            twoStacksHandler.setStackInSlot(0, inputItem);
            twoStacksHandler.setStackInSlot(1, itemHeldInHand);
            return new RecipeWrapper(twoStacksHandler);
        }
    }

    public static boolean isTarget(ArmInteractionPoint target, BlockPos beltPos) {
        return target.getPos().equals(beltPos) && ((IArmInteractionPoint)target).isAssemblyTarget();
    }

    // Returns null if valid, else a String error message key
    public static String validateTarget(Level level, BlockPos armPos, BlockPos targetPos) {
        if (targetPos.getY() != armPos.getY() ||
            Mth.abs(targetPos.getX() - armPos.getX()) + Mth.abs(targetPos.getZ() - armPos.getZ()) != 1) {
            return "not_adjacent";
        }

        BlockEntity aboveBE = level.getBlockEntity(targetPos.above(2));
        if (aboveBE instanceof SmartBlockEntity aboveSmartBE) {
            if (aboveSmartBE.getAllBehaviours().stream().anyMatch(b -> b instanceof BeltProcessingBehaviour))
                return "interference";
        }

        return isValidTargetBlock(level, targetPos) ? null : "invalid_block";
    }

    public static boolean isValidTargetBlock(Level level, BlockPos pos) {
        BlockEntity targetBE = level.getBlockEntity(pos);
        if (targetBE instanceof BeltBlockEntity) return true;

        if (targetBE instanceof SmartBlockEntity targetSmartBE) {
            if (targetSmartBE.getBehaviour(DepotBehaviour.TYPE) != null) return true;
        }
        return false;
    }
}
