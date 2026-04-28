package amaryllis.get_creative.mixin.compat.sable;

import amaryllis.get_creative.industrial_fan.IndustrialFanBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EncasedFanBlockEntity.class)
public class IndustrialFanThrust extends KineticBlockEntity {

    @Shadow boolean sable$blocked;
    @Unique private int getCreative$blockedCount;

    public IndustrialFanThrust(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Overrides BlockEntitySubLevelActor::sable$tick implemented in EncasedFanBlockEntity via Sable mixin
    @Inject(method = "sable$tick", at = @At("HEAD"), cancellable = true)
    public void updateBlockage(final ServerSubLevel subLevel, CallbackInfo cbi) {
        if (!((Object)this instanceof IndustrialFanBlockEntity industrialFan)) return;

        getCreative$blockedCount = 0;
        industrialFan.getFacingBlocks().forEach(pos -> {
            if (!level.getBlockState(pos).isAir()) getCreative$blockedCount += 1;
        });

        sable$blocked = (getCreative$blockedCount == 9);
        cbi.cancel();
    }

    @ModifyReturnValue(method = "sable$getPropSpeed", at = @At("RETURN"))
    private float modifyThrust(float original) {
        if (!((Object)this instanceof IndustrialFanBlockEntity)) return original;

        return original * (9 - getCreative$blockedCount) / 3f;
    }

}
