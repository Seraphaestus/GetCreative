package amaryllis.get_creative.linked_controller.base;

import amaryllis.get_creative.linked_controller.AllLinkedDevices;
import amaryllis.get_creative.linked_controller.LinkHelper;
import amaryllis.get_creative.linked_controller.LinkedDevicesClient;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlock;
import amaryllis.get_creative.linked_controller.packets.LinkedDeviceBindPacket;
import amaryllis.get_creative.linked_controller.packets.LinkedDeviceInputPacket;
import amaryllis.get_creative.linked_controller.packets.LinkedDeviceStopLecternPacket;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.controller.*;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler.Mode;
import static com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler.PACKET_RATE;

@OnlyIn(Dist.CLIENT)
public class LinkedDeviceClientHandler {

    protected LinkedDeviceItem linkedDeviceItem;

    public final static List<KeyMapping> MOVEMENT_KEYS = new ArrayList<>(6);
    public final List<LinkedDeviceButton> BUTTONS = new ArrayList<>();
    static {
        final var options = Minecraft.getInstance().options;
        MOVEMENT_KEYS.addAll(List.of(
                options.keyUp, options.keyLeft, options.keyDown, options.keyRight,
                options.keyShift, options.keyJump
        ));
    }

    public static final LayeredDraw.Layer OVERLAY = LinkedDeviceClientHandler::renderOverlay;

    public LinkedControllerClientHandler.Mode MODE = Mode.IDLE;
    public Collection<Integer> currentlyPressed = new HashSet<>();
    protected BlockPos lecternPos;
    protected BlockPos selectedLocation = BlockPos.ZERO;
    protected int packetCooldown;

    protected String langKey;
    protected int defaultPressCooldown;

    public LinkedDeviceClientHandler(LinkedDeviceItem item_instance) {
        linkedDeviceItem = item_instance;
    }

    public void addButton(KeyMapping key, float posX, float posY, float posZ) {
        BUTTONS.add(new LinkedDeviceButton(key, posX, posY, posZ, LinkedDeviceButton.DEFAULT));
    }
    public void addAlternativeButton(KeyMapping key, float posX, float posY, float posZ) {
        BUTTONS.add(new LinkedDeviceButton(key, posX, posY, posZ, LinkedDeviceButton.ALTERNATIVE));
    }

    public void toggleBindMode(BlockPos location) {
        if (MODE == Mode.IDLE) {
            MODE = Mode.BIND;
            selectedLocation = location;
        } else {
            onReset();
        }
    }

    protected boolean trySkipActivation() {
        if (!linkedDeviceItem.skipActivation()) return false;

        currentlyPressed = new ArrayList<>(List.of(0));
        CatnipServices.NETWORK.sendToServer(new LinkedDeviceInputPacket(currentlyPressed, true));
        defaultPressCooldown = 5; // 4 ticks, 2 redstone ticks

        return true;
    }

    public void toggle() {
        if (MODE != Mode.IDLE) {
            onReset();
            return;
        }

        if (trySkipActivation()) return;

        MODE = Mode.ACTIVE;
        lecternPos = null;
    }

    public void activateInLectern(BlockPos lecternAt) {
        if (MODE != Mode.IDLE) return;

        if (trySkipActivation()) return;

        MODE = Mode.ACTIVE;
        lecternPos = lecternAt;
    }

    public void deactivateInLectern() {
        if (MODE != Mode.ACTIVE || !inLectern()) return;
        onReset();
    }

    public boolean inLectern() {
        return lecternPos != null;
    }

    protected void onReset() {
        MODE = Mode.IDLE;

        // This line was causing an issue where movement-input-eating had no effect; seems to work fine without it
        //ControlsUtil.getControls().forEach(kb -> kb.setDown(ControlsUtil.isActuallyPressed(kb)));
        packetCooldown = 0;
        selectedLocation = BlockPos.ZERO;

        if (inLectern()) CatnipServices.NETWORK.sendToServer(new LinkedDeviceStopLecternPacket(lecternPos));
        lecternPos = null;

        if (!currentlyPressed.isEmpty())
            CatnipServices.NETWORK.sendToServer(new LinkedDeviceInputPacket(currentlyPressed, false));
        currentlyPressed.clear();

        LinkedDevicesClient.getRenderer(linkedDeviceItem).resetButtons();
    }

    public void tick() {
        LinkedDevicesClient.getRenderer(linkedDeviceItem).tick();

        final boolean forceActive = linkedDeviceItem.skipActivation();

        if (MODE == Mode.IDLE && !forceActive) return;
        if (packetCooldown > 0) packetCooldown--;
        if (defaultPressCooldown > -2) defaultPressCooldown--; // 2 extra ticks to keep the powered model visible for

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        ItemStack heldItem = player.getMainHandItem();

        if (player.isSpectator()) {
            onReset();
            return;
        }

        if (!inLectern() && !heldItem.is(linkedDeviceItem)) {
            heldItem = player.getOffhandItem();
            if (!heldItem.is(linkedDeviceItem)) {
                onReset();
                return;
            }
        }

        if (inLectern() && ((LecternDeviceBlock) LecternDeviceBlock.BLOCK.get())
                .getBlockEntityOptional(mc.level, lecternPos)
                .map(be -> !be.isUsedBy(mc.player))
                .orElse(true)) {
            deactivateInLectern();
            return;
        }

        if (mc.screen != null) {
            onReset();
            return;
        }

        if (InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
            onReset();
            return;
        }

        List<LinkedDeviceButton> buttons = LinkedDevicesClient.getClientHandler(linkedDeviceItem).BUTTONS;
        List<KeyMapping> controls = new ArrayList<>(buttons.size());

        Collection<Integer> pressedKeys = new HashSet<>();
        if (defaultPressCooldown > 0) {
            pressedKeys.add(0);
        } else {
            for (int i = 0; i < buttons.size(); i++) {
                controls.add(buttons.get(i).key());
                if (i == 0 && linkedDeviceItem.skipActivation()) continue;
                if (ControlsUtil.isActuallyPressed(controls.get(i))) {
                    while (controls.get(i).consumeClick());
                    pressedKeys.add(i);
                }
            }
        }

        Collection<Integer> newKeys = new HashSet<>(pressedKeys);
        Collection<Integer> releasedKeys = currentlyPressed;
        newKeys.removeAll(releasedKeys);
        releasedKeys.removeAll(pressedKeys);

        if (MODE == Mode.ACTIVE || (MODE == Mode.IDLE && forceActive)) {
            // Released Keys
            if (!releasedKeys.isEmpty()) {
                CatnipServices.NETWORK.sendToServer(new LinkedDeviceInputPacket(releasedKeys, false, lecternPos));
                AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1f, .5f, true);
            }

            // Newly Pressed Keys
            if (!newKeys.isEmpty()) {
                CatnipServices.NETWORK.sendToServer(new LinkedDeviceInputPacket(newKeys, true, lecternPos));
                packetCooldown = PACKET_RATE;
                AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1f, .75f, true);
            }

            // Keepalive Pressed Keys
            if (packetCooldown == 0) {
                if (!pressedKeys.isEmpty()) {
                    CatnipServices.NETWORK.sendToServer(new LinkedDeviceInputPacket(pressedKeys, true, lecternPos));
                    packetCooldown = PACKET_RATE;
                }
            }
        }

        if (MODE == Mode.BIND) {
            VoxelShape shape = mc.level.getBlockState(selectedLocation).getShape(mc.level, selectedLocation);
            if (!shape.isEmpty())
                Outliner.getInstance().showAABB("controller", shape.bounds()
                                .move(selectedLocation))
                        .colored(0xB73C2D)
                        .lineWidth(1 / 16f);

            for (Integer keyIndex: newKeys) {
                bindToRedstoneLink(selectedLocation, keyIndex, controls.get(keyIndex));
                MODE = Mode.IDLE;
                break;
            }
        }

        currentlyPressed = pressedKeys;
        controls.forEach(key -> key.setDown(false));

        if (forceActive) return;

        // Block key inputs for any KeyMappings bound to the same key as an active button
        controls.forEach(buttonKey -> {
            Arrays.stream(mc.options.keyMappings)
                    .filter(key -> key.getKey().getValue() == buttonKey.getKey().getValue())
                    .forEach(key -> {
                        if (ControlsUtil.isActuallyPressed(key)) { while (key.consumeClick()); }
                        key.setDown(false);
                    });
        });

        // Block movement key inputs
        if (linkedDeviceItem.disableMovementWhileActive()) {
            MOVEMENT_KEYS.forEach(key -> {
                if (ControlsUtil.isActuallyPressed(key)) { while (key.consumeClick()); }
                key.setDown(false);
            });
        }
    }

    public void bindToRedstoneLink(BlockPos linkPos, int buttonIndex, KeyMapping key) {
        final var mc = Minecraft.getInstance();
        LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(mc.level, linkPos, LinkBehaviour.TYPE);
        if (linkBehaviour == null) return;

        CatnipServices.NETWORK.sendToServer(LinkedDeviceBindPacket.Bind(buttonIndex, linkPos));
        CreateLang.translate("linked_controller.key_bound", key.getTranslatedKeyMessage().getString()).sendStatus(mc.player);
    }

    public void unbindFromRedstoneLink(ItemStack heldItem, BlockPos linkPos) {
        final var mc = Minecraft.getInstance();
        LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(mc.level, linkPos, LinkBehaviour.TYPE);
        int linkedButtonCount = LinkHelper.getLinkedButtonCount(heldItem, linkBehaviour);
        if (linkedButtonCount <= 0) return;

        CatnipServices.NETWORK.sendToServer(LinkedDeviceBindPacket.Unbind(linkPos));
        var message = (linkedButtonCount > 1)
                ? Component.translatable("get_creative.linked_device.key_unbound", linkedButtonCount)
                : Component.translatable("get_creative.linked_device.key_unbound.single",
                        Component.translatable(heldItem.getDescriptionId()));
        mc.player.displayClientMessage(message, true);
    }

    public static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (Minecraft.getInstance().options.hideGui) return;

        for (int deviceID = 0; deviceID < AllLinkedDevices.BY_INDEX.size(); deviceID++) {
            var clientHandler = LinkedDevicesClient.getClientHandler(deviceID);
            if (clientHandler.MODE == Mode.BIND) {
                clientHandler.renderBindOverlay(guiGraphics);
                break;
            }
        }
    }

    public void renderBindOverlay(GuiGraphics guiGraphics) {
        final int width1 = guiGraphics.guiWidth();
        final int height1 = guiGraphics.guiHeight();
        final var mc = Minecraft.getInstance();

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        Screen tooltipScreen = new Screen(CommonComponents.EMPTY) {};
        tooltipScreen.init(mc, width1, height1);

        Object[] keys = linkedDeviceItem.getBindMessageArguments();

        if (langKey == null) langKey = BuiltInRegistries.ITEM.getKey(linkedDeviceItem).toLanguageKey();

        List<Component> list = new ArrayList<>();
        list.add(CreateLang.translateDirect("linked_controller.bind_mode").withStyle(ChatFormatting.GOLD));
        list.addAll(TooltipHelper.cutTextComponent(Component.translatable(langKey + ".press_keybind", keys), FontHelper.Palette.ALL_GRAY));

        int width = 0;
        int height = list.size() * mc.font.lineHeight;
        for (Component iTextComponent : list)
            width = Math.max(width, mc.font.width(iTextComponent));
        int x = (width1 / 3) - width / 2;
        int y = height1 - height - 24;

        guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, list, x, y);

        poseStack.popPose();
    }

}