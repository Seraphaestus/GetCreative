package amaryllis.get_creative.contraptions.moving_interaction;

import amaryllis.get_creative.contraptions.CustomInteractionBehaviours;
import amaryllis.get_creative.linked_controller.AllLinkedDevices;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class LecternDeviceMovingInteraction extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos pos, AbstractContraptionEntity contraptionEntity) {
        if (!player.getMainHandItem().isEmpty()) return false;

        final Contraption contraption = contraptionEntity.getContraption();
        final StructureTemplate.StructureBlockInfo info = contraption.getBlocks().get(pos);
        final BlockState currentState = info.state();

        final var data = contraption.getBlocks().get(pos).nbt();
        if (data == null) return false;

        if (data.contains("DeviceID"))
            player.setItemInHand(InteractionHand.MAIN_HAND, createLinkedDevice(player.level(), data));
        AllSoundEvents.CONTROLLER_TAKE.playOnServer(player.level(), player.blockPosition());

        BlockState state = Blocks.LECTERN.defaultBlockState()
                .setValue(LecternBlock.FACING, currentState.getValue(LecternBlock.FACING))
                .setValue(LecternBlock.POWERED, currentState.getValue(LecternBlock.POWERED));

        final var blockEntity = new LecternBlockEntity(pos, state);
        CustomInteractionBehaviours.replaceBlock(contraptionEntity, pos, state, data, blockEntity);

        return true;
    }

    private static ItemStack createLinkedDevice(Level level, CompoundTag data) {
        final var deviceID = data.getInt("DeviceID");
        final var controllerData = CatnipCodecUtils.decode(ItemContainerContents.CODEC, level.registryAccess(),
                data.get("ControllerData")).orElse(ItemContainerContents.EMPTY);
        final var stack = new ItemStack(AllLinkedDevices.getDevice(deviceID));
        stack.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, controllerData);
        return stack;
    }

}
