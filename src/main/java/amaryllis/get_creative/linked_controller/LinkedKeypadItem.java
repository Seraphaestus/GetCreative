package amaryllis.get_creative.linked_controller;

import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LinkedKeypadItem extends LinkedDeviceItem {

    public LinkedKeypadItem(Properties properties, int deviceIndex) {
        super(properties, deviceIndex, 11);
    }

    //            < -- X -- >
    //      0 1 2 3 4 5 6 7 8 9 A B C
    //    0 . . . . . . . . . . . . .
    // ^  1 . . 1 1 2 2 3 3 . . . . .
    // |  2 . . @ 1 @ 2 @ 3 . X X . .
    // Z  3 . . 4 4 5 5 6 6 . @ X . .
    // |  4 . . @ 4 @ 5 @ 6 . # # . .
    // v  5 . . 7 7 8 8 9 9 . @ # . .
    //    6 . . @ 7 @ 8 @ 9 . . . . .
    //    7 . . . . . . . . . . . . .

    @Override
    @OnlyIn(Dist.CLIENT)
    public Object[] getBindMessageArguments() {
        var BUTTONS = LinkedDevicesClient.getClientHandler(deviceIndex).BUTTONS;
        return new Object[] {
            BUTTONS.get(0).getTranslatedKey(),
            BUTTONS.get(8).getTranslatedKey(),
            BUTTONS.get(9).getTranslatedKey(),
            BUTTONS.get(10).getTranslatedKey()
        };
    }
}
