package amaryllis.get_creative.mixin.compat.sable;

import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlock;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlockEntity;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LecternDeviceBlock.class)
public class LecternDevicePatch implements BlockSubLevelAssemblyListener {

    @Override
    public void afterMove(final ServerLevel originLevel, final ServerLevel resultingLevel, final BlockState newState, final BlockPos oldPos, final BlockPos newPos) {
        if (originLevel.getBlockEntity(oldPos) instanceof final LecternDeviceBlockEntity be) {
            be.nullifyController();
        }
    }
}
