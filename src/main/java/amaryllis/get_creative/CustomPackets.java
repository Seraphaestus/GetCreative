package amaryllis.get_creative;

import amaryllis.get_creative.contraptions.hinge_bearing.HingeBearingInteractPacket;
import amaryllis.get_creative.linked_controller.packets.LinkedDeviceBindPacket;
import amaryllis.get_creative.linked_controller.packets.LinkedDeviceInputPacket;
import amaryllis.get_creative.linked_controller.packets.LinkedDeviceStopLecternPacket;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Locale;

public enum CustomPackets implements BasePacketPayload.PacketTypeProvider {
    // Client to Server
    HINGE_BEARING_INTERACT(HingeBearingInteractPacket.class, HingeBearingInteractPacket.STREAM_CODEC),
    LINKED_DEVICE_INPUT(LinkedDeviceInputPacket.class, LinkedDeviceInputPacket.STREAM_CODEC),
    LINKED_DEVICE_BIND(LinkedDeviceBindPacket.class, LinkedDeviceBindPacket.STREAM_CODEC),
    LINKED_DEVICE_USE_LECTERN(LinkedDeviceStopLecternPacket.class, LinkedDeviceStopLecternPacket.STREAM_CODEC)
    ;

    private final CatnipPacketRegistry.PacketType<?> type;

    <T extends BasePacketPayload> CustomPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        String name = this.name().toLowerCase(Locale.ROOT);
        this.type = new CatnipPacketRegistry.PacketType<>(
                new CustomPacketPayload.Type<>(GetCreative.ID(name)),
                clazz, codec
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) this.type.type();
    }

    public static void register() {
        CatnipPacketRegistry packetRegistry = new CatnipPacketRegistry(GetCreative.MOD_ID, 1);
        for (CustomPackets packet : CustomPackets.values()) {
            packetRegistry.registerPacket(packet.type);
        }
        packetRegistry.registerAllPackets();
    }

}
