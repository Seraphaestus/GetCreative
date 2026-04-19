package amaryllis.get_creative.encapsulation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

public interface ISchematicTransformation {
    void getCreative$update(BlockPos anchor, Rotation rotation);
}
