package amaryllis.get_creative.contraptions.moving_interaction;

import amaryllis.get_creative.contraptions.hinge_bearing.HandleBlock;
import amaryllis.get_creative.contraptions.hinge_bearing.HingeBearingBlockEntity;
import amaryllis.get_creative.contraptions.hinge_bearing.IControlledContraptionEntityAccessor;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class HandleMovingInteraction extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos pos, AbstractContraptionEntity contraption) {
        if (!(contraption instanceof ControlledContraptionEntity controlledContraption)) return false;

        final BlockPos controllerPos = ((IControlledContraptionEntityAccessor)controlledContraption).getCreative$getControllerPos();
        final BlockEntity controller = player.level().getBlockEntity(controllerPos);
        if (!(controller instanceof HingeBearingBlockEntity hingeBearing)) return false;

        if (!player.level().isClientSide) {
            hingeBearing.openDoor(player, contraption, pos);

            final var handleInfo = contraption.getContraption().getBlocks().get(pos);
            final Block block = (handleInfo != null) ? handleInfo.state().getBlock() : null;
            final var material = (block instanceof HandleBlock handleBlock) ? handleBlock.material : HandleBlock.Material.WOODEN;
            hingeBearing.playSound(material);
        }

        return true;
    }

}
