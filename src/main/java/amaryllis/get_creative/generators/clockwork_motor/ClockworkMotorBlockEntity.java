package amaryllis.get_creative.generators.clockwork_motor;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.value_settings.BoundedScrollValueBehaviour;
import amaryllis.get_creative.generators.clockwork_motor.wind_up_key.WindUpKeyBlockEntity;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

public class ClockworkMotorBlockEntity extends GeneratingKineticBlockEntity {

    public static Supplier<BlockEntityType<ClockworkMotorBlockEntity>> BLOCK_ENTITY;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "clockwork_motor", () -> BlockEntityType.Builder.of(
                        ClockworkMotorBlockEntity::new, ClockworkMotorBlock.BLOCK.get()
                ).build(null));
    }

    private static final double LOG_2 = Math.log(2);

    protected static int CHARGE_GAIN = 256;
    protected static int MAX_CHARGE = 30 * 20 * CHARGE_GAIN; // 30s to fully wind
    protected static int COOLDOWN = WindUpKeyBlockEntity.COOLDOWN + 2;
    protected int charge = 0;
    protected boolean tryingToRelease = true;
    protected int cooldown = COOLDOWN;

    protected BoundedScrollValueBehaviour configuredSpeed;
    protected static final int MIN_SPEED = 1;
    protected static final int MAX_SPEED = 256;
    protected static final int DEFAULT_SPEED = 8;

    public ClockworkMotorBlockEntity(BlockPos pos, BlockState blockState) {
        super(BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        var horizontalSlot = new CenteredSideValueBoxTransform((state, direction) ->
                state.getValue(BearingBlock.FACING).getAxis() != direction.getAxis());
        configuredSpeed = new BoundedScrollValueBehaviour("Speed", MIN_SPEED, DEFAULT_SPEED, MAX_SPEED,
                Component.translatable("get_creative.clockwork_motor.configured_speed"),
                this, horizontalSlot);
        behaviours.add(configuredSpeed);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("ClockworkCharge", charge);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        charge = compound.getInt("ClockworkCharge");
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) updateGeneratedRotation();
    }

    @Override
    public void tick() {
        super.tick();

        if (isWindingUp()) {
            // Increment charge in direction of rotation
            if (charge > -MAX_CHARGE && charge < MAX_CHARGE) charge += Mth.sign(speed) * CHARGE_GAIN;
            if (!level.isClientSide) RotationPropagator.handleRemoved(level, getBlockPos(), this);
            tryingToRelease = true;
            cooldown = COOLDOWN;

        } else if (charge != 0) {
            if (tryingToRelease) {
                if (speed != 0) return;
                if (cooldown > 0) {
                    cooldown--;
                    return;
                }
                tryingToRelease = false;
            }
            charge = (charge > 0)
                    ? Math.max(0, charge - getChargeDrain())
                    : Math.min(0, charge + getChargeDrain());
            if (!level.isClientSide) {
                RotationPropagator.handleAdded(level, getBlockPos(), this);
                updateGeneratedRotation();
            }
        }
    }

    public boolean isWindingUp() {
        return source != null && source.equals(getBlockPos().offset(getBlockState().getValue(FACING).getNormal()));
    }

    public boolean isReleasing() {
        return !tryingToRelease && charge != 0 && !isWindingUp();
    }

    public int getChargeDrain() {
        final float speed = configuredSpeed.get();
        // A ratio of the wind-down duration to the wind-up time
        // e.g. 8 RPM -> 1/3 -> 3x what you put in
        // From 1 RPM to 32 RPM, each power of 2 reduces the time by -1x, from 6x to 1x
        // From 32 RPM to 256 RPM, each power of 2 reduces the time by *0.5x, from 1x to 1/8x
        final double speedFactor = (speed >= 16) ? speed / 32d : 1 / (6 - Math.log(speed) / LOG_2);
        return Math.max(1, (int)Math.round(CHARGE_GAIN * speedFactor));
    }

    @Override
    public float getGeneratedSpeed() {
        if (!getBlockState().is(ClockworkMotorBlock.BLOCK) || !isReleasing()) return 0;
        return -Mth.sign(charge) * configuredSpeed.get();
    }

    protected float easeOutIn(float t) {
        return (float)Math.pow(Math.max(0, 1.25 * t - 0.3), 1.8) + 0.1f;
    }

    public boolean showGoggleTooltip() {
        return charge != 0;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!super.addToGoggleTooltip(tooltip, isPlayerSneaking)) return false;
        if (!showGoggleTooltip()) return false;

        CreateLang.builder()
                .add(Component.translatable("tooltip.get_creative.clockwork_motor"))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 0);

        addGoggleTooltipBody(tooltip, isPlayerSneaking);

        return true;
    }
    public void addGoggleTooltipBody(List<Component> tooltip, boolean isPlayerSneaking) {
        final float duration = Math.abs(charge) / (getChargeDrain() * 20f); // in seconds

        CreateLang.builder()
            .add(Component.translatable("tooltip.get_creative.clockwork_charge",
                Component.translatable("tooltip.get_creative.clockwork_charge.info",
                    Component.translatable("tooltip.get_creative.time_in_seconds", String.format("%.1f", duration))
                        .withStyle(ChatFormatting.AQUA),
                    Component.translatable("tooltip.get_creative.speed_in_rpm", LangNumberFormat.format(configuredSpeed.get()))
                        .withStyle(SpeedLevel.of(configuredSpeed.get()).getTextColor())
                ).withStyle(ChatFormatting.GRAY)))
            .style(ChatFormatting.DARK_GRAY)
            .forGoggles(tooltip, 1);
    }
}