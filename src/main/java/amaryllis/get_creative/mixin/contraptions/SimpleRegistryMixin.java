package amaryllis.get_creative.mixin.contraptions;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.impl.registry.SimpleRegistryImpl;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleRegistryImpl.class)
public class SimpleRegistryMixin<K, V> {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private void onRegister(K object, V value, CallbackInfo cbi) {
        if (value instanceof MovementBehaviour movementBehaviour && object instanceof Block block) {
            if (!GetCreative.shouldRegisterActor(block, movementBehaviour)) {
                cbi.cancel();
            }
        }
    }

}
