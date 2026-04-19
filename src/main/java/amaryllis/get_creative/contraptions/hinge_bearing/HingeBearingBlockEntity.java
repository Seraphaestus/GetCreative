package amaryllis.get_creative.contraptions.hinge_bearing;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.contraptions.hinge_bearing.HandleBlock.Material;
import amaryllis.get_creative.value_settings.BoundedScrollValueBehaviour;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Supplier;

public class HingeBearingBlockEntity extends KineticBlockEntity
        implements IBearingBlockEntity, IDisplayAssemblyExceptions {

    public static Supplier<BlockEntityType<HingeBearingBlockEntity>> BLOCK_ENTITY;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "hinge_bearing", () -> BlockEntityType.Builder.of(
                        HingeBearingBlockEntity::new, HingeBearingBlock.BLOCK.get()
                ).build(null));
    }

    protected ControlledContraptionEntity movedContraption;
    protected boolean running;
    protected boolean assembleNextTick;
    protected AssemblyException lastException;

    protected BoundedScrollValueBehaviour openSpeed;
    protected static final int MAX_SPEED = 32;
    protected static final int DEFAULT_SPEED = 16;

    public enum OpenState { NEUTRAL, OPEN_CW, OPEN_CCW }
    protected OpenState targetState = OpenState.NEUTRAL;
    protected double openAngle = 90;
    protected final static double epsilon = 1;

    protected float angle;
    protected float prevAngle;
    protected float clientAngleDiff;

    protected boolean isHingeTurning = false;


    public HingeBearingBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);
        setLazyTickRate(3);    }

    @Override public boolean isWoodenTop() {
        return false;
    }
    @Override public AssemblyException getLastAssemblyException() {
        return lastException;
    }
    @Override public BlockPos getBlockPosition() {
        return worldPosition;
    }
    @Override public boolean isValid() {
        return !isRemoved();
    }
    @Override public boolean isAttachedTo(AbstractContraptionEntity contraption) {
        return movedContraption == contraption;
    }
    public boolean isRunning() {
        return running;
    }
    public ControlledContraptionEntity getMovedContraption() {
        return movedContraption;
    }

    public boolean isHingeTurning() {
        return isHingeTurning;
    }
    protected void updateHingeTurning() {
        boolean isHingeTurning = running && Math.abs(getTargetAngle() - angle) >= epsilon;
        if (isHingeTurning != this.isHingeTurning && level != null) {
            this.isHingeTurning = isHingeTurning;
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        }
    }

    public void setAngle(float forcedAngle) { angle = forcedAngle; }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        openSpeed = new BoundedScrollValueBehaviour("Speed", DEFAULT_SPEED, MAX_SPEED,
                Component.translatable("get_creative.hinge_bearing.opening_speed"),
                this, getMovementModeSlot());
        behaviours.add(openSpeed);

        registerAwardables(behaviours, AllAdvancements.CONTRAPTION_ACTORS);
    }

    @Override
    public void remove() {
        if (!level.isClientSide) disassemble();
        super.remove();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putBoolean("Running", running);
        compound.putFloat("Angle", angle);
        if (targetState != OpenState.NEUTRAL) compound.putBoolean("TargetState", targetState == OpenState.OPEN_CW);
        AssemblyException.write(compound, registries, lastException);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        if (wasMoved) {
            super.read(compound, registries, clientPacket);
            return;
        }

        float angleBefore = angle;
        running = compound.getBoolean("Running");
        angle = compound.getFloat("Angle");
        targetState = !compound.contains("TargetState") ? OpenState.NEUTRAL
            : (compound.getBoolean("TargetState") ? OpenState.OPEN_CW : OpenState.OPEN_CCW);
        lastException = AssemblyException.read(compound, registries);
        super.read(compound, registries, clientPacket);

        if (!clientPacket) return;

        if (running) {
            if (movedContraption == null || !movedContraption.isStalled()) {
                clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore, angle);
                angle = angleBefore;
            }
        } else movedContraption = null;
    }

    @Override
    public float getInterpolatedAngle(float partialTicks) {
        if (isVirtual()) return Mth.lerp(partialTicks + .5f, prevAngle, angle);
        if (movedContraption == null || movedContraption.isStalled() || !running) partialTicks = 0;
        return Mth.lerp(partialTicks, angle, getNewAngle());
    }

    public double getTargetAngle() {
        return switch(targetState) {
            case OpenState.NEUTRAL -> 0;
            case OpenState.OPEN_CW -> openAngle;
            case OpenState.OPEN_CCW -> -openAngle;
        };
    }

    public float getAngularSpeed() {
        final double deltaAngle = getTargetAngle() - angle;
        if (Math.abs(deltaAngle) < epsilon) return 0;
        float speed = convertToAngular(Mth.sign(deltaAngle) * openSpeed.get());
        if (level.isClientSide) {
            speed *= ServerSpeedProvider.get();
            speed += clientAngleDiff / 3f;
        }
        return speed;
    }

    public float getNewAngle() {
        float newAngle = angle + getAngularSpeed();
        newAngle = (newAngle + 180) % 360 - 180;
        if (targetState == OpenState.NEUTRAL) {
            if (newAngle > angle && newAngle > 0 ||
                newAngle < angle && newAngle < 0) newAngle = 0;
        }
        newAngle = (float)Mth.clamp(newAngle, -openAngle, openAngle);
        if (Math.abs(getTargetAngle() - newAngle) < epsilon) newAngle = (float)getTargetAngle();
        return newAngle;
    }

    public void assemble() {
        if (!(level.getBlockState(worldPosition).getBlock() instanceof BearingBlock)) return;

        Direction direction = getBlockState().getValue(BearingBlock.FACING);
        BearingContraption contraption = new BearingContraption(false, direction);
        try {
            if (!contraption.assemble(level, worldPosition)) return;
            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return;
        }

        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
        movedContraption = ControlledContraptionEntity.create(level, this, contraption);
        BlockPos anchor = worldPosition.relative(direction);
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        movedContraption.setRotationAxis(direction.getAxis());
        level.addFreshEntity(movedContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

        if (contraption.containsBlockBreakers()) award(AllAdvancements.CONTRAPTION_ACTORS);

        running = true;
        angle = 0;
        sendData();
    }

    public void disassemble() {
        if (!running && movedContraption == null) return;
        angle = 0;
        targetState = OpenState.NEUTRAL;
        if (movedContraption != null) {
            movedContraption.disassemble();
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition);
        }

        movedContraption = null;
        running = false;
        assembleNextTick = false;
        sendData();
    }

    @Override
    public void tick() {
        super.tick();

        prevAngle = angle;
        if (level.isClientSide) clientAngleDiff /= 2;

        if (!level.isClientSide && assembleNextTick) {
            assembleNextTick = false;
            if (running) {
                if (speed == 0 && (movedContraption == null || movedContraption.getContraption().getBlocks().isEmpty())) {
                    if (movedContraption != null) movedContraption.getContraption().stop(level);
                    disassemble();
                    return;
                }
            } else assemble();
        }

        if (running) {
            boolean isStalled = movedContraption != null && movedContraption.isStalled();
            if (!isStalled) angle = getNewAngle();

            applyRotation();
        }

        updateHingeTurning();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (movedContraption != null && !level.isClientSide) sendData();
    }

    protected void applyRotation() {
        if (movedContraption == null) return;
        movedContraption.setAngle(angle);
        BlockState blockState = getBlockState();
        if (blockState.hasProperty(BlockStateProperties.FACING))
            movedContraption.setRotationAxis(blockState.getValue(BlockStateProperties.FACING).getAxis());
    }

    @Override
    public void attach(ControlledContraptionEntity contraption) {
        BlockState blockState = getBlockState();
        if (!(contraption.getContraption() instanceof BearingContraption)) return;
        if (!blockState.hasProperty(BearingBlock.FACING)) return;

        this.movedContraption = contraption;
        setChanged();
        BlockPos anchor = worldPosition.relative(blockState.getValue(BearingBlock.FACING));
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        if (!level.isClientSide) {
            this.running = true;
            sendData();
        }
    }

    @Override
    public void onStall() {
        if (!level.isClientSide) sendData();
    }

    public void openDoor(Player player, AbstractContraptionEntity contraption, BlockPos localInteractPos) {
        openDoor(player, contraption, localInteractPos, player.isShiftKeyDown());
    }
    public void openDoor(Player player, AbstractContraptionEntity contraption, BlockPos localInteractPos, boolean openTowardsPlayer) {
        if (targetState != OpenState.NEUTRAL) {
            openDoor(targetState != OpenState.OPEN_CW);
            return;
        }

        Vector3f pivot = getBlockPosition().getCenter().toVector3f();
        Vector3f playerPos = player.position().toVector3f();
        Vector3f interactPos = contraption.toGlobalVector(localInteractPos.getCenter(), 1).toVector3f();

        boolean turnClockwise = shouldOpenClockwise(playerPos, interactPos, pivot);
        if (openTowardsPlayer) turnClockwise = !turnClockwise;
        openDoor(turnClockwise);
    }
    public void openDoor(boolean clockwise) {
        targetState = (targetState != OpenState.NEUTRAL) ? OpenState.NEUTRAL
                    : (clockwise ? OpenState.OPEN_CW : OpenState.OPEN_CCW);
    }
    public boolean shouldOpenClockwise(Vector3f playerPos, Vector3f handlePos, Vector3f pivot) {
        // Simulate opening the door in each direction, check which puts the handle closer to the player
        final float epsilon = 0.01f; // ~0.5 degrees
        Vec3i axis = getBlockState().getValue(HingeBearingBlock.FACING).getNormal();
        axis = new Vec3i(Math.abs(axis.getX()), Math.abs(axis.getY()), Math.abs(axis.getZ()));
        final var handlePos_CW = new Vector3f(handlePos).sub(pivot).rotateAxis(epsilon, axis.getX(), axis.getY(), axis.getZ()).add(pivot);
        final var handlePos_CCW = new Vector3f(handlePos).sub(pivot).rotateAxis(-epsilon, axis.getX(), axis.getY(), axis.getZ()).add(pivot);

        return handlePos_CW.distanceSquared(playerPos) > handlePos_CCW.distanceSquared(playerPos);
    }

    public void playSound(Material isWooden) {
        if (level == null) return;
        final boolean isOpening = (targetState != OpenState.NEUTRAL);
        SoundEvent sound = switch (isWooden) {
            case Material.WOODEN -> isOpening ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE;
            case Material.COPPER -> isOpening ? SoundEvents.COPPER_DOOR_OPEN : SoundEvents.COPPER_DOOR_CLOSE;
            case Material.IRON ->   isOpening ? SoundEvents.IRON_DOOR_OPEN   : SoundEvents.IRON_DOOR_CLOSE;
        };
        level.playSound(null, getBlockPos(), sound, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (super.addToTooltip(tooltip, isPlayerSneaking)) return true;
        if (isPlayerSneaking || running) return false;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BearingBlock)) return false;

        BlockState attachedState = level.getBlockState(worldPosition.relative(state.getValue(BearingBlock.FACING)));
        if (attachedState.canBeReplaced()) return false;

        TooltipHelper.addHint(tooltip, "hint.empty_bearing");
        return true;
    }
}
