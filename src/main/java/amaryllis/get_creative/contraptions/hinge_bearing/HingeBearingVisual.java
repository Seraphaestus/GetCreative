package amaryllis.get_creative.contraptions.hinge_bearing;

import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

import java.util.function.Consumer;

public class HingeBearingVisual extends KineticBlockEntityVisual<HingeBearingBlockEntity> implements SimpleDynamicVisual {

    // Can't use BearingVisual because the Hinge Bearing doesn't have an input shaft on the bottom

    final OrientedInstance topInstance;

    final Axis rotationAxis;
    final Quaternionf blockOrientation;

    public HingeBearingVisual(VisualizationContext context, HingeBearingBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        Direction facing = blockState.getValue(BlockStateProperties.FACING);
        rotationAxis = Axis.of(Direction.get(Direction.AxisDirection.POSITIVE, rotationAxis()).step());

        blockOrientation = getBlockStateOrientation(facing);

        PartialModel top = blockEntity.isWoodenTop() ? AllPartialModels.BEARING_TOP_WOODEN : AllPartialModels.BEARING_TOP;

        topInstance = instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(top)).createInstance();
        topInstance.position(getVisualPosition())
                .rotation(blockOrientation)
                .setChanged();
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        float interpolatedAngle = blockEntity.getInterpolatedAngle(ctx.partialTick() - 1);
        Quaternionf rot = rotationAxis.rotationDegrees(interpolatedAngle);

        rot.mul(blockOrientation);

        topInstance.rotation(rot)
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(topInstance);
    }

    @Override
    protected void _delete() {
        topInstance.delete();
    }

    protected static Quaternionf getBlockStateOrientation(Direction facing) {
        Quaternionf orientation = (facing.getAxis().isHorizontal())
            ? Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite()))
            : new Quaternionf();
        orientation.mul(Axis.XP.rotationDegrees(-90 - AngleHelper.verticalAngle(facing)));
        return orientation;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(topInstance);
    }
}
