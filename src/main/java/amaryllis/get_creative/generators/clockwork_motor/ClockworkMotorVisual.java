package amaryllis.get_creative.generators.clockwork_motor;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

import java.util.function.Consumer;

import static com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer.getAngleForBe;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class ClockworkMotorVisual extends KineticBlockEntityVisual<ClockworkMotorBlockEntity> implements SimpleDynamicVisual {

    protected final RotatingInstance halfShaft;
    protected final TransformedInstance mechanism;

    public ClockworkMotorVisual(VisualizationContext context, ClockworkMotorBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        final Direction direction = ClockworkMotorBlock.getInputDirection(blockState);
        final Direction.Axis axis = direction.getAxis();

        mechanism = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ClockworkMotorBlock.MODEL)).createInstance();
        rotateMechanism(partialTick);

        halfShaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF)).createInstance();
        halfShaft.setup(blockEntity)
                .setPosition(getVisualPosition())
                .setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, pos))
                .rotateToFace(Direction.SOUTH, direction.getOpposite())
                .setChanged();
    }

    @Override
    public void update(float pt) {
        halfShaft.setup(blockEntity).setChanged();
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        rotateMechanism(ctx.partialTick());
    }

    private void rotateMechanism(float pt) {
        final var axis = ((IRotate) blockState.getBlock()).getRotationAxis(blockState);
        var facing = blockState.getValue(BlockStateProperties.FACING);
        BlockEntity source = level.getBlockEntity(pos.relative(facing));

        float angle = (source instanceof HandCrankBlockEntity crank)
                ? crank.getIndependentAngle(pt)
                : getAngleForBe(blockEntity, pos, axis);

        mechanism.setIdentityTransform()
                .translate(getVisualPosition())
                .center()
                .rotate(angle, Direction.get(Direction.AxisDirection.POSITIVE, facing.getAxis()))
                .rotate(new Quaternionf().rotateTo(0, 1, 0, facing.getStepX(), facing.getStepY(), facing.getStepZ()))
                .uncenter()
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(halfShaft);
        relight(mechanism);
    }

    @Override
    protected void _delete() {
        halfShaft.delete();
        mechanism.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(halfShaft);
        consumer.accept(mechanism);
    }
}