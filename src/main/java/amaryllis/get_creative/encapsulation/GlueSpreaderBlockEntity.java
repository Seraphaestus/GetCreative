package amaryllis.get_creative.encapsulation;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.contraptions.glue.GlueEffectPacket;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.TRIGGERED;

public class GlueSpreaderBlockEntity extends SmartBlockEntity {

    public static Supplier<BlockEntityType<GlueSpreaderBlockEntity>> BLOCK_ENTITY;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "glue_spreader", () -> BlockEntityType.Builder.of(
                        GlueSpreaderBlockEntity::new, GlueSpreaderBlock.BLOCK.get()
                ).build(null));
    }

    public LerpedFloat piston = LerpedFloat.linear();
    protected boolean update = false;

    public GlueSpreaderBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);
    }

    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void initialize() {
        super.initialize();
        if (level.isClientSide) piston.startWithValue(getBlockState().getValue(TRIGGERED) ? 1 : 0);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (clientPacket) update = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide) return;

        piston.tickChaser();

        if (!update) return;
        update = false;
        int pistonTarget = getBlockState().getValue(TRIGGERED) ? 1 : 0;
        piston.chase(pistonTarget, .4f, LerpedFloat.Chaser.LINEAR);
        CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> VisualizationHelper.queueUpdate(this));
    }

    public void activate(Level level, BlockPos pos, Direction facing) {
        BlockPos target = pos.relative(facing);
        for (Direction.Axis axis: Direction.Axis.values()) {
            if (axis == facing.getAxis()) continue;
            tryGlue(level, target, Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE));
            tryGlue(level, target, Direction.fromAxisAndDirection(axis, AxisDirection.NEGATIVE));
        }
    }
    public boolean tryGlue(Level level, BlockPos from, Direction to) {
        if (!isConnectedToSpreader(to)) return false;
        if (SuperGlueEntity.isGlued(level, from, to, null)) return false;

        BlockPos toPos = from.relative(to);
        if (!SuperGlueEntity.isValidFace(level, from, to) ||
            !SuperGlueEntity.isValidFace(level, toPos, to.getOpposite())) return false;

        SuperGlueEntity entity = new SuperGlueEntity(level, SuperGlueEntity.span(from, toPos));

        if (!level.isClientSide) {
            level.addFreshEntity(entity);
            CatnipServices.NETWORK.sendToClientsTrackingEntity(entity, new GlueEffectPacket(from, to, true));
        }
        return true;
    }

    public boolean isConnectedToSpreader(Direction direction) {
        if (level == null) return false;
        BlockState adjState = level.getBlockState(getBlockPos().relative(direction));
        return adjState.is(GlueSpreaderBlock.BLOCK) && (adjState.getValue(FACING) == getBlockState().getValue(FACING));
    }
}
