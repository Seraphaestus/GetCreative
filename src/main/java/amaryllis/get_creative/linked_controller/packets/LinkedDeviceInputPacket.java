package amaryllis.get_creative.linked_controller.packets;

import amaryllis.get_creative.CustomPackets;
import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlockEntity;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LinkedDeviceInputPacket extends LinkedDevicePacketBase {

    public static final StreamCodec<ByteBuf, LinkedDeviceInputPacket> STREAM_CODEC = StreamCodec.composite(
            CatnipStreamCodecBuilders.list(ByteBufCodecs.INT), p -> p.activatedButtons,
            ByteBufCodecs.BOOL, p -> p.press,
            CatnipStreamCodecs.NULLABLE_BLOCK_POS, LinkedDevicePacketBase::getLecternPos,
            LinkedDeviceInputPacket::new
    );

    private final List<Integer> activatedButtons;
    private final boolean press;

    public LinkedDeviceInputPacket(Collection<Integer> activatedButtons, boolean press) {
        this(activatedButtons, press, null);
    }

    public LinkedDeviceInputPacket(Collection<Integer> activatedButtons, boolean press, BlockPos lecternPos) {
        super(lecternPos);
        this.activatedButtons = List.copyOf(activatedButtons);
        this.press = press;
    }

    @Override
    protected void handleLectern(ServerPlayer player, LecternDeviceBlockEntity lectern) {
        if (lectern.isUsedBy(player)) handleItem(player, lectern.getDevice());
    }

    @Override
    protected void handleItem(ServerPlayer player, ItemStack heldItem) {
        Level world = player.getCommandSenderWorld();
        UUID uniqueID = player.getUUID();
        BlockPos pos = player.blockPosition();

        if (player.isSpectator() && press) return;

        var device = (LinkedDeviceItem) heldItem.getItem();
        LinkedControllerServerHandler.receivePressed(world, pos, uniqueID, activatedButtons.stream()
                .map(i -> device.toFrequency(heldItem, i))
                .collect(Collectors.toList()), press);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CustomPackets.LINKED_DEVICE_INPUT;
    }
}

