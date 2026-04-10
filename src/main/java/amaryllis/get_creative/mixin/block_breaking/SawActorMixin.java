package amaryllis.get_creative.mixin.block_breaking;

import amaryllis.get_creative.Config;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SawMovementBehaviour.class)
public class SawActorMixin extends BlockBreakingMovementBehaviour {

    @Inject(method = "onBlockBroken", at = @At("Head"), cancellable = true)
    private void getCreative$onBlockBroken(MovementContext context, BlockPos pos, BlockState brokenState, CallbackInfo callback) {
        if (Config.SAW_CAN_MUTLIBREAK.isFalse()) {
            super.onBlockBroken(context, pos, brokenState);

            callback.cancel();
        }
    }
}
