package amaryllis.get_creative.linked_controller;

import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class LinkHelper {

    public static boolean isDeviceBoundToLink(ItemStack heldItem, LinkBehaviour linkBehaviour) {
        if (linkBehaviour == null) return false;
        if (!(heldItem.getItem() instanceof LinkedDeviceItem deviceType)) return false;

        ItemStackHandler frequencyItems = deviceType.getFrequencyItems(heldItem);

        var frequency = LinkHelper.getFrequency(linkBehaviour);
        for (int idx = 0; idx < deviceType.buttonCount; idx++) {
            if (LinkHelper.hasFrequency(frequencyItems, idx, frequency)) return true;
        }
        return false;
    }
    public static int getLinkedButtonCount(ItemStack heldItem, LinkBehaviour linkBehaviour) {
        if (linkBehaviour == null) return 0;
        if (!(heldItem.getItem() instanceof LinkedDeviceItem deviceType)) return 0;

        ItemStackHandler frequencyItems = deviceType.getFrequencyItems(heldItem);

        int count = 0;
        var frequency = LinkHelper.getFrequency(linkBehaviour);
        for (int idx = 0; idx < deviceType.buttonCount; idx++) {
            if (LinkHelper.hasFrequency(frequencyItems, idx, frequency)) count += 1;
        }
        return count;
    }

    public static ItemStack[] getFrequency(LinkBehaviour linkBehaviour) {
        return new ItemStack[] {
                linkBehaviour.getNetworkKey().getFirst().getStack(),
                linkBehaviour.getNetworkKey().getSecond().getStack()
        };
    }

    public static boolean hasFrequency(ItemStackHandler frequencyItems, int buttonIndex, ItemStack[] frequency) {
        return hasFrequency(frequencyItems, buttonIndex, frequency[0], frequency[1]);
    }
    public static boolean hasFrequency(ItemStackHandler frequencyItems, int buttonIndex, ItemStack key1, ItemStack key2) {
        return frequencyItems.getStackInSlot(buttonIndex * 2    ).is(key1.getItem()) &&
                frequencyItems.getStackInSlot(buttonIndex * 2 + 1).is(key2.getItem());
    }

    public static void setFrequency(ItemStackHandler frequencyItems, int buttonIndex, ItemStack[] frequency) {
        setFrequency(frequencyItems, buttonIndex, frequency[0], frequency[1]);
    }
    public static void setFrequency(ItemStackHandler frequencyItems, int buttonIndex, ItemStack key1, ItemStack key2) {
        frequencyItems.setStackInSlot(buttonIndex * 2,     key1.copy());
        frequencyItems.setStackInSlot(buttonIndex * 2 + 1, key2.copy());
    }

    public static void clearFrequency(ItemStackHandler frequencyItems, int buttonIndex) {
        frequencyItems.setStackInSlot(buttonIndex * 2,     ItemStack.EMPTY);
        frequencyItems.setStackInSlot(buttonIndex * 2 + 1, ItemStack.EMPTY);
    }

}
