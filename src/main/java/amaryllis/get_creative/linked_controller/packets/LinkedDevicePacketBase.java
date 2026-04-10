package amaryllis.get_creative.linked_controller.packets;

import javax.annotation.Nullable;

import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlockEntity;
import net.createmod.catnip.net.base.ServerboundPacketPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;


public abstract class LinkedDevicePacketBase implements ServerboundPacketPayload {
    @Nullable protected final BlockPos lecternPos;

    public LinkedDevicePacketBase(@Nullable BlockPos lecternPos) {
        this.lecternPos = lecternPos;
    }

    @Nullable public BlockPos getLecternPos() { return lecternPos; }

    @Override
    public void handle(ServerPlayer player) {
        if (this.lecternPos != null) {
            BlockEntity be = player.level().getBlockEntity(lecternPos);
            if (!(be instanceof LecternDeviceBlockEntity)) return;
            handleLectern(player, (LecternDeviceBlockEntity) be);
        } else {
            ItemStack controller = player.getMainHandItem();
            if (!(controller.getItem() instanceof LinkedDeviceItem)) {
                controller = player.getOffhandItem();
                if (!(controller.getItem() instanceof LinkedDeviceItem)) return;
            }
            handleItem(player, controller);
        }
    }

    protected abstract void handleItem(ServerPlayer player, ItemStack heldItem);
    protected abstract void handleLectern(ServerPlayer player, LecternDeviceBlockEntity lectern);
}
