package amaryllis.get_creative.linked_controller.lectern;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.linked_controller.AllLinkedDevices;
import amaryllis.get_creative.linked_controller.LecternControllerHandler;
import amaryllis.get_creative.linked_controller.LinkedDevicesClient;
import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static amaryllis.get_creative.linked_controller.AllLinkedDevices.getDeviceIndex;
import static com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity.playerInRange;
import static com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity.playerIsUsingLectern;

public class LecternDeviceBlockEntity extends SmartBlockEntity {

    public static Supplier<BlockEntityType<LecternDeviceBlockEntity>> BLOCK_ENTITY;
    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "lectern_device", () -> BlockEntityType.Builder.of(
                        LecternDeviceBlockEntity::new, LecternDeviceBlock.BLOCK.get()
                ).build(null));
    }

    protected int deviceID = 0;
    protected ItemContainerContents controllerData = ItemContainerContents.EMPTY;
    protected UUID user;
    protected UUID prevUser;    // used only on client
    protected boolean deactivatedThisTick;    // used only on server

    public LecternDeviceBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("DeviceID", IntTag.valueOf(deviceID));
        compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, controllerData).orElseThrow());
        if (user != null) compound.putUUID("User", user);
    }

    @Override
    public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
        super.writeSafe(compound, registries);
        compound.put("DeviceID", IntTag.valueOf(deviceID));
        compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, controllerData).orElseThrow());
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        deviceID = compound.getInt("DeviceID");
        controllerData = CatnipCodecUtils.decode(ItemContainerContents.CODEC, registries, compound.get("ControllerData")).orElse(ItemContainerContents.EMPTY);
        user = compound.hasUUID("User") ? compound.getUUID("User") : null;
    }

    public ItemStack getDevice() {
        return createLinkedDevice();
    }

    public boolean hasUser() {
        return user != null;
    }

    public boolean isUsedBy(Player player) {
        return hasUser() && user.equals(player.getUUID());
    }

    public void tryStartUsing(Player player) {
        if (!deactivatedThisTick && !hasUser() && !playerIsUsingLectern(player) && playerInRange(player, level, worldPosition))
            startUsing(player);
    }

    public void tryStopUsing(Player player) {
        if (isUsedBy(player)) stopUsing(player);
    }

    private void startUsing(Player player) {
        user = player.getUUID();
        player.getPersistentData().putBoolean("IsUsingLecternController", true);
        sendData();
    }

    private void stopUsing(Player player) {
        user = null;
        if (player != null) player.getPersistentData().remove("IsUsingLecternController");
        deactivatedThisTick = true;
        sendData();
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            CatnipServices.PLATFORM.executeOnClientOnly(() -> this::tryToggleActive);
            prevUser = user;
        }

        if (!level.isClientSide) {
            deactivatedThisTick = false;

            if (!(level instanceof ServerLevel) || user == null) return;

            Entity entity = ((ServerLevel) level).getEntity(user);
            if (!(entity instanceof Player player)) {
                stopUsing(null);
                return;
            }

            if (!playerInRange(player, level, worldPosition) || !playerIsUsingLectern(player))
                stopUsing(player);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tryToggleActive() {
        final var clientHandler = LinkedDevicesClient.getClientHandler(deviceID);
        if (user == null && Minecraft.getInstance().player.getUUID().equals(prevUser)) {
            clientHandler.deactivateInLectern();
        } else if (prevUser == null && Minecraft.getInstance().player.getUUID().equals(user)) {
            clientHandler.activateInLectern(worldPosition);
        }
    }

    public void setController(ItemStack newController) {
        if (newController != null) {
            if (newController.getItem() instanceof LinkedDeviceItem linkedDevice) deviceID = getDeviceIndex(linkedDevice);
            controllerData = newController.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY);
            AllSoundEvents.CONTROLLER_PUT.playOnServer(level, worldPosition);
            sendData();
        }
    }

    public void nullifyController() {
        controllerData = LecternControllerHandler.NULL_DATA;
    }

    public void swapControllers(ItemStack stack, Player player, InteractionHand hand, BlockState state) {
        ItemStack newController = stack.copy();
        stack.setCount(0);
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, createLinkedDevice());
        } else {
            dropController(state);
        }
        setController(newController);
    }

    public void dropController(BlockState state) {
        if (controllerData == LecternControllerHandler.NULL_DATA) return;

        Entity playerEntity = ((ServerLevel) level).getEntity(user);
        if (playerEntity instanceof Player) stopUsing((Player) playerEntity);

        Direction dir = state.getValue(LecternControllerBlock.FACING);
        double x = worldPosition.getX() + 0.5 + 0.25 * dir.getStepX();
        double y = worldPosition.getY() + 1;
        double z = worldPosition.getZ() + 0.5 + 0.25 * dir.getStepZ();
        ItemEntity itementity = new ItemEntity(level, x, y, z, createLinkedDevice());
        itementity.setDefaultPickUpDelay();
        level.addFreshEntity(itementity);
        controllerData = ItemContainerContents.EMPTY;
    }

    public ItemStack createLinkedDevice() {
        var stack = new ItemStack(AllLinkedDevices.getDevice(deviceID));
        stack.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, controllerData);
        return stack;
    }
}
