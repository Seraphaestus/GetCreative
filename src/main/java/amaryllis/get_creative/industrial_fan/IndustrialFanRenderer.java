package amaryllis.get_creative.industrial_fan;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class IndustrialFanRenderer extends KineticBlockEntityRenderer<IndustrialFanBlockEntity>  {

    public IndustrialFanRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(IndustrialFanBlockEntity be, BlockState state) {
        final Direction direction = state.getValue(FACING);
        return CachedBuffers.partialFacing(IndustrialFanBlock.MODEL, be.getBlockState(), direction.getOpposite());
    }

}
