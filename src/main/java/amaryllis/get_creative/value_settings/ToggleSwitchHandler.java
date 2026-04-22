package amaryllis.get_creative.value_settings;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class ToggleSwitchHandler {

    public static boolean isHovered(BlockEntity be, Vec3 hitPos, Direction hitDir, int states) {
        Direction facing = be.getBlockState().getValue(BlockStateProperties.FACING);
        if (hitDir.getAxis() == facing.getAxis()) return false;

        Vec3 localHitPos = hitPos.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));

        double height = 2 / 16d;
        double radius = (states + 1) / 16d;
        boolean inRadiusX = (hitDir.getAxis() == Direction.Axis.X) || Math.abs(localHitPos.x - 0.5) <= radius;
        boolean inRadiusY = (hitDir.getAxis() == Direction.Axis.Y) || Math.abs(localHitPos.y - 0.5) <= radius;
        boolean inRadiusZ = (hitDir.getAxis() == Direction.Axis.Z) || Math.abs(localHitPos.z - 0.5) <= radius;

        return switch (facing) {
            case Direction.UP    -> inRadiusX && inRadiusZ && localHitPos.y < height;
            case Direction.DOWN  -> inRadiusX && inRadiusZ && localHitPos.y > 1 - height;
            case Direction.EAST  -> inRadiusY && inRadiusZ && localHitPos.x < height;
            case Direction.WEST  -> inRadiusY && inRadiusZ && localHitPos.x > 1 - height;
            case Direction.SOUTH -> inRadiusX && inRadiusY && localHitPos.z < height;
            case Direction.NORTH -> inRadiusX && inRadiusY && localHitPos.z > 1 - height;
        };
    }

}
