package amaryllis.get_creative.linked_controller;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.GetCreativeClient;
import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

import static amaryllis.get_creative.linked_controller.base.LinkedDeviceItemRenderer.fromRight;

public class AllLinkedDevices {

    public static final List<DeferredItem> BY_INDEX = new ArrayList<>();

    private static final int LINKED_REMOTE = 0;
    private static final int LINKED_KEYPAD = 1;
    private static final int MACRO_CONTROLLER = 2;

    public static void register() {
        createDeviceItem("linked_remote", LinkedRemoteItem::new);
        createDeviceItem("linked_keypad", LinkedKeypadItem::new);
        createDeviceItem("linked_macro_controller", LinkedMacroControllerItem::new);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerClient() {
        for (int deviceIndex = 0; deviceIndex < 3; deviceIndex++) {
            LinkedDevicesClient.createClientHandler(deviceIndex);
        }

        final var options = Minecraft.getInstance().options;

        // Do not make a button tied to ESC or RMB, which are reserved for quitting active mode
        // Unless using skipActivation, in which case do use RMB for the first button

        // Linked Remote
        var clientHandler = LinkedDevicesClient.getClientHandler(LINKED_REMOTE);
        clientHandler.addButton(options.keyUse, fromRight(-4), 0, 3);

        // Linked Keypad
        clientHandler = LinkedDevicesClient.getClientHandler(LINKED_KEYPAD);
        clientHandler.addButton(options.keyHotbarSlots[0], 2, 0, 2);
        clientHandler.addAlternativeButton(options.keyHotbarSlots[1], 4, 0, 2);
        clientHandler.addButton(options.keyHotbarSlots[2], 6, 0, 2);
        clientHandler.addAlternativeButton(options.keyHotbarSlots[3], 2, 0, 4);
        clientHandler.addButton(options.keyHotbarSlots[4], 4, 0, 4);
        clientHandler.addAlternativeButton(options.keyHotbarSlots[5], 6, 0, 4);
        clientHandler.addButton(options.keyHotbarSlots[6], 2, 0, 6);
        clientHandler.addAlternativeButton(options.keyHotbarSlots[7], 4, 0, 6);
        clientHandler.addButton(options.keyHotbarSlots[8], 6, 0, 6);
        clientHandler.addButton(options.keyJump,  fromRight(-3), 0, 3);
        clientHandler.addButton(options.keyShift, fromRight(-3), 0, 5);

        // Macro Controller
        clientHandler = LinkedDevicesClient.getClientHandler(MACRO_CONTROLLER);
        clientHandler.addButton(options.keyUp, 2, 0, 2 - 1.5f);
        clientHandler.addButton(options.keyLeft, 0, 0, 4 - 1.5f);
        clientHandler.addButton(options.keyDown, 2, 0, 6 - 1.5f);
        clientHandler.addButton(options.keyRight, 4, 0, 4 - 1.5f);
        clientHandler.addButton(GetCreativeClient.ARROW_UP.get(), 9, 0, 2 - 1.5f);
        clientHandler.addButton(GetCreativeClient.ARROW_LEFT.get(), 7, 0, 4 - 1.5f);
        clientHandler.addButton(GetCreativeClient.ARROW_DOWN.get(), 9, 0, 6 - 1.5f);
        clientHandler.addButton(GetCreativeClient.ARROW_RIGHT.get(), 11, 0, 4 - 1.5f);
        clientHandler.addAlternativeButton(GetCreativeClient.EXTRA_1.get(), -0.5f, 0, 8 - 1.5f);
        clientHandler.addButton(GetCreativeClient.EXTRA_2.get(), 1.5f, 0, 9 - 1.5f);
        clientHandler.addButton(GetCreativeClient.EXTRA_3.get(), 9.5f, 0, 9 - 1.5f);
        clientHandler.addAlternativeButton(GetCreativeClient.EXTRA_4.get(), 11.5f, 0, 8 - 1.5f);
        clientHandler.addButton(options.keyJump, 4.5f, 0, 7 - 1.5f);
        clientHandler.addButton(options.keyShift, 6.5f, 0, 7 - 1.5f);

        LinkedRemoteItem.bindToRedstoneLink_ClientOnly = AllLinkedDevices::bindLinkedRemote;
    }
    @OnlyIn(Dist.CLIENT)
    private static void bindLinkedRemote(ItemStack heldItem, BlockPos pos) {
        var clientHandler = LinkedDevicesClient.getClientHandler(LINKED_REMOTE);
        clientHandler.bindToRedstoneLink(pos, 0, clientHandler.BUTTONS.getFirst().key());
    }

    protected static <T extends Item> void createDeviceItem(String id, NonNullBiFunction<Item.Properties, Integer, T> constructor) {
        final int deviceIndex = BY_INDEX.size();
        final var item = GetCreative.ITEMS.registerItem(id,
                (properties) -> constructor.apply(properties, deviceIndex),
                new Item.Properties().stacksTo(1));
        BY_INDEX.add(item);
    }

    public static LinkedDeviceItem getDevice(int index) {
        return (LinkedDeviceItem) BY_INDEX.get(index).asItem();
    }
    public static int getDeviceIndex(LinkedDeviceItem deviceItem) {
        for (int i = 0; i < BY_INDEX.size(); i++) {
            if (BY_INDEX.get(i).asItem().equals(deviceItem)) return i;
        }
        return -1;
    }

    @OnlyIn(Dist.CLIENT)
    public static void onTick() {
        AllLinkedDevices.BY_INDEX.forEach(deferredItem -> {
            LinkedDevicesClient.getClientHandler((LinkedDeviceItem) deferredItem.asItem()).tick();
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void onClickInput() {
        AllLinkedDevices.BY_INDEX.forEach(deferredItem -> {
            LinkedDevicesClient.getClientHandler((LinkedDeviceItem) deferredItem.asItem()).deactivateInLectern();
        });
    }
}
