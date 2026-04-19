package amaryllis.get_creative.mixin.precision_assembly;

import amaryllis.get_creative.precision_assembly.ArmAssembly;
import amaryllis.get_creative.precision_assembly.IArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointHandler;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Iterator;

@Mixin(ArmInteractionPointHandler.class)
public class ArmInteractionPointHandlerMixin {

    @Inject(method = "rightClickingBlocksSelectsThem", cancellable = true, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint;cycleMode()V",
            shift = At.Shift.AFTER))
    private static void overrideStatus(PlayerInteractEvent.RightClickBlock event, CallbackInfo callback) {
        if (event.getEntity() == null) return;

        ArmInteractionPoint selected = getSelected(event.getPos());

        boolean isAssemblyTarget = ((IArmInteractionPoint)selected).isAssemblyTarget();
        if (isAssemblyTarget) {
            BlockState state = event.getLevel().getBlockState(event.getPos());
            var blockName = CreateLang.blockName(state).style(ChatFormatting.WHITE).component();

            var message = Component.translatable(ArmAssembly.LANG_KEY, blockName).withColor(ArmAssembly.OUTLINE_COLOR);
            event.getEntity().displayClientMessage(message, true);

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            callback.cancel();
        }
    }

    @Overwrite
    private static void drawOutlines(Collection<ArmInteractionPoint> selection) {
        for (Iterator<ArmInteractionPoint> iterator = selection.iterator(); iterator.hasNext(); ) {
            ArmInteractionPoint point = iterator.next();

            if (!point.isValid()) {
                iterator.remove();
                continue;
            }

            Level level = point.getLevel();
            BlockPos pos = point.getPos();
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getShape(level, pos);
            if (shape.isEmpty()) continue;

            int color = ((IArmInteractionPoint)point).isAssemblyTarget() ? ArmAssembly.OUTLINE_COLOR : point.getMode().getColor();
            Outliner.getInstance().showAABB(point, shape.bounds().move(pos))
                    .colored(color)
                    .lineWidth(1 / 16f);
        }
    }

    @Shadow private static ArmInteractionPoint getSelected(BlockPos pos) { return null; }
    @Shadow private static void put(ArmInteractionPoint point) {}

}