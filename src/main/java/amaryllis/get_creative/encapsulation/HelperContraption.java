package amaryllis.get_creative.encapsulation;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class HelperContraption extends Contraption {

    protected Direction facing;

    public HelperContraption(Direction facing) {
        this.facing = facing;
    }

    @Override
    public boolean assemble(Level level, BlockPos pos) throws AssemblyException {
        if (!searchMovedStructure(level, pos.relative(facing), null)) return false;
        return !blocks.isEmpty();
    }

    @Override
    protected boolean isAnchoringBlockAt(BlockPos pos) {
        return pos.equals(anchor.relative(facing.getOpposite()));
    }

    @Override
    public boolean canBeStabilized(Direction facing, BlockPos localPos) {
        return false;
    }

    @Override
    public ContraptionType getType() {
        return null;
    }
}
