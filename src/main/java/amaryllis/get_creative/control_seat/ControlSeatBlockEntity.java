package amaryllis.get_creative.control_seat;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.value_settings.BoundedScrollValueBehaviour;
import amaryllis.get_creative.value_settings.SwappableScrollValues;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class ControlSeatBlockEntity extends SmartBlockEntity {

    public static Supplier<BlockEntityType<ControlSeatBlockEntity>> BLOCK_ENTITY;

    private static final Direction[] SIDES = new Direction[] { Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH };

    protected int[] previousSignals = new int[4];
    protected boolean hasPassenger = false;

    protected SwappableScrollValues swappableScrollValues;
    public BoundedScrollValueBehaviour yawRange;
    public BoundedScrollValueBehaviour pitchRange;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "control_seat", () -> {
                    var blocks = ControlSeatBlock.BLOCKS.values().stream().map(DeferredHolder::get).toList().toArray(new Block[0]);
                    return BlockEntityType.Builder.of(ControlSeatBlockEntity::new, blocks).build(null);
                });
    }

    public ControlSeatBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        swappableScrollValues = new SwappableScrollValues(this, List.of(
            pitchRange = new BoundedScrollValueBehaviour("Pitch Range", 90, 90,
                    Component.translatable("get_creative.control_seat.pitch_range"), this, new ValuePanelSlot(true)),
            yawRange = new BoundedScrollValueBehaviour("Yaw Range", 180, 180,
                Component.translatable("get_creative.control_seat.yaw_range"), this, new ValuePanelSlot(false))
        ));
    }
    @Override
    public <T extends BlockEntityBehaviour> T getBehaviour(BehaviourType<T> type) {
        return swappableScrollValues.getBehaviour(type, () -> super.getBehaviour(type));
    }
    @Override
    public Collection<BlockEntityBehaviour> getAllBehaviours() {
        return swappableScrollValues.getAllBehaviours(super.getAllBehaviours());
    }

    @Override
    public void tick() {
        super.tick();

        // Update signal from passenger's facing
        ControlSeatBlock seatBlock = (ControlSeatBlock)getBlockState().getBlock();
        Entity passenger = seatBlock.getPassenger(level, getBlockPos());
        boolean hasPassenger = (passenger != null);
        if (this.hasPassenger != hasPassenger) {
            this.hasPassenger = hasPassenger;
            if (!hasPassenger) updateSignal();
        }
        if (hasPassenger) updateSignal();

        // Hotswap between value panels depending on the nearest player
        if (level.getGameTime() % 5 == 0) {
            Player nearestPlayer = level.getNearestPlayer(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), Player.DEFAULT_BLOCK_INTERACTION_RANGE, false);
            if (nearestPlayer != null) {
                int desiredValuePanelIndex = shouldShowPitchValuePanel(nearestPlayer) ? 0 : 1;
                swappableScrollValues.setIndex(desiredValuePanelIndex);
            }
        }
    }

    public void updateSignal() {
        BlockPos pos = getBlockPos();
        BlockState blockState = level.getBlockState(pos);
        if (!(blockState.getBlock() instanceof ControlSeatBlock seatBlock)) return;

        boolean anyChanged = false;
        for (int i = 0; i < 4; i++) {
            int newSignal = seatBlock.getSignal(blockState, level, pos, SIDES[i]);
            if (newSignal != previousSignals[i]) {
                previousSignals[i] = newSignal;
                anyChanged = true;
                level.neighborChanged(pos.relative(SIDES[i].getOpposite()), seatBlock, pos);
            }
        }
        if (anyChanged) {
            sendData();
        }
    }

    public boolean shouldShowPitchValuePanel(Player viewer) {
        Vec3 blockCenter = getBlockPos().getCenter();
        Vec3 viewPos = viewer.getEyePosition();
        Vec3 viewNormal = viewer.getViewVector(0);
        Vec3 toEast = blockCenter.add(0.5, 0, 0).subtract(viewPos);
        Vec3 toWest = blockCenter.add(-0.5, 0, 0).subtract(viewPos);
        Vec3 toSouth = blockCenter.add(0, 0, 0.5).subtract(viewPos);
        Vec3 toNorth = blockCenter.add(0, 0, -0.5).subtract(viewPos);
        Vec3 nearestXFace = ((toEast.lengthSqr() > toWest.lengthSqr()) ? toEast : toWest).normalize();
        Vec3 nearestZFace = ((toSouth.lengthSqr() > toNorth.lengthSqr()) ? toSouth : toNorth).normalize();
        boolean lookingAtXAxisFace = viewNormal.dot(nearestXFace) < viewNormal.dot(nearestZFace);

        Direction.Axis pitchAxis = getBlockState().getValue(ControlSeatBlock.FACING).getAxis();
        return (pitchAxis == Direction.Axis.X) == lookingAtXAxisFace;
    }

    @Override
    public void write(CompoundTag data, HolderLookup.Provider registries, boolean clientPacket) {
        data.putIntArray("Signals", previousSignals);
        data.putBoolean("HasPassenger", hasPassenger);
        swappableScrollValues.write(data);
        super.write(data, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag data, HolderLookup.Provider registries, boolean clientPacket) {
        if (data.contains("Signals")) previousSignals = data.getIntArray("Signals");
        if (data.contains("HasPassenger")) hasPassenger = data.getBoolean("HasPassenger");
        swappableScrollValues.read(data);
        super.read(data, registries, clientPacket);
    }

    protected static class ValuePanelSlot extends ValueBoxTransform.Sided {

        protected final boolean inFacingAxis;

        public ValuePanelSlot(boolean inFacingAxis) {
            this.inFacingAxis = inFacingAxis;
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 4, 15.5);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            if (direction.getAxis() == Direction.Axis.Y) return false;
            Direction.Axis facingAxis = state.getValue(ControlSeatBlock.FACING).getAxis();
            return (direction.getAxis() == facingAxis) == inFacingAxis;
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            if (!getSide().getAxis().isHorizontal()) {
                Direction facing = state.getValue(ControlSeatBlock.FACING);
                TransformStack.of(ms).rotateYDegrees(AngleHelper.horizontalAngle(facing) + 180);
            }
            super.rotate(level, pos, state, ms);
        }
    }
}
