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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
                int desiredValuePanelIndex = chooseValuePanel(nearestPlayer);
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

    // Returns the index of the swappable value panel that should be shown
    public int chooseValuePanel(Player viewer) {
        HitResult hitResult = viewer.pick(viewer.blockInteractionRange(), 0, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) return -1;
        BlockHitResult blockHitResult = (BlockHitResult)hitResult;
        if (!blockHitResult.getBlockPos().equals(getBlockPos())) return -1;

        Vec3 relativeHitPos = blockHitResult.getLocation().subtract(getBlockPos().getCenter());

        Direction.Axis selectedAxis = blockHitResult.getDirection().getAxis();
        if (selectedAxis == Direction.Axis.Y) return -1;

        Direction.Axis pitchAxis = getBlockState().getValue(ControlSeatBlock.FACING).getAxis();
        return (selectedAxis == pitchAxis) ? 0 : 1;
    }

    @Override
    public void write(CompoundTag data, HolderLookup.Provider registries, boolean clientPacket) {
        data.putIntArray("Signals", previousSignals);
        data.putBoolean("HasPassenger", hasPassenger);
        super.write(data, registries, clientPacket);
        swappableScrollValues.write(data, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag data, HolderLookup.Provider registries, boolean clientPacket) {
        if (data.contains("Signals")) previousSignals = data.getIntArray("Signals");
        if (data.contains("HasPassenger")) hasPassenger = data.getBoolean("HasPassenger");
        super.read(data, registries, clientPacket);
        swappableScrollValues.read(data, registries, clientPacket);
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
