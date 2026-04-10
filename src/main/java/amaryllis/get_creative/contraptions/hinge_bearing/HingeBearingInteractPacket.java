package amaryllis.get_creative.contraptions.hinge_bearing;

import amaryllis.get_creative.CustomPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record HingeBearingInteractPacket(int contraptionID, BlockPos interactPos) implements ServerboundPacketPayload {

    public static final StreamCodec<ByteBuf, HingeBearingInteractPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.contraptionID,
            CatnipStreamCodecs.NULLABLE_BLOCK_POS, p -> p.interactPos,
            HingeBearingInteractPacket::new
    );

    public HingeBearingInteractPacket(AbstractContraptionEntity contraptionID, BlockPos interactPos) {
        this(contraptionID.getId(), interactPos);
    }

    public AbstractContraptionEntity getContraption(Level level) {
        Entity entity = level.getEntity(contraptionID);
        if (entity instanceof AbstractContraptionEntity contraption) return contraption;
        return null;
    }

    @Override
    public void handle(ServerPlayer player) {
        AbstractContraptionEntity contraption = getContraption(player.level());
        if (contraption == null || !(contraption instanceof ControlledContraptionEntity controlledContraption)) return;

        final BlockPos controllerPos = ((IControlledContraptionEntityAccessor)controlledContraption).getCreative$getControllerPos();
        final BlockEntity controller = player.level().getBlockEntity(controllerPos);
        if (!(controller instanceof HingeBearingBlockEntity hingeBearing)) return;

        hingeBearing.openDoor(player, contraption, interactPos);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CustomPackets.HINGE_BEARING_INTERACT;
    }
}

