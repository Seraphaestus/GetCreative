package amaryllis.get_creative.precision_assembly;

import amaryllis.get_creative.recipes.CustomCreateRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class ArmAssemblyRecipe extends ProcessingRecipe<RecipeWrapper, ArmAssemblyRecipe.Params> implements IAssemblyRecipe {

    protected boolean keepHeldItem;

    public ArmAssemblyRecipe(Params params) {
        super(CustomCreateRecipeTypes.ARM_ASSEMBLY, params);
        keepHeldItem = params.keepHeldItem;
    }

    @Override
    public boolean matches(RecipeWrapper input, Level level) {
        if (!getInputItem().test(input.getItem(0))) return false;

        Ingredient requiredHeldItem = getRequiredHeldItem();
        ItemStack heldItem = (input.size() >= 2) ? input.getItem(1) : ItemStack.EMPTY;
        return !requiredHeldItem.isEmpty() ? requiredHeldItem.test(heldItem) : heldItem.isEmpty();
    }

    @Override protected int getMaxInputCount() { return 2; }
    @Override protected int getMaxOutputCount() { return 4; }

    public boolean shouldKeepHeldItem() {
        return keepHeldItem;
    }

    public Ingredient getRequiredHeldItem() {
        if (ingredients.size() < 2) return Ingredient.EMPTY;
        return ingredients.get(1);
    }

    public Ingredient getInputItem() {
        if (ingredients.isEmpty()) throw new IllegalStateException("Precision Assembly Recipe has no ingredient!");
        return ingredients.getFirst();
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
        var requiredHeldItem = getRequiredHeldItem();
        if (!requiredHeldItem.isEmpty()) list.add(requiredHeldItem);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        Ingredient requiredHeldItem = getRequiredHeldItem();
        if (requiredHeldItem.isEmpty()) return Component.translatable("get_creative.recipe.assembly.arm");

        ItemStack[] matchingStacks = requiredHeldItem.getItems();
        if (matchingStacks.length == 0) return Component.literal("Invalid");
        String itemName = Component.translatable(matchingStacks[0].getDescriptionId()).getString();
        return Component.translatable("get_creative.recipe.assembly.arm_with_item", itemName);
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(AllBlocks.MECHANICAL_ARM.get());
    }

    @Override
    public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
        return () -> ArmAssemblyCategory.RecipeViewerCategory::new;
    }

    //region Factory, Builder, and Serializer
    @FunctionalInterface
    public interface Factory<R extends ArmAssemblyRecipe> extends ProcessingRecipe.Factory<Params, R> {
        @NotNull R create(Params params);
    }

    public static class Builder<R extends ArmAssemblyRecipe> extends ProcessingRecipeBuilder<Params, R, ArmAssemblyRecipe.Builder<R>> {
        public Builder(ArmAssemblyRecipe.Factory<R> factory, ResourceLocation recipeId) {
            super(factory, recipeId);
        }

        @Override
        protected Params createParams() {
            return new Params();
        }

        @Override
        public ArmAssemblyRecipe.Builder<R> self() {
            return this;
        }

        public ArmAssemblyRecipe.Builder<R> toolNotConsumed() {
            params.keepHeldItem = true;
            return this;
        }
    }

    public static class Serializer<R extends ArmAssemblyRecipe> implements RecipeSerializer<R> {
        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        public Serializer(ProcessingRecipe.Factory<Params, R> factory) {
            this.codec = ProcessingRecipe.codec(factory, Params.CODEC);
            this.streamCodec = ProcessingRecipe.streamCodec(factory, Params.STREAM_CODEC);
        }

        @Override
        public @NotNull MapCodec<R> codec() {
            return codec;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
            return streamCodec;
        }

    }
    //endregion

    public static class Params extends ProcessingRecipeParams {
        public static MapCodec<Params> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                codec(Params::new).forGetter(Function.identity()),
                Codec.BOOL.optionalFieldOf("keep_held_item", false).forGetter(Params::keepHeldItem)
        ).apply(instance, (params, keepHeldItem) -> {
            params.keepHeldItem = keepHeldItem;
            return params;
        }));
        public static StreamCodec<RegistryFriendlyByteBuf, Params> STREAM_CODEC = streamCodec(Params::new);

        protected boolean keepHeldItem;

        protected final boolean keepHeldItem() {
            return keepHeldItem;
        }

        @Override
        protected void encode(RegistryFriendlyByteBuf buffer) {
            super.encode(buffer);
            ByteBufCodecs.BOOL.encode(buffer, keepHeldItem);
        }

        @Override
        protected void decode(RegistryFriendlyByteBuf buffer) {
            super.decode(buffer);
            keepHeldItem = ByteBufCodecs.BOOL.decode(buffer);
        }
    }
}