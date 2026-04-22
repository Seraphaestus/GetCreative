package amaryllis.get_creative.mixin.encapsulation;

import amaryllis.get_creative.encapsulation.CapsulePreviewHandler;
import com.simibubi.create.CreateClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateClient.class)
public class CreateClientMixin {

    @Inject(method = "invalidateRenderers", at = @At("TAIL"))
    private static void invalidateRenderers(CallbackInfo cbi) {
        CapsulePreviewHandler.invalidateRenderer();
    }
}
