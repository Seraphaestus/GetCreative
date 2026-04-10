package amaryllis.get_creative.mixin.block_breaking;

import amaryllis.get_creative.Config;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SawBlockEntity.class)
public class SawMixin extends BlockBreakingKineticBlockEntity {

    public SawMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    @Inject(method = "onBlockBroken", at = @At("Head"), cancellable = true)
    private void getCreative$onBlockBroken(BlockState stateToBreak, CallbackInfo callback) {
        if (Config.SAW_CAN_MUTLIBREAK.isFalse()) {
            super.onBlockBroken(stateToBreak);
            callback.cancel();
        }
    }

    // TODO: consider return super.canBreak(world, breakingPos, state) && !state.getCollisionShape(world, breakingPos)
    //			.isEmpty() && !AllTags.AllBlockTags.TRACKS.matches(state);

    @Inject(method = "isSawable", at = @At("Head"), cancellable = true)
    private static void getCreative$isSawable(BlockState stateToBreak, CallbackInfoReturnable<Boolean> callback) {
        if (Config.SAW_CAN_BREAK_ALL_BLOCKS.isTrue()) {
            if (stateToBreak.is(BlockTags.SAPLINGS)) {
                callback.setReturnValue(false);
            } else {
                callback.setReturnValue(true);
            }
        }
    }

    @Shadow
    protected BlockPos getBreakingPos() { return null; }
}
