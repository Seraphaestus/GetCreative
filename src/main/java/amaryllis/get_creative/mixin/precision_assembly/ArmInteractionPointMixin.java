package amaryllis.get_creative.mixin.precision_assembly;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.precision_assembly.ArmAssembly;
import amaryllis.get_creative.precision_assembly.IArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmInteractionPoint.class)
public class ArmInteractionPointMixin implements IArmInteractionPoint {

    @Shadow protected Mode mode = Mode.DEPOSIT;
    protected boolean isAssemblyTarget = false;
    protected boolean isFirstCycle = true;

    @Shadow Level level;
    @Shadow BlockPos pos;

    @Inject(method = "cycleMode", at = @At("HEAD"), cancellable = true)
    private void cycleToExtraModeState(CallbackInfo callback) {
        if (!ArmAssembly.isValidTargetBlock(level, pos)) {
            isAssemblyTarget = false;
            return;
        }

        if (isFirstCycle) {
            isFirstCycle = false;
            mode = Mode.TAKE;
            callback.cancel();
        }
        else if (mode == Mode.DEPOSIT) {
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
        nbt.putBoolean("IsFirstCycle", isFirstCycle);
    }

    @Inject(method = "deserialize", at = @At("TAIL"))
    protected void deserializeModeExtension(CompoundTag nbt, BlockPos anchor, CallbackInfo callbackInfo) {
        if (nbt.contains("IsAssemblyTarget")) isAssemblyTarget = nbt.getBoolean("IsAssemblyTarget");
        if (nbt.contains("IsFirstCycle")) isFirstCycle = nbt.getBoolean("IsFirstCycle");
    }

}
