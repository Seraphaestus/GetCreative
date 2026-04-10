package amaryllis.get_creative.generators.breeze_whirler;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Supplier;

public class BreezeWhirlerBlockEntity extends GeneratingKineticBlockEntity {

    public static Supplier<BlockEntityType<BreezeWhirlerBlockEntity>> BLOCK_ENTITY;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "breeze_whirler", () -> BlockEntityType.Builder.of(
                        BreezeWhirlerBlockEntity::new, BreezeWhirlerBlock.BLOCK.get()
                ).build(null));
    }

    public LerpedFloat headAngle;
    public boolean warpHeadAngle;
    public boolean isPlayerAbove;

    public BreezeWhirlerBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);
        headAngle = LerpedFloat.angular();
        headAngle.startWithValue((getDefaultHeadAngle(state) + 180) % 360);
        warpHeadAngle = true;
    }

    public static boolean supportsVisualization() {
        return false;
    }

    @Override
    public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1)).forEach(offset -> {
            if (offset.distSqr(BlockPos.ZERO) == 2) neighbours.add(worldPosition.offset(offset));
        });
        return neighbours;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (!getBlockState().is(BreezeWhirlerBlock.BLOCK)) return 0;
        final var axis = Direction.get(Direction.AxisDirection.POSITIVE, getBlockState().getValue(BreezeWhirlerBlock.AXIS));
        return convertToDirection((float)Config.BREEZE_WHIRLER_ROTATION_SPEED.getAsDouble(), axis);
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide && shouldTickAnimation()) tickAnimation();
    }

    @OnlyIn(Dist.CLIENT)
    private boolean shouldTickAnimation() {
        return !supportsVisualization() || !VisualizationManager.supportsVisualization(level);
    }

    @OnlyIn(Dist.CLIENT)
    void tickAnimation() {
        if (warpHeadAngle) {
            headAngle.startWithValue(getHeadAngle());
            warpHeadAngle = false;
        } else {
            headAngle.chase(getHeadAngle(), .25f, LerpedFloat.Chaser.exp(5));
            headAngle.tickChaser();
        }
        isPlayerAbove = (Minecraft.getInstance().player.getEyeY() > getBlockPos().getCenter().y());
    }

    protected float getDefaultHeadAngle(BlockState state) {
        var axis = state.getOptionalValue(BreezeWhirlerBlock.AXIS).orElse(Direction.Axis.X);
        if (axis == Direction.Axis.Y) axis = Direction.Axis.X;
        return AngleHelper.horizontalAngle(Direction.get(Direction.AxisDirection.POSITIVE, axis));
    }

    @OnlyIn(Dist.CLIENT)
    protected float getHeadAngle() {
        float target = 0;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !player.isInvisible()) {
            double playerX = isVirtual() ? -4 : player.getX();
            double playerZ = isVirtual() ? -10 : player.getZ();
            double dx = playerX - (getBlockPos().getX() + 0.5);
            double dz = playerZ - (getBlockPos().getZ() + 0.5);
            target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
        }
        return headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1);
    }
}
