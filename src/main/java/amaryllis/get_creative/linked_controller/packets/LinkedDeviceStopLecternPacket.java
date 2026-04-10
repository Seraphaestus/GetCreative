package amaryllis.get_creative.linked_controller.packets;

import amaryllis.get_creative.CustomPackets;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class LinkedDeviceStopLecternPacket extends LinkedDevicePacketBase {

    public static final StreamCodec<ByteBuf, LinkedDeviceStopLecternPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(
            LinkedDeviceStopLecternPacket::new, LinkedDevicePacketBase::getLecternPos
    );

    public LinkedDeviceStopLecternPacket(BlockPos lecternPos) {
        super(Objects.requireNonNull(lecternPos));
    }

    @Override
    protected void handleLectern(ServerPlayer player, LecternDeviceBlockEntity lectern) {
        lectern.tryStopUsing(player);
    }

    @Override
    protected void handleItem(ServerPlayer player, ItemStack heldItem) {}

    @Override
    public PacketTypeProvider getTypeProvider() { return CustomPackets.LINKED_DEVICE_USE_LECTERN; }
}
