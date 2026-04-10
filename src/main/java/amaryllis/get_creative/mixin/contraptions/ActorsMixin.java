package amaryllis.get_creative.mixin.contraptions;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.behaviour.BellMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.CampfireMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.dispenser.DispenserMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.dispenser.DropperMovementBehaviour;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AllMovementBehaviours.class)
public class ActorsMixin {

    @Overwrite
    static void registerDefaults() {
        GetCreative.tryRegisterActor(Blocks.BELL, new BellMovementBehaviour());
        GetCreative.tryRegisterActor(Blocks.CAMPFIRE, new CampfireMovementBehaviour());
        GetCreative.tryRegisterActor(Blocks.SOUL_CAMPFIRE, new CampfireMovementBehaviour());
        GetCreative.tryRegisterActor(Blocks.DISPENSER, new DispenserMovementBehaviour());
        GetCreative.tryRegisterActor(Blocks.DROPPER, new DropperMovementBehaviour());
    }

}
