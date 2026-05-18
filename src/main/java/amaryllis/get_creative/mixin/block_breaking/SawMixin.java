package amaryllis.get_creative.mixin.block_breaking;

import amaryllis.get_creative.Config;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SawBlockEntity.class)
public class SawMixin extends BlockBreakingKineticBlockEntity {

    @Shadow ProcessingInventory inventory;
    @Shadow int recipeIndex;
    @Shadow FilteringBehaviour filtering;

    public SawMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    @Inject(method = "onBlockBroken", at = @At("Head"), cancellable = true)
    private void getCreative$onBlockBroken(BlockState stateToBreak, CallbackInfo callback) {
        if (Config.SAW_CAN_MUTLIBREAK.isFalse()) {
            super.onBlockBroken(stateToBreak);
            callback.cancel();
        }
    }

    // TODO: consider return super.canBreak(world, breakingPos, state) && !state.getCollisionShape(world, breakingPos)
    //			.isEmpty() && !AllTags.AllBlockTags.TRACKS.matches(state);

    @Inject(method = "isSawable", at = @At("Head"), cancellable = true)
    private static void getCreative$isSawable(BlockState stateToBreak, CallbackInfoReturnable<Boolean> callback) {
        if (Config.SAW_CAN_BREAK_ALL_BLOCKS.isTrue()) {
            if (stateToBreak.is(BlockTags.SAPLINGS)) {
                callback.setReturnValue(false);
            } else {
                callback.setReturnValue(true);
            }
        }
    }


    //#region Deterministic Saw Processing
    @Redirect(method = "start", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;", ordinal = 0))
    private Object forceDeterministicResults(List<RecipeHolder<? extends Recipe<?>>> recipes, int cycledIndex, ItemStack input) {
        return getCreative$getRecipe(recipes, input);
    }

    @Redirect(method = "applyRecipe", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;", ordinal = 1))
    private Object forceDeterministicResults(List<RecipeHolder<? extends Recipe<?>>> recipes, int cycledIndex) {
        ItemStack input = inventory.getStackInSlot(0);
        return getCreative$getRecipe(recipes, input);
    }

    private boolean getCreative$hasFilter() {
        return filtering != null && filtering.isActive() && !filtering.getFilter().isEmpty();
    }

    private RecipeHolder<? extends Recipe<?>> getCreative$getRecipe(List<RecipeHolder<? extends Recipe<?>>> recipes, ItemStack input) {
        if (Config.DETERMINISTIC_SAW_PROCESSING.isTrue() && recipes.size() > 1 && !getCreative$hasFilter()) {
            ItemStack[] recipeResults = new ItemStack[recipes.size()];
            for (int i = 0; i < recipes.size(); i++) recipeResults[i] = recipes.get(i).value().getResultItem(level.registryAccess());

            // Handle wood
            if (input.is(ItemTags.LOGS)) {
                // Prefer to strip
                for (int i = 0; i < recipes.size(); i++) {
                    if (recipeResults[i].is(Tags.Items.STRIPPED_LOGS) || recipeResults[i].is(Tags.Items.STRIPPED_WOODS))
                        return recipes.get(i);
                }
                // Then prefer to saw into planks
                for (int i = 0; i < recipes.size(); i++) {
                    if (recipeResults[i].is(ItemTags.PLANKS)) return recipes.get(i);
                }
            }
            // Prefer to turn block into stairs
            ResourceLocation stairsID = BuiltInRegistries.ITEM.getKey(input.getItem()).withSuffix("_stairs");
            for (int i = 0; i < recipes.size(); i++) {
                ResourceLocation resultID = BuiltInRegistries.ITEM.getKey(recipeResults[i].getItem());
                if (resultID.equals(stairsID)) return recipes.get(i);
            }
            for (int i = 0; i < recipes.size(); i++) {
                if (recipeResults[i].is(ItemTags.STAIRS)) return recipes.get(i);
            }
        }
        return recipes.get(recipeIndex);
    }
    //#endregion

    @Shadow
    protected BlockPos getBreakingPos() { return null; }
}
