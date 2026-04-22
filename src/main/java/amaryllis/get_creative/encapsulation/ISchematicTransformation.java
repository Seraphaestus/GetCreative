package amaryllis.get_creative.encapsulation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;

public interface ISchematicTransformation {
    void getCreative$update(BlockPos anchor, Vec3i anchorOffset, boolean positionHasChanged,
                            Rotation rotation, Rotation prevRotation);
}
