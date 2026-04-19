package amaryllis.get_creative.precision_assembly;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber
public class ArmInteractionEventHandler {

    @SubscribeEvent
    private static void giveItemToMechanicalArm(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof ArmBlockEntity mechanicalArm)) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (((IMechanicalArm)mechanicalArm).getCreative$setHeldItem(stack.copy())) {
            stack.setCount(0);
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

}
