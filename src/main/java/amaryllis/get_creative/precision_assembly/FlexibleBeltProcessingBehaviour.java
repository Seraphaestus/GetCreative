package amaryllis.get_creative.precision_assembly;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class FlexibleBeltProcessingBehaviour extends BeltProcessingBehaviour {

    public static Vec3i STANDARD_OFFSET = new Vec3i(0, 2, 0);
    public static ArrayList<Vec3i> ADJACENT_OFFSETS = new ArrayList<>(List.of(
            new Vec3i(1, 0, 0),
            new Vec3i(-1, 0, 0),
            new Vec3i(0, 0, 1),
            new Vec3i(0, 0, -1)
    ));
    private static int STANDARD_OFFSET_IDX = 0;

    public static ArrayList<Vec3i> CHECKABLE_BELT_OFFSETS = new ArrayList<>(6);

    static {
        CHECKABLE_BELT_OFFSETS.add(STANDARD_OFFSET);
        CHECKABLE_BELT_OFFSETS.addAll(ADJACENT_OFFSETS);
    }

    public FlexibleBeltProcessingBehaviour(SmartBlockEntity be) {
        super(be);
    }

    public boolean canTargetBelt(Vec3i offsetFromBelt) {
        return offsetFromBelt.equals(STANDARD_OFFSET);
    }

    public static BeltProcessingBehaviour getBeltProcessingBehaviour(Level level, BlockPos pos) {
        for (int i = 0; i < CHECKABLE_BELT_OFFSETS.size(); i++) {
            Vec3i offset = CHECKABLE_BELT_OFFSETS.get(i);
            var behaviour = BlockEntityBehaviour.get(level, pos.offset(offset), TYPE);
            if (behaviour instanceof FlexibleBeltProcessingBehaviour flexibleBehaviour) {
                if (flexibleBehaviour.canTargetBelt(offset)) return behaviour;
            }
            else if (behaviour != null && i == STANDARD_OFFSET_IDX) return behaviour;
        }
        return null;
    }

}
