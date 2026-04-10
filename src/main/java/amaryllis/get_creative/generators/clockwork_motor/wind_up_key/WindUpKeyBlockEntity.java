package amaryllis.get_creative.generators.clockwork_motor.wind_up_key;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.generators.clockwork_motor.ClockworkMotorBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

public class WindUpKeyBlockEntity extends HandCrankBlockEntity {

    public static Supplier<BlockEntityType<WindUpKeyBlockEntity>> BLOCK_ENTITY;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "wind_up_key", () -> BlockEntityType.Builder.of(
                        WindUpKeyBlockEntity::new, WindUpKeyBlock.BLOCK.get()
                ).build(null));
    }

    public static int TURN_ANGLE = 180;
    public static int COOLDOWN = 4;

    protected int startAngle;
    protected int targetAngle;
    protected int totalUseTicks;

    protected int cooldown;

    public WindUpKeyBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);
    }
    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("TotalUseTicks", totalUseTicks);
        compound.putInt("StartAngle", startAngle);
        compound.putInt("TargetAngle", targetAngle);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        totalUseTicks = compound.getInt("TotalUseTicks");
        startAngle = compound.getInt("StartAngle");
        targetAngle = compound.getInt("TargetAngle");
    }

    @Override
    public void tick() {
        super.tick();

        if (inUse == 0 && cooldown > 0) cooldown--;

        independentAngle = level.isClientSide() ? getIndependentAngle(0) : 0;
    }

    public boolean activate(boolean sneak) {
        if (getTheoreticalSpeed() != 0) return false;
        if (inUse > 0 || cooldown > 0) return false;
        if (level.isClientSide) return true;
        if (!(getBlockState().getBlock() instanceof WindUpKeyBlock windUpKeyBlock)) return false;

        // Always overshoot, target will stop early
        int rotationSpeed = windUpKeyBlock.getRotationSpeed();
        double degreesPerTick = KineticBlockEntity.convertToAngular(rotationSpeed);
        inUse = (int) Math.ceil(TURN_ANGLE / degreesPerTick) + 2;

        startAngle = (int) ((independentAngle) % 90 + 360) % 90;
        targetAngle = (int) AngleHelper.wrapAngle180(startAngle + TURN_ANGLE) + 360;
        //targetAngle = Math.round((startAngle + (TURN_ANGLE > 135 ? 180 : 90)) / 90f) * 90;
        totalUseTicks = inUse;
        backwards = sneak;

        sequenceContext = SequencedGearshiftBlockEntity.SequenceContext.fromGearshift(SequencerInstructions.TURN_ANGLE, rotationSpeed, TURN_ANGLE);
        updateGeneratedRotation();
        cooldown = COOLDOWN;

        return true;
    }

    @Override
    public float getIndependentAngle(float partialTicks) {
        if (inUse == 0 && source != null && getSpeed() != 0)
            return KineticBlockEntityRenderer.getAngleForBe(this, worldPosition,
                   KineticBlockEntityRenderer.getRotationAxisOf(this));

        int step = getBlockState().getOptionalValue(WindUpKeyBlock.FACING).orElse(Direction.SOUTH).getAxisDirection().getStep();

        return (inUse > 0 && totalUseTicks > 0
                ? Mth.lerp(Math.min(totalUseTicks, totalUseTicks - inUse + partialTicks) / (float) totalUseTicks,
                startAngle, targetAngle)
                : targetAngle) * Mth.DEG_TO_RAD * (backwards ? -1 : 1) * step;
    }

    // Prevent interaction with sequenced gearshifts
    @Override protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {}

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (level == null) return added;

        final Direction targetDir = getBlockState().getValue(WindUpKeyBlock.FACING).getOpposite();
        final BlockEntity target = level.getBlockEntity(getBlockPos().offset(targetDir.getNormal()));
        if (!(target instanceof ClockworkMotorBlockEntity clockworkMotor)) return added;
        if (!clockworkMotor.showGoggleTooltip()) return added;

        CreateLang.builder()
                .add(Component.translatable("tooltip.get_creative.adjacent",
                     Component.translatable("block.get_creative.clockwork_motor")))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 0);

        clockworkMotor.addGoggleTooltipBody(tooltip, isPlayerSneaking);
        return true;
    }
}
