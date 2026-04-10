package amaryllis.get_creative.linked_controller;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class LinkedRemoteItem extends LinkedDeviceItem {

    public LinkedRemoteItem(Properties properties, int deviceIndex) {
        super(properties, deviceIndex, 1);
    }

    protected static final PartialModel POWERED = PartialModel.of(GetCreative.ID( "item/linked_remote_powered"));
    @Override protected BakedModel getPoweredModel() { return POWERED.get(); }

    @Override public boolean skipActivation() { return true; }
    @Override public boolean canPlaceOnLectern() { return false; }

    public static @Nullable BiConsumer<ItemStack, BlockPos> bindToRedstoneLink_ClientOnly;
    @Override protected void bindToRedstoneLink(Level level, Player player, UseOnContext context) {
        if (level.isClientSide && bindToRedstoneLink_ClientOnly != null) {
            bindToRedstoneLink_ClientOnly.accept(context.getItemInHand(), context.getClickedPos());
        }
        player.getCooldowns().addCooldown(this, 2);
    }

}