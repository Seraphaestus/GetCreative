package amaryllis.get_creative.linked_controller;

import amaryllis.get_creative.linked_controller.base.LinkedDeviceClientHandler;
import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import amaryllis.get_creative.linked_controller.base.LinkedDeviceItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class LinkedDevicesClient {

    private static final Map<Integer, LinkedDeviceClientHandler> CLIENT_HANDLERS = new HashMap<>();
    private static final Map<Integer, LinkedDeviceItemRenderer> RENDERERS = new HashMap<>();

    public static LinkedDeviceClientHandler createClientHandler(int deviceIndex) {
        LinkedDeviceItem device = AllLinkedDevices.getDevice(deviceIndex);
        var clientHandler = new LinkedDeviceClientHandler(device);
        CLIENT_HANDLERS.put(deviceIndex, clientHandler);
        return clientHandler;
    }

    public static LinkedDeviceItemRenderer createRenderer(int deviceIndex) {
        LinkedDeviceItem device = AllLinkedDevices.getDevice(deviceIndex);
        var renderer = new LinkedDeviceItemRenderer(device);
        RENDERERS.put(deviceIndex, renderer);
        return renderer;
    }

    public static LinkedDeviceClientHandler getClientHandler(int index) {
        return CLIENT_HANDLERS.get(index);
    }
    public static LinkedDeviceClientHandler getClientHandler(LinkedDeviceItem device) {
        return CLIENT_HANDLERS.get(device.deviceIndex);
    }

    public static LinkedDeviceItemRenderer getRenderer(int index) {
        return RENDERERS.get(index);
    }
    public static LinkedDeviceItemRenderer getRenderer(LinkedDeviceItem device) {
        return RENDERERS.get(device.deviceIndex);
    }
}
