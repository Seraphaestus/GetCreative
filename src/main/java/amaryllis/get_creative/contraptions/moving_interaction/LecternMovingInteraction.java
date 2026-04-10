package amaryllis.get_creative.contraptions.moving_interaction;

import amaryllis.get_creative.contraptions.CustomInteractionBehaviours;
import amaryllis.get_creative.linked_controller.AllLinkedDevices;
import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlock;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlockEntity;
import com.simibubi.create.*;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class LecternMovingInteraction extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos pos, AbstractContraptionEntity contraptionEntity) {
        final var stack = player.getMainHandItem();

        boolean isLinkedDevice = false;
        if (!stack.is(AllItems.LINKED_CONTROLLER)) {
            if (!(stack.getItem() instanceof LinkedDeviceItem)) return false;
            isLinkedDevice = true;
        }

        final Contraption contraption = contraptionEntity.getContraption();
        final StructureTemplate.StructureBlockInfo info = contraption.getBlocks().get(pos);
        final BlockState currentState = info.state();

        if (currentState.getValue(LecternBlock.HAS_BOOK)) return false;

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        AllSoundEvents.CONTROLLER_PUT.playFrom(player);

        final var state = (isLinkedDevice
                           ? LecternDeviceBlock.BLOCK.get().defaultBlockState()
                           : AllBlocks.LECTERN_CONTROLLER.getDefaultState())
                .setValue(LecternBlock.FACING, currentState.getValue(LecternBlock.FACING))
                .setValue(LecternBlock.POWERED, currentState.getValue(LecternBlock.POWERED));

        final var data = new CompoundTag();
        if (isLinkedDevice) data.putInt("DeviceID", AllLinkedDevices.getDeviceIndex((LinkedDeviceItem) stack.getItem()));
        final var controllerData = stack.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY);
        data.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, player.registryAccess(), controllerData).orElseThrow());

        final var blockEntity = isLinkedDevice
            ? new LecternDeviceBlockEntity(pos, state)
            : new LecternControllerBlockEntity(AllBlockEntityTypes.LECTERN_CONTROLLER.get(), pos, state);
        blockEntity.setLevel(player.level());
        setController(blockEntity, stack, isLinkedDevice);
        CustomInteractionBehaviours.replaceBlock(contraptionEntity, pos, state, data, blockEntity);

        return true;
    }

    protected void setController(BlockEntity blockEntity, ItemStack stack, boolean isLinkedDevice) {
        if (isLinkedDevice) {
            ((LecternDeviceBlockEntity) blockEntity).setController(stack);
        } else {
            ((LecternControllerBlockEntity) blockEntity).setController(stack);
        }
    }

}