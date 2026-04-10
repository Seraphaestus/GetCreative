package amaryllis.get_creative.linked_controller.base;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.linked_controller.LecternControllerHandler;
import amaryllis.get_creative.linked_controller.LinkHelper;
import amaryllis.get_creative.linked_controller.LinkedDevicesClient;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.simibubi.create.AllBlocks.LECTERN_CONTROLLER;

public abstract class LinkedDeviceItem extends Item {

    protected static final PartialModel DEFAULT_POWERED = PartialModel.of(GetCreative.ID("create", "item/linked_controller/powered"));
    public final int deviceIndex;
    public final int buttonCount;

    public LinkedDeviceItem(Properties properties, int deviceIndex, int buttonCount) {
        super(properties);
        this.deviceIndex = deviceIndex;
        this.buttonCount = buttonCount;
    }

    // If true, using the item will automatically activate the first button, without entering active mode
    public boolean skipActivation() { return false; }
    public boolean disableMovementWhileActive() { return true; }
    public boolean canPlaceOnLectern() { return true; }

    @OnlyIn(Dist.CLIENT)
    public Object[] getBindMessageArguments() {
        final var buttons = LinkedDevicesClient.getClientHandler(deviceIndex).BUTTONS;
        final var output = new Object[buttons.size()];
        for (int i = 0; i < buttons.size(); i++) {
            output[i] = buttons.get(i).getTranslatedKey();
        }
        return output;
    }

    // equipProgress in [0, 1], handModifier either -1 or 1
    @OnlyIn(Dist.CLIENT)
    public void transformActiveHeldDevice(PoseTransformStack msr, float equipProgress, float handModifier) {
        msr.translate(0, equipProgress / 4, equipProgress / 4 * handModifier);
        msr.rotateYDegrees(equipProgress * -30 * handModifier);
        msr.rotateZDegrees(equipProgress * -30);
    }

    protected BakedModel getPoweredModel() { return DEFAULT_POWERED.get(); }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState target = level.getBlockState(pos);

        if (!player.mayBuild()) return use(level, player, context.getHand()).getResult();

        if (AllBlocks.REDSTONE_LINK.has(target)) {
            LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(level, pos, LinkBehaviour.TYPE);
            // Bind to redstone link
            if (!player.isShiftKeyDown()) {
                bindToRedstoneLink(level, player, context);
                return InteractionResult.SUCCESS;
            // Unbind from redstone link
            } else if (LinkHelper.isDeviceBoundToLink(stack, linkBehaviour)) {
                unbindFromRedstoneLink(level, player, context);
                return InteractionResult.SUCCESS;
            }
        }

        if (player.isShiftKeyDown()) {
            // Swap devices with Lectern Device Block
            if (target.is(LecternDeviceBlock.BLOCK.get()) && canPlaceOnLectern()) {
                if (!level.isClientSide) {
                    LecternDeviceBlock.getBlock().withBlockEntityDo(level, pos, be ->
                            be.swapControllers(stack, player, context.getHand(), target));
                }
                return InteractionResult.SUCCESS;
            }
            // Swap devices with Lectern Controller Block
            if (target.is(LECTERN_CONTROLLER.get()) && canPlaceOnLectern()) {
                if (!level.isClientSide) {
                    LECTERN_CONTROLLER.get().withBlockEntityDo(level, pos, be ->
                            LecternControllerHandler.swapControllerForDevice(level, pos, target, be, stack, player, context.getHand()));
                }
                return InteractionResult.SUCCESS;
            }
        } else {
            // Put device in Lectern
            if (target.is(Blocks.LECTERN) && !target.getValue(LecternBlock.HAS_BOOK) && canPlaceOnLectern()) {
                if (!level.isClientSide) {
                    ItemStack lecternStack = player.isCreative() ? stack.copy() : stack.split(1);
                    ((LecternDeviceBlock)LecternDeviceBlock.BLOCK.get()).replaceLectern(target, level, pos, lecternStack);
                }
                return InteractionResult.SUCCESS;
            }

            if (target.is(LecternDeviceBlock.BLOCK.get())) return InteractionResult.PASS;
        }

        return use(level, player, context.getHand()).getResult();
    }

    protected void bindToRedstoneLink(Level level, Player player, UseOnContext context) {
        if (level.isClientSide) CatnipServices.PLATFORM.executeOnClientOnly(() ->
                () -> this.toggleBindMode(context.getClickedPos()));
        player.getCooldowns().addCooldown(this, 2);
    }
    protected void unbindFromRedstoneLink(Level level, Player player, UseOnContext context) {
        if (level.isClientSide) CatnipServices.PLATFORM.executeOnClientOnly(() ->
                () -> this.unbind(context.getItemInHand(), context.getClickedPos()));
        player.getCooldowns().addCooldown(this, 2);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            if (level.isClientSide) CatnipServices.PLATFORM.executeOnClientOnly(() -> this::toggleActive);
            player.getCooldowns().addCooldown(this, 2);
        }

        return InteractionResultHolder.pass(heldItem);
    }

    public ItemStackHandler getFrequencyItems(ItemStack stack) {
        ItemStackHandler newInv = new ItemStackHandler(buttonCount * 2);
        if (!stack.is(this)) throw new IllegalArgumentException("Cannot get frequency items from non-controller: " + stack);
        if (!stack.has(AllDataComponents.LINKED_CONTROLLER_ITEMS)) return newInv;
        ItemHelper.fillItemStackHandler(stack.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY), newInv);
        return newInv;
    }

    public Couple<RedstoneLinkNetworkHandler.Frequency> toFrequency(ItemStack controller, int slot) {
        ItemStackHandler frequencyItems = getFrequencyItems(controller);
        return Couple.create(RedstoneLinkNetworkHandler.Frequency.of(frequencyItems.getStackInSlot(slot * 2)),
                RedstoneLinkNetworkHandler.Frequency.of(frequencyItems.getStackInSlot(slot * 2 + 1)));
    }

    @OnlyIn(Dist.CLIENT)
    protected void toggleBindMode(BlockPos pos) {
        LinkedDevicesClient.getClientHandler(deviceIndex).toggleBindMode(pos);
    }

    @OnlyIn(Dist.CLIENT)
    protected void toggleActive() {
        LinkedDevicesClient.getClientHandler(deviceIndex).toggle();
    }

    @OnlyIn(Dist.CLIENT)
    protected void unbind(ItemStack stack, BlockPos pos) {
        LinkedDevicesClient.getClientHandler(deviceIndex).unbindFromRedstoneLink(stack, pos);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, LinkedDevicesClient.createRenderer(deviceIndex)));
    }

}
