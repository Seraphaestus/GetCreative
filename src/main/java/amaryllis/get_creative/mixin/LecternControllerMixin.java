package amaryllis.get_creative.mixin;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.linked_controller.LecternControllerHandler;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LecternControllerBlockEntity.class)
public class LecternControllerMixin extends SmartBlockEntity implements LecternControllerHandler.ILecternController {

    @Shadow ItemContainerContents controllerData;

    public LecternControllerMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void nullifyController() {
        controllerData = LecternControllerHandler.NULL_DATA;
    }

    @Overwrite
    public static boolean playerInRange(Player player, Level world, BlockPos pos) {
        double reach = Config.LECTERN_CONTROLLER_REACH.get() * player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        return player.distanceToSqr(Vec3.atCenterOf(pos)) < reach * reach;
    }

    @Inject(method = "dropController", at = @At("HEAD"), cancellable = true)
    private void dropController(CallbackInfo callback) {
        if (controllerData == LecternControllerHandler.NULL_DATA) callback.cancel();
    }

    @Shadow public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
}
