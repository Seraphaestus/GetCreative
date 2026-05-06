package amaryllis.get_creative.mixin.gramophone;

import amaryllis.get_creative.appliances.gramophone.GramophoneBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.turntable.TurntableBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(TurntableBlockEntity.class)
public class TurntableMixin extends KineticBlockEntity {

    public TurntableMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (level == null) return added;

        if (!(level.getBlockEntity(getBlockPos().above()) instanceof GramophoneBlockEntity gramophone)) return added;
        if (!gramophone.showGoggleTooltip()) return added;

        CreateLang.builder()
                .add(Component.translatable("tooltip.get_creative.gramophone"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip, 0);

        gramophone.addGoggleTooltipBody(tooltip, isPlayerSneaking);
        return true;
    }

}
