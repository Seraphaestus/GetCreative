package amaryllis.get_creative.ponder;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.encapsulation.EncapsulatorBlock;
import amaryllis.get_creative.encapsulation.GlueSpreaderBlock;
import amaryllis.get_creative.industrial_fan.IndustrialFanBlock;
import amaryllis.get_creative.ponder.scenes.EncapsulationScenes;
import amaryllis.get_creative.ponder.scenes.IndustrialFanScenes;
import amaryllis.get_creative.ponder.scenes.MechanicalArmScenes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class GetCreativePonderPlugin implements PonderPlugin {

    @Override
    public @NotNull String getModId() {
        return GetCreative.MOD_ID;
    }

    @Override
    public void registerScenes(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(AllBlocks.MECHANICAL_ARM.getId())
                .addStoryBoard("mechanical_arm/processing", MechanicalArmScenes::processing);

        helper.forComponents(GlueSpreaderBlock.BLOCK.getId())
                .addStoryBoard("glue_spreader", EncapsulationScenes::glue_spreader);

        helper.forComponents(EncapsulatorBlock.BLOCK.getId())
                .addStoryBoard("encapsulator", EncapsulationScenes::encapsulator);

        helper.forComponents(IndustrialFanBlock.BLOCK.getId())
                .addStoryBoard("industrial_fan/direction", IndustrialFanScenes::direction, AllCreatePonderTags.KINETIC_APPLIANCES)
                .addStoryBoard("industrial_fan/processing", IndustrialFanScenes::processing);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.addToTag(AllCreatePonderTags.CONTRAPTION_ASSEMBLY)
                .add(GlueSpreaderBlock.BLOCK.getId())
                .add(EncapsulatorBlock.BLOCK.getId());
    }
}