package amaryllis.get_creative.mixin.precision_assembly;

import amaryllis.get_creative.recipes.precision_assembly.FlexibleBeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BeltInventory.class)
public class BeltInventoryMixin {

    @Shadow BeltBlockEntity belt;

    @Overwrite
    protected BeltProcessingBehaviour getBeltProcessingAtSegment(int segment) {
        final BlockPos origin = BeltHelper.getPositionForOffset(belt, segment);
        return FlexibleBeltProcessingBehaviour.getBeltProcessingBehaviour(belt.getLevel(), origin);
    }
}
