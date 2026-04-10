package amaryllis.get_creative.mixin.contraptions;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MovementBehaviour.class)
public interface ActorMixin {

    @Overwrite
    static <B extends Block> NonNullConsumer<? super B> movementBehaviour(MovementBehaviour behaviour) {
        return block -> GetCreative.tryRegisterActor(block, behaviour);
    }
}