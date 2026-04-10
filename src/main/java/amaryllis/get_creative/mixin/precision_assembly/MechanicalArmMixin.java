package amaryllis.get_creative.mixin.precision_assembly;

import amaryllis.get_creative.precision_assembly.ArmAssemblyBehaviour;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ArmBlockEntity.class)
public class MechanicalArmMixin extends KineticBlockEntity implements TransformableBlockEntity, ArmAssemblyBehaviour.IArmAssembler {

    @Shadow List<ArmInteractionPoint> inputs;
    @Shadow List<ArmInteractionPoint> outputs;
    @Shadow ItemStack heldItem;
    @Shadow int tooltipWarmup;

    protected ArmAssemblyBehaviour armAssemblyBehaviour;
    protected boolean processingArmAssembly = false;

    public MechanicalArmMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) { super(typeIn, pos, state); }

    @Shadow public void transform(BlockEntity blockEntity, StructureTransform transform) {}
    @Shadow private void selectIndex(boolean input, int index) {}
    @Shadow protected void initInteractionPoints() {}
    @Shadow private boolean tickMovementProgress() { return false; }

    @Inject(method = "write", at = @At(value = "HEAD"))
    public void getCreative$write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo cbi) {
        compound.putBoolean("ProcessingArmAssembly", processingArmAssembly);
    }

    @Inject(method = "read", at = @At(value = "HEAD"))
    protected void getCreative$read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo cbi) {
        processingArmAssembly = tag.getBoolean("ProcessingArmAssembly");
    }

    @Inject(method = "addBehaviours", at = @At(value = "RETURN"))
    public void getCreative$addBehaviours(List<BlockEntityBehaviour> behaviours, CallbackInfo cbi) {
        armAssemblyBehaviour = new ArmAssemblyBehaviour((ArmBlockEntity)(Object)this);
        behaviours.add(armAssemblyBehaviour);
    }

    @Inject(method = "collectItem", at = @At(value = "HEAD"), cancellable = true)
    public void getCreative$skipCollectingItem(CallbackInfo cbi) {
        if (processingArmAssembly) cbi.cancel();
    }

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    public void getCreative$skipLockingItem(CallbackInfo cbi) {
        if (processingArmAssembly) {
            // Hacky code reduplication to simulate the origin up until the part we want to exit
            super.tick();
            initInteractionPoints();
            boolean targetReached = tickMovementProgress();
            if (tooltipWarmup > 0) tooltipWarmup--;
            //
            cbi.cancel();
        }
    }

    public boolean canProcessArmAssembly(BlockPos beltPos) {
        if (!heldItem.isEmpty()) return false;
        if (getSpeed() == 0) return false;

        ArmInteractionPoint target;
        if (inputs.size() == 1 && outputs.isEmpty()) target = inputs.getFirst();
        else if (inputs.isEmpty() && outputs.size() == 1) target = outputs.getFirst();
        else if (inputs.size() == 1 && outputs.size() == 1 && inputs.getFirst().equals(outputs.getFirst())) target = outputs.getFirst();
        else return false;

        return target.getPos().equals(beltPos);
    }

    public void startArmAssembly() {
        processingArmAssembly = true;
        selectIndex(!inputs.isEmpty(), 0);
    }

    public void completeArmAssembly() {
        processingArmAssembly = false;
    }
}
