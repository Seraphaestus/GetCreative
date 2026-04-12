package amaryllis.get_creative.mixin.precision_assembly;

import amaryllis.get_creative.precision_assembly.FlexibleBeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DepotBehaviour.class)
public class DepotBehaviourMixin extends BlockEntityBehaviour {

    public DepotBehaviourMixin(SmartBlockEntity be) {
        super(be);
    }

    @ModifyVariable(method = "tick", at = @At("STORE"))
    private BeltProcessingBehaviour changeVariable(BeltProcessingBehaviour original) {
        return FlexibleBeltProcessingBehaviour.getBeltProcessingBehaviour(blockEntity.getLevel(), blockEntity.getBlockPos());
    }

    @Shadow public BehaviourType<?> getType() { return null; }
}
