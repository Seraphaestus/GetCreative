package amaryllis.get_creative.mixin.block_breaking;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.block_breaking.KineticBlockBreaking;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockBreakingKineticBlockEntity.class)
public abstract class KineticBlockBreakerMixin extends KineticBlockEntity {

    public KineticBlockBreakerMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) { super(typeIn, pos, state); }

    @Shadow
    private BlockPos getBreakingPos() { return null; }

    @ModifyVariable(method = "tick", name = "breakSpeed", at = @At(value = "Store", ordinal = 0))
    private float getCreative$modifyBreakSpeed(float breakSpeed) {
        final BlockPos breakingPos = getBreakingPos();
        if (level == null || breakingPos == null) return breakSpeed;
        return breakSpeed * KineticBlockBreaking.getBreakSpeedModifier(getBlockState().getBlock(), level.getBlockState(breakingPos));
    }
}
