package amaryllis.get_creative.mixin.block_breaking;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.block_breaking.KineticBlockBreaking;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockBreakingMovementBehaviour.class)
public class KineticBlockBreakerActorMixin implements MovementBehaviour {

    @Overwrite
    protected float getBlockBreakingSpeed(MovementContext context) {
        float lowerLimit = 1 / 128f;
        if (context.contraption instanceof MountedContraption) lowerLimit = 1f;
        if (context.contraption instanceof CarriageContraption) lowerLimit = 2f;
        final float breakSpeed = Mth.clamp(Math.abs(context.getAnimationSpeed()) / 500f, lowerLimit, 16f);

        final Level level = context.world;
        final CompoundTag data = context.data;
        if (level == null || !data.contains("BreakingPos")) return breakSpeed;
        final BlockPos targetPos = NBTHelper.readBlockPos(data, "BreakingPos");
        return breakSpeed * KineticBlockBreaking.getBreakSpeedModifier(context.state.getBlock(), level.getBlockState(targetPos));
    }

}
