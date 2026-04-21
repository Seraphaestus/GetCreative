package amaryllis.get_creative.mixin.block_breaking;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.kinetics.drill.DrillActorVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrillActorVisual.class)
public class DrillActorVisualMixin extends ActorVisual {

    @Shadow double rotation;
    @Shadow double previousRotation;

    public DrillActorVisualMixin(VisualizationContext visualizationContext, BlockAndTintGetter world, MovementContext context) {
        super(visualizationContext, world, context);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo cbi) {
        if (GetCreative.shouldDisableActor(AllBlocks.MECHANICAL_DRILL.get(), context)) {
            previousRotation = rotation;
            cbi.cancel();
        }
    }

    @Shadow protected void _delete() {}
}
