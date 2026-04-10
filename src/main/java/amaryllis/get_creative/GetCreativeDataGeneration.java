package amaryllis.get_creative;

import amaryllis.get_creative.block_breaking.KineticBlockBreaking;
import amaryllis.get_creative.contraptions.hinge_bearing.HandleBlock;
import amaryllis.get_creative.contraptions.hinge_bearing.HingeBearingBlock;
import amaryllis.get_creative.fluid_barrel.FluidBarrelBlock;
import amaryllis.get_creative.generators.EntityCaptureItem;
import amaryllis.get_creative.generators.breeze_whirler.BreezeWhirlerBlock;
import amaryllis.get_creative.generators.clockwork_motor.ClockworkMotorBlock;
import amaryllis.get_creative.generators.clockwork_motor.wind_up_key.WindUpKeyBlock;
import amaryllis.get_creative.generators.haunted_cogwheel.HauntedCogwheelBlock;
import amaryllis.get_creative.industrial_fan.IndustrialFanBlock;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlock;
import com.simibubi.create.AllTags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber
public class GetCreativeDataGeneration {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        final var generator = event.getGenerator();
        final var packOutput = generator.getPackOutput();
        final var fileHelper = event.getExistingFileHelper();
        final var lookupProvider = event.getLookupProvider();

        // Data
        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider));
        generator.addProvider(event.includeServer(), new ModBlockTagProvider(packOutput, lookupProvider, fileHelper));
        generator.addProvider(event.includeServer(), new ModEntityTagProvider(packOutput, lookupProvider, fileHelper));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));

        // Assets
        generator.addProvider(event.includeClient(), new ModBlockAssetsProvider(packOutput, fileHelper));
        generator.addProvider(event.includeClient(), new ItemModelsGenerator(packOutput, fileHelper));
    }

    // Block Drops
    private static class ModLootTableProvider extends BlockLootSubProvider {
        protected ModLootTableProvider(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override protected void generate() {
            dropSelf(HauntedCogwheelBlock.BLOCK.get());
            dropSelf(WindUpKeyBlock.BLOCK.get());
            dropSelf(ClockworkMotorBlock.BLOCK.get());
            dropSelf(BreezeWhirlerBlock.BLOCK.get());
            dropSelf(BreezeWhirlerBlock.EMPTY_BLOCK.get());

            dropSelf(IndustrialFanBlock.BLOCK.get());
            dropSelf(HingeBearingBlock.BLOCK.get());
            HandleBlock.BLOCKS.values().forEach(block -> dropSelf(block.get()));
            dropSelf(FluidBarrelBlock.BLOCK.get());

            dropOther(LecternDeviceBlock.BLOCK.get(), Blocks.LECTERN);
        }

        @Override protected Iterable<Block> getKnownBlocks() {
            return GetCreative.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
        }
    }

    // Block Tags
    private static class ModBlockTagProvider extends BlockTagsProvider {
        public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
            super(output, lookupProvider, GetCreative.MOD_ID, existingFileHelper);
        }

        @Override protected void addTags(HolderLookup.Provider provider) {
            tag(KineticBlockBreaking.DRILL_MINEABLE_TAG)
                    .addTag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .addTag(BlockTags.MINEABLE_WITH_SHOVEL);

            tag(KineticBlockBreaking.SAW_MINEABLE_TAG)
                    .addTag(BlockTags.MINEABLE_WITH_AXE)
                    .addTag(BlockTags.MINEABLE_WITH_HOE);

            tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(HingeBearingBlock.BLOCK.get())
                    .add(IndustrialFanBlock.BLOCK.get())
                    .add(HauntedCogwheelBlock.BLOCK.get())
                    .add(WindUpKeyBlock.BLOCK.get())
                    .add(ClockworkMotorBlock.BLOCK.get())
                    .add(BreezeWhirlerBlock.BLOCK.get())
                    .add(BreezeWhirlerBlock.EMPTY_BLOCK.get())
                    .add(getMetalHandles());

            tag(BlockTags.MINEABLE_WITH_AXE)
                    .add(HingeBearingBlock.BLOCK.get())
                    .add(HauntedCogwheelBlock.BLOCK.get())
                    .add(LecternDeviceBlock.BLOCK.get())
                    .add(ClockworkMotorBlock.BLOCK.get())
                    .add(getWoodenHandles())
                    .add(FluidBarrelBlock.BLOCK.get());

            tag(AllTags.AllBlockTags.WRENCH_PICKUP.tag)
                    .add(HingeBearingBlock.BLOCK.get())
                    .add(IndustrialFanBlock.BLOCK.get())
                    .add(HauntedCogwheelBlock.BLOCK.get())
                    .add(WindUpKeyBlock.BLOCK.get())
                    .add(ClockworkMotorBlock.BLOCK.get())
                    .add(BreezeWhirlerBlock.BLOCK.get())
                    .add(BreezeWhirlerBlock.EMPTY_BLOCK.get());

            tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .add(HingeBearingBlock.BLOCK.get());
        }

        protected Block[] getWoodenHandles() {
            List<Block> output = new ArrayList<>();
            HandleBlock.WOOD_TYPES.forEach(woodType ->
                    output.add(HandleBlock.BLOCKS.get(woodType + "_handle").get()));
            output.add(HandleBlock.BLOCKS.get("crimson_handle").get());
            output.add(HandleBlock.BLOCKS.get("warped_handle").get());
            output.add(HandleBlock.BLOCKS.get("bamboo_handle").get());
            return output.toArray(new Block[0]);
        }
        protected Block[] getMetalHandles() {
            return new Block[] {
                    HandleBlock.BLOCKS.get("copper_handle").get(),
                    HandleBlock.BLOCKS.get("exposed_copper_handle").get(),
                    HandleBlock.BLOCKS.get("weathered_copper_handle").get(),
                    HandleBlock.BLOCKS.get("oxidized_copper_handle").get(),
                    HandleBlock.BLOCKS.get("iron_handle").get(),
                    HandleBlock.BLOCKS.get("industrial_iron_handle").get(),
                    HandleBlock.BLOCKS.get("brass_handle").get(),
            };
        }
    }

    // Entity Tags
    private static class ModEntityTagProvider extends EntityTypeTagsProvider {
        public ModEntityTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
            super(output, lookupProvider, GetCreative.MOD_ID, existingFileHelper);
        }

        @Override protected void addTags(HolderLookup.Provider provider) {
            tag(((EntityCaptureItem)BreezeWhirlerBlock.EMPTY_ITEM.get()).CAPTURABLE)
                    .add(EntityType.BREEZE);
        }
    }

    // Recipes
    private static class ModRecipeProvider extends RecipeProvider {
        public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(@NotNull RecipeOutput output) {
            HandleBlock.WOOD_TYPES.forEach(type ->
                    handleRecipe(type, type + "_planks").save(output));
            handleRecipe("crimson", "crimson_planks").save(output);
            handleRecipe("warped", "warped_planks").save(output);
            handleRecipe("bamboo", "bamboo_planks").save(output);
            handleRecipe("iron", "iron_ingot").save(output);
            handleRecipe("brass", "create", "brass_ingot").save(output);
        }

        private ShapedRecipeBuilder handleRecipe(String type, String inputPath) {
            return handleRecipe(type, "minecraft", inputPath);
        }
        private ShapedRecipeBuilder handleRecipe(String type, String inputNamespace, String inputPath) {
            final Item input = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(inputNamespace, inputPath));
            final Item handle = HandleBlock.ITEMS.get(type + "_handle").get();
            return new ShapedRecipeBuilder(RecipeCategory.REDSTONE, handle, 3)
                    .pattern("###").pattern("###").pattern(" # ")
                    .define('#', input)
                    .unlockedBy("hasItem", has(input));
        }
    }

    // BlockStates and Block Models
    private static class ModBlockAssetsProvider extends BlockStateProvider {
        public ModBlockAssetsProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, GetCreative.MOD_ID, existingFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
            HandleBlock.WOOD_TYPES.forEach(type ->
                    handleModel(type + "_handle", mcLoc("block/stripped_" + type + "_log_top")));
            handleModel("crimson_handle", mcLoc("block/stripped_crimson_stem_top"));
            handleModel("warped_handle", mcLoc("block/stripped_warped_stem_top"));
            List.of("bamboo", "iron", "industrial_iron", "brass").forEach(type ->
                handleModel(type + "_handle", modLoc("block/handle/" + type)));

            handleModel("copper_handle", modLoc("block/handle/copper"));
            handleModel("exposed_copper_handle", modLoc("block/handle/copper_exposed"));
            handleModel("weathered_copper_handle", modLoc("block/handle/copper_weathered"));
            handleModel("oxidized_copper_handle", modLoc("block/handle/copper_oxidized"));
        }

        protected void handleModel(String ID, ResourceLocation texture) {
            directionalBlock(HandleBlock.BLOCKS.get(ID).get(), models()
                    .withExistingParent(ID, GetCreative.ID("block/handle"))
                    .texture("all", texture));
        }
    }

    // Item Models
    private static class ItemModelsGenerator extends ItemModelProvider {
        public ItemModelsGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, GetCreative.MOD_ID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            HandleBlock.TYPES.keySet().forEach(type ->
                    withExistingParent(type + "_handle", modLoc("block/" + type + "_handle")));
        }
    }
}
