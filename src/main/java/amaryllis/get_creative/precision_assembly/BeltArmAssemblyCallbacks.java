package amaryllis.get_creative.precision_assembly;

import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BeltArmAssemblyCallbacks {

    static ProcessingResult onItemReceived(TransportedItemStack transported,
                                           TransportedItemStackHandlerBehaviour handler, ArmAssemblyBehaviour behaviour) {
        if (behaviour.mechanicalArm.getSpeed() == 0) return ProcessingResult.PASS;
        if (behaviour.running) return ProcessingResult.HOLD;
        if (!ArmAssembly.trySimulate(behaviour.mechanicalArm, transported))
            return ProcessingResult.PASS;

        behaviour.start();
        return ProcessingResult.HOLD;
    }

    static ProcessingResult whenItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler,
                                         ArmAssemblyBehaviour behaviour) {
        if (behaviour.mechanicalArm.getSpeed() == 0) return ProcessingResult.PASS;
        if (!behaviour.running) return ProcessingResult.PASS;
        if (behaviour.runningTicks != ArmAssemblyBehaviour.CYCLE / 2) return ProcessingResult.HOLD;

        ArrayList<ItemStack> results = new ArrayList<>();
        if (!ArmAssembly.tryProcess(behaviour.mechanicalArm, transported, results)) return ProcessingResult.PASS;

        boolean bulk = ArmAssembly.canProcessInBulk() || transported.stack.getCount() == 1;

        transported.clearFanProcessingData();

        List<TransportedItemStack> collect = results.stream()
                .map(stack -> {
                    TransportedItemStack copy = transported.copy();
                    boolean centered = BeltHelper.isItemUpright(stack);
                    copy.stack = stack;
                    copy.locked = true;
                    copy.angle = centered ? 180 : behaviour.getWorld().random.nextInt(360);
                    return copy;
                })
                .collect(Collectors.toList());

        if (bulk) {
            handler.handleProcessingOnItem(transported, collect.isEmpty()
                    ? TransportedResult.removeItem()
                    : TransportedResult.convertTo(collect));

        } else {
            TransportedItemStack left = transported.copy();
            left.stack.shrink(1);

            handler.handleProcessingOnItem(transported, collect.isEmpty()
                    ? TransportedResult.convertTo(left)
                    : TransportedResult.convertToAndLeaveHeld(collect, left));
        }

        behaviour.blockEntity.sendData();
        return ProcessingResult.HOLD;
    }

}
