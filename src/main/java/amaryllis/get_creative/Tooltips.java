package amaryllis.get_creative;

import amaryllis.get_creative.appliances.industrial_fan.IndustrialFanBlock;
import amaryllis.get_creative.contraptions.ActorConfigHandler;
import amaryllis.get_creative.utility.CompatHelper;
import amaryllis.get_creative.utility.TagHelper;
import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.trains.station.StationBlock;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@EventBusSubscriber(value = Dist.CLIENT)
public class Tooltips {

    private static final TagKey<Block> ACTOR_COLLECTS_ITEMS = TagKey.create(Registries.BLOCK, Create.asResource("actor_collects_items"));

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null) return;

        Item item = event.getItemStack().getItem();
        Block block = (item instanceof BlockItem blockItem) ? blockItem.getBlock() : null;

        // Add shift-tooltip support for mod items
        String namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
        if (namespace.equals(GetCreative.MOD_ID)) addShiftDescriptions(event, block, item);

        // Add tooltip for configured blocks
        if (block != null && Config.SHOW_CONFIG_TOOLTIPS.isTrue()) {
            var configOptions = ConfigOptions.of(block);
            if (configOptions.hasAny()) {
                boolean expand = Screen.hasControlDown();

                int index = getConfigDocumentationIndex(event.getToolTip());
                var ctrl = Component.translatable("create.tooltip.keyCtrl")
                        .withStyle(expand ? ChatFormatting.WHITE : ChatFormatting.GRAY);
                var tooltip = Component.translatable("tooltip.get_creative.config", ctrl)
                        .withStyle(ChatFormatting.DARK_GRAY);

                event.getToolTip().add(index, tooltip);
                if (expand) addConfigDocumentation(event, index + 1, block, configOptions);
            }
        }
    }

    private static void addShiftDescriptions(ItemTooltipEvent event, Block block, Item item) {
        // Allow item variants to redirect to one single tooltip
        if (block instanceof RedirectShiftTooltip redirector) {
            var itemID = redirector.redirectShiftTooltip();
            if (BuiltInRegistries.ITEM.containsKey(itemID)) {
                item = BuiltInRegistries.ITEM.get(itemID);
            }
        }

        // Data-driven Create tooltips
        TooltipModifier tooltip = new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE);
        tooltip = tooltip.andThen(TooltipModifier.mapNull(KineticStats.create(item)));
        tooltip.modify(event);
    }

    private static Stream<RecipeHolder<?>> getRecipes() {
        return Minecraft.getInstance().getConnection().getRecipeManager().getRecipes().stream();
    }

    private static int getConfigDocumentationIndex(List<Component> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().isEmpty()) return i;
        }
        return tooltip.size();
    }

    private static void addConfigDocumentation(ItemTooltipEvent event, int index, Block block, ConfigOptions configOptions) {
        List<Component> components = new ArrayList<>();

        if (configOptions.actorDisabled || configOptions.gravityActor) {
            components.add(Component.translatable(CompatHelper.isModLoaded("sable")
                ? "tooltip.get_creative.config.actors.disabled.simulated"
                : "tooltip.get_creative.config.actors.disabled"
            ));
            if (configOptions.gravityActor) components.add(Component.translatable("tooltip.get_creative.config.actors.gravity"));
        }
        if (configOptions.noStoreItems && !configOptions.actorDisabled) components.add(Component.translatable("tooltip.get_creative.config.no_store_items"));
        if (configOptions.miningSpecialty) {
            var speciality = Component.translatable(block instanceof DrillBlock
                    ? "tooltip.get_creative.config.mining_speciality.drill"
                    : "tooltip.get_creative.config.mining_speciality.saw");
            components.add(Component.translatable("tooltip.get_creative.config.mining_speciality", speciality));
        }
        if (configOptions.noMultibreak) components.add(Component.translatable("tooltip.get_creative.config.saw.no_multibreak"));
        if (configOptions.deterministicSaw) components.add(Component.translatable("tooltip.get_creative.config.saw.deterministic"));
        if (configOptions.noReplant) components.add(Component.translatable("tooltip.get_creative.config.harvester.no_replant"));
        if (configOptions.harvestImmature) components.add(Component.translatable("tooltip.get_creative.config.harvester.immature"));
        if (configOptions.fueledTrains) components.add(Component.translatable("tooltip.get_creative.config.trains.need_fuel"));
        for (ResourceLocation fanType: configOptions.noFanProcessing) {
            var typePath = fanType.getPath();
            if (typePath.equals("splashing")) typePath = "washing";
            var type = Component.translatable(fanType.getNamespace() + ".recipe.fan_" + typePath);
            components.add(Component.translatable("tooltip.get_creative.config.no_fan_processing", type));
        }

        for (int i = components.size() - 1; i >= 0; i--) {
            event.getToolTip().addAll(index, TooltipHelper.cutTextComponent(components.get(i), FontHelper.Palette.RED));
        }
    }


    private record ConfigOptions(boolean actorDisabled, boolean gravityActor, boolean noStoreItems, boolean miningSpecialty,
                                 boolean noMultibreak, boolean deterministicSaw, boolean harvestImmature, boolean noReplant,
                                 boolean fueledTrains, List<ResourceLocation> noFanProcessing) {
        public boolean hasAny() {
            return actorDisabled || gravityActor || noStoreItems || miningSpecialty ||
                   noMultibreak || deterministicSaw || harvestImmature || noReplant ||
                   fueledTrains || !noFanProcessing.isEmpty();
        }

        public static ConfigOptions of(Block block) {
            boolean actorDisabled = ActorConfigHandler.isActorDisabled(block);
            boolean isSaw = block instanceof SawBlock;
            boolean isHarvester = block instanceof HarvesterBlock;
            boolean miningSpecialityDrill = block instanceof DrillBlock && Config.DRILL_SPEED_MODIFIER.get() != 1;
            boolean miningSpecialitySaw = isSaw && Config.SAW_CAN_BREAK_ALL_BLOCKS.isTrue() && Config.SAW_SPEED_MODIFIER.get() != 1;
            List<ResourceLocation> disabledFanProcessingTypes =
                (block instanceof EncasedFanBlock || block instanceof IndustrialFanBlock)
                    ? FanProcessingTypeHelper.getDisabledTypes() : List.of();

            return new ConfigOptions(
                    actorDisabled,
                    !actorDisabled && ActorConfigHandler.isActorGravityOnly(block),
                    block.defaultBlockState().is(ACTOR_COLLECTS_ITEMS) && !AllConfigs.server().kinetics.moveItemsToStorage.get(),
                    miningSpecialityDrill || miningSpecialitySaw,
                    isSaw && Config.SAW_CAN_MUTLIBREAK.isFalse(),
                    isSaw && Config.DETERMINISTIC_SAW_PROCESSING.isTrue(),
                    isHarvester && AllConfigs.server().kinetics.harvestPartiallyGrown.get(),
                    isHarvester && !AllConfigs.server().kinetics.harvesterReplants.get(),
                    block instanceof StationBlock && AllConfigs.server().trains.trainTopSpeed.get() <= 0,
                    disabledFanProcessingTypes
            );
        }
    }


    private record FanProcessingTypeHelper(TagKey<Block> blockCatalysts, TagKey<Fluid> fluidCatalysts, Boolean hasRecipes) {
        private static final Map<ResourceLocation, FanProcessingTypeHelper> PROCESSING_TYPES = new HashMap<>();

        private static final ResourceLocation FAN_BLASTING = Create.asResource("blasting");
        private static final ResourceLocation FAN_SMOKING = Create.asResource("smoking");

        public static FanProcessingTypeHelper of(ResourceLocation typeID, RecipeType<?> recipeType) {
            var catalystID = typeID.withPrefix("fan_processing_catalysts/");
            return new FanProcessingTypeHelper(
                    TagKey.create(Registries.BLOCK, catalystID),
                    TagKey.create(Registries.FLUID, catalystID),
                    getRecipes().anyMatch(recipe -> recipe.value().getType().equals(recipeType))
            );
        }

        public static @Nullable RecipeType<?> getRecipeType(ResourceLocation typeID) {
            if (typeID.equals(FAN_BLASTING)) return RecipeType.BLASTING;
            if (typeID.equals(FAN_SMOKING)) return RecipeType.SMOKING;
            return BuiltInRegistries.RECIPE_TYPE.get(typeID);
        }

        public boolean isDisabled(RegistryAccess registryAccess) {
            return !hasRecipes || !TagHelper.hasAnyBlocks(registryAccess, blockCatalysts)
                               && !TagHelper.hasAnyFluids(registryAccess, fluidCatalysts);
        }

        public static boolean isDisabled(ResourceLocation type) {
            if (Minecraft.getInstance().level == null) return false;
            var registryAccess = Minecraft.getInstance().level.registryAccess();

            // Redirect Blasting, Smoking types which don't have their own recipe type
            RecipeType<?> recipeType = FanProcessingTypeHelper.getRecipeType(type);
            if (recipeType == null) return false;

            // Cache data for fan processing type
            if (!PROCESSING_TYPES.containsKey(type)) {
                PROCESSING_TYPES.put(type, FanProcessingTypeHelper.of(type, recipeType));
            }

            return PROCESSING_TYPES.get(type).isDisabled(registryAccess);
        }

        public static List<ResourceLocation> getDisabledTypes() {
            List<ResourceLocation> disabledTypes = new ArrayList<>();
            for (var entry : CreateBuiltInRegistries.FAN_PROCESSING_TYPE.entrySet()) {
                var type = entry.getKey().location();
                if (FanProcessingTypeHelper.isDisabled(type)) disabledTypes.add(type);
            }
            return disabledTypes;
        }

    }

    
    public interface RedirectShiftTooltip {
        ResourceLocation redirectShiftTooltip();
    }
}
