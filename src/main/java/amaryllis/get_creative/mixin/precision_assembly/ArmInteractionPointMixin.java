package amaryllis.get_creative.mixin.precision_assembly;

import amaryllis.get_creative.precision_assembly.IArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmInteractionPoint.class)
public class ArmInteractionPointMixin implements IArmInteractionPoint {

    @Shadow protected Mode mode = Mode.DEPOSIT;
    protected boolean isAssemblyTarget = true; // Starts as true because new points immediately call cycleMode

    @Inject(method = "cycleMode", at = @At("HEAD"), cancellable = true)
    private void cycleToExtraModeState(CallbackInfo callback) {
        if (mode == Mode.DEPOSIT) {
            if (isAssemblyTarget) mode = Mode.TAKE;
            isAssemblyTarget = !isAssemblyTarget;
            callback.cancel();
        }
    }

    public boolean isAssemblyTarget() {
        return isAssemblyTarget;
    }

    @Inject(method = "serialize", at = @At("TAIL"))
    protected void serializeModeExtension(CompoundTag nbt, BlockPos anchor, CallbackInfo callbackInfo) {
        nbt.putBoolean("IsAssemblyTarget", isAssemblyTarget);
    }

    @Inject(method = "deserialize", at = @At("TAIL"))
    protected void deserializeModeExtension(CompoundTag nbt, BlockPos anchor, CallbackInfo callbackInfo) {
        if (nbt.contains("IsAssemblyTarget")) isAssemblyTarget = nbt.getBoolean("IsAssemblyTarget");
    }

}
