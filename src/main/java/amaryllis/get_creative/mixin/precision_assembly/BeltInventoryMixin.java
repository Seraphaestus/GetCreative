package amaryllis.get_creative.mixin.precision_assembly;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.precision_assembly.ArmAssemblyBehaviour;
import amaryllis.get_creative.precision_assembly.FlexibleBeltProcessingBehaviour;
import com.ibm.icu.util.Output;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeltInventory.class)
public class BeltInventoryMixin {

    @Shadow BeltBlockEntity belt;

    @Overwrite
    protected BeltProcessingBehaviour getBeltProcessingAtSegment(int segment) {
        final BlockPos origin = BeltHelper.getPositionForOffset(belt, segment);
        return FlexibleBeltProcessingBehaviour.getBeltProcessingBehaviour(belt.getLevel(), origin);
    }
}
