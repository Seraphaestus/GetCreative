package amaryllis.get_creative.utility;


import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record FanProcessingTypeHelper(ResourceLocation typeID, TagKey<Block> blockCatalysts, TagKey<Fluid> fluidCatalysts, Boolean hasRecipes) {
    private static final Map<ResourceLocation, FanProcessingTypeHelper> PROCESSING_TYPES = new HashMap<>();
    private static final Map<ResourceLocation, Component> NAMES = new HashMap<>();

    private static final ResourceLocation FAN_BLASTING = Create.asResource("blasting");
    private static final ResourceLocation FAN_SMOKING = Create.asResource("smoking");

    public static FanProcessingTypeHelper of(ResourceLocation typeID, RecipeType<?> recipeType) {
        var catalystID = typeID.withPrefix("fan_processing_catalysts/");
        return new FanProcessingTypeHelper(
                typeID,
                TagKey.create(Registries.BLOCK, catalystID),
                TagKey.create(Registries.FLUID, catalystID),
                getRecipes().anyMatch(recipe -> recipe.value().getType().equals(recipeType))
        );
    }

    private static Stream<RecipeHolder<?>> getRecipes() {
        return Minecraft.getInstance().getConnection().getRecipeManager().getRecipes().stream();
    }

    public static @Nullable RecipeType<?> getRecipeType(ResourceLocation typeID) {
        if (typeID.equals(FAN_BLASTING)) return RecipeType.BLASTING;
        if (typeID.equals(FAN_SMOKING)) return RecipeType.SMOKING;
        return BuiltInRegistries.RECIPE_TYPE.get(typeID);
    }

    public boolean isDisabled(RegistryAccess registryAccess) {
        if (!hasRecipes) return true;

        return !TagHelper.hasAnyBlocks(registryAccess, blockCatalysts)
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

    public static List<ResourceLocation> getDisabledTypes(boolean createOnly) {
        List<ResourceLocation> disabledTypes = new ArrayList<>();
        for (var entry : CreateBuiltInRegistries.FAN_PROCESSING_TYPE.entrySet()) {
            var type = entry.getKey().location();
            if (createOnly && !type.getNamespace().equals("create")) continue;
            if (FanProcessingTypeHelper.isDisabled(type)) disabledTypes.add(type);
        }
        return disabledTypes;
    }

    public static Component getName(ResourceLocation type) {
        if (!NAMES.containsKey(type)) {
            var path = type.getPath();
            if (path.equals("splashing")) path = "washing";

            var key = type.getNamespace() + ".recipe.fan_" + type.getPath();
            var name = Component.translatable(key);

            if (name.toString().equals(key)) {
                key = "recipe." + type.getNamespace() + ".fan_" + type.getPath();
                name = Component.translatable(key);

                if (name.toString().equals(key)) {
                    name = Component.literal("Fan " + path.substring(0, 1).toUpperCase() + path.substring(1));
                }
            }
            NAMES.put(type, name);
        }
        return NAMES.get(type);
    }

}
