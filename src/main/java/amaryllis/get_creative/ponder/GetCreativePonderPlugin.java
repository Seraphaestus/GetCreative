package amaryllis.get_creative.ponder;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.ponder.scenes.MechanicalArmScenes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.infrastructure.ponder.scenes.ArmScenes;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class GetCreativePonderPlugin implements PonderPlugin {

    @Override
    public @NotNull String getModId() {
        return GetCreative.MOD_ID;
    }

    @Override
    public void registerScenes(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

        HELPER.forComponents(AllBlocks.MECHANICAL_ARM)
                .addStoryBoard("mechanical_arm/processing", MechanicalArmScenes::processing);
    }

    /*
    // Example of registering a new ponder to a block which doesn't have any ponders
    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<RegistryEntry<?, ?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

        HELPER.addToTag(AllCreatePonderTags.DISPLAY_SOURCES)
                .add(AllBlocks.TRACK_SIGNAL)
    }*/
}