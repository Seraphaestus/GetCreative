package amaryllis.get_creative.linked_controller.packets;

import amaryllis.get_creative.CustomPackets;
import amaryllis.get_creative.linked_controller.LinkHelper;
import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlockEntity;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class LinkedDeviceBindPacket extends LinkedDevicePacketBase {

    public static final StreamCodec<ByteBuf, LinkedDeviceBindPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.button,
            BlockPos.STREAM_CODEC, p -> p.linkLocation,
            ByteBufCodecs.BOOL, p -> p.unbind,
            LinkedDeviceBindPacket::new
    );

    protected final int button;
    protected final BlockPos linkLocation;
    protected final boolean unbind;

    public LinkedDeviceBindPacket(int button, BlockPos linkLocation, boolean unbind) {
        super(null);
        this.button = button;
        this.linkLocation = linkLocation;
        this.unbind = unbind;
    }
    public LinkedDeviceBindPacket(@Nullable BlockPos lecternPos) {
        super(lecternPos);
        // dummy values
        this.button = 0;
        this.linkLocation = null;
        this.unbind = true;
    }

    public static LinkedDeviceBindPacket Bind(int button, BlockPos linkLocation) {
        return new LinkedDeviceBindPacket(button, linkLocation, false);
    }
    public static LinkedDeviceBindPacket Unbind(BlockPos linkLocation) {
        return new LinkedDeviceBindPacket(0, linkLocation, true);
    }

    @Override
    protected void handleItem(ServerPlayer player, ItemStack heldItem) {
        if (player.isSpectator()) return;

        var device = (LinkedDeviceItem) heldItem.getItem();
        ItemStackHandler frequencyItems = device.getFrequencyItems(heldItem);
        LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(player.level(), linkLocation, LinkBehaviour.TYPE);
        if (linkBehaviour == null) return;

        var frequency = LinkHelper.getFrequency(linkBehaviour);
        if (unbind) {
            for (int idx = 0; idx < device.buttonCount; idx++) {
                if (LinkHelper.hasFrequency(frequencyItems, idx, frequency)) {
                    LinkHelper.clearFrequency(frequencyItems, idx);
                }
            }
        } else {
            LinkHelper.setFrequency(frequencyItems, button, frequency);
        }
        heldItem.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemHelper.containerContentsFromHandler(frequencyItems));
    }

    @Override
    protected void handleLectern(ServerPlayer player, LecternDeviceBlockEntity lectern) {}

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CustomPackets.LINKED_DEVICE_BIND;
    }
}