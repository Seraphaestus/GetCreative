package amaryllis.get_creative.industrial_fan;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class IndustrialFanVisual extends KineticBlockEntityVisual<IndustrialFanBlockEntity>  {

    protected final RotatingInstance fan;

    public IndustrialFanVisual(VisualizationContext context, IndustrialFanBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        final Direction direction = blockState.getValue(FACING);
        final Direction opposite = direction.getOpposite();

        fan = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(IndustrialFanBlock.MODEL)).createInstance();

        fan.setup(blockEntity)
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, opposite)
                .setChanged();
    }

    @Override
    public void update(float pt) {
        fan.setup(blockEntity).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(fan);
    }

    @Override
    protected void _delete() {
        fan.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(fan);
    }

}
