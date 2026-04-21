package amaryllis.get_creative.mixin.contraptions;

import amaryllis.get_creative.contraptions.hinge_bearing.HingeBearingBlockEntity;
import amaryllis.get_creative.contraptions.hinge_bearing.HingeBearingInteractPacket;
import amaryllis.get_creative.contraptions.hinge_bearing.IControlledContraptionEntityAccessor;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractContraptionEntity.class)
public class ContraptionMixin {

    // Note this is only called on the client, so we have to send a packet for the interaction to actually be handled
    // We don't return true, because that would tell Create's handler that it needs to process the interaction itself
    @ModifyReturnValue(method = "handlePlayerInteraction", at = @At(value = "RETURN", ordinal = 1))
    private boolean getCreative$onHandlePlayerInteraction(boolean hasInteraction,
                Player player, BlockPos localPos, Direction side, InteractionHand interactionHand)
    {
        if (hasInteraction) return true;

        // Player has interacted with the contraption without any interaction occurring -> try opening hinge bearing

        AbstractContraptionEntity thisInstance = (AbstractContraptionEntity)(Object)this;
        if (thisInstance instanceof ControlledContraptionEntity controlledContraption) {
            final BlockPos controllerPos = ((IControlledContraptionEntityAccessor)controlledContraption).getCreative$getControllerPos();
            final BlockEntity controller = player.level().getBlockEntity(controllerPos);
            if (controller instanceof HingeBearingBlockEntity) {
                CatnipServices.NETWORK.sendToServer(new HingeBearingInteractPacket(thisInstance, localPos));
                player.swing(interactionHand);
            }
        }

        return false;
    }
}
