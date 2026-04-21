package amaryllis.get_creative.mixin.contraptions;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MovementBehaviour.class)
public interface MovementBehaviourMixin {

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void isActive(MovementContext context, CallbackInfoReturnable<Boolean> cbi) {
        if (GetCreative.shouldDisableActor(context.state.getBlock(), context))
            cbi.setReturnValue(false);
    }

}
