package amaryllis.get_creative.linked_controller;

import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlock;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlockEntity;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

import static com.simibubi.create.AllBlocks.LECTERN_CONTROLLER;
import static com.simibubi.create.AllItems.LINKED_CONTROLLER;

@EventBusSubscriber
public class LecternControllerHandler {

    public static ItemContainerContents NULL_DATA = ItemContainerContents.fromItems(List.of(new ItemStack(Items.STICK)));

    @SubscribeEvent
    public static void onUseController(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(LINKED_CONTROLLER)) return;

        Player player = event.getEntity();
        if (player == null || !player.mayBuild()) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState target = level.getBlockState(pos);

        // Swap devices with Lectern Device Block
        if (player.isShiftKeyDown() && target.is(LecternDeviceBlock.BLOCK.get())) {
            if (!level.isClientSide) {
                LecternDeviceBlock.getBlock().withBlockEntityDo(level, pos, be ->
                        swapDeviceForController(level, pos, target, be, stack, player, event.getHand()));
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void swapControllerForDevice(Level level, BlockPos pos, BlockState state, LecternControllerBlockEntity blockEntity,
                                               ItemStack stack, Player player, InteractionHand hand) {
        ItemStack newDevice = stack.copy();
        stack.setCount(0);
        if (player.getItemInHand(hand).isEmpty()) {
            ItemStack prevController = createLinkedControllerFrom(blockEntity);
            player.setItemInHand(hand, prevController);
            ((ILecternController)blockEntity).nullifyController();
        } else {
            blockEntity.dropController(state);
        }
        LecternDeviceBlock.getBlock().replaceLectern(state, level, pos, newDevice);
    }

    public static void swapDeviceForController(Level level, BlockPos pos, BlockState state, LecternDeviceBlockEntity blockEntity,
                                               ItemStack stack, Player player, InteractionHand hand) {
        ItemStack newController = stack.copy();
        stack.setCount(0);
        if (player.getItemInHand(hand).isEmpty()) {
            ItemStack prevDevice = blockEntity.createLinkedDevice();
            player.setItemInHand(hand, prevDevice);
            blockEntity.nullifyController();
        } else {
            blockEntity.dropController(state);
        }
        LECTERN_CONTROLLER.get().replaceLectern(state, level, pos, newController);
    }

    public static ItemStack createLinkedControllerFrom(LecternControllerBlockEntity blockEntity) {
        ItemStack output = LINKED_CONTROLLER.asStack();
        output.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, getControllerData(blockEntity));
        return output;
    }

    public static ItemContainerContents getControllerData(LecternControllerBlockEntity blockEntity) {
        var registryAccess = blockEntity.getLevel().registryAccess();
        CompoundTag data = new CompoundTag();
        blockEntity.writeSafe(data, registryAccess);
        return CatnipCodecUtils.decode(ItemContainerContents.CODEC, registryAccess, data.get("ControllerData"))
                .orElse(ItemContainerContents.EMPTY);
    }

    public interface ILecternController {
        void nullifyController();
    }
}
