package amaryllis.get_creative.generators.clockwork_motor;

import amaryllis.get_creative.Config;
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
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

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
    protected static final int MAX_SPEED = 256;
    protected static final int DEFAULT_SPEED = 16;

    public ClockworkMotorBlockEntity(BlockPos pos, BlockState blockState) {
        super(BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        var horizontalSlot = new CenteredSideValueBoxTransform((state, direction) ->
                state.getValue(BearingBlock.FACING).getAxis() != direction.getAxis());
        configuredSpeed = new BoundedScrollValueBehaviour("Speed", DEFAULT_SPEED, MAX_SPEED,
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
            int maxCharge = (int)(MAX_CHARGE * Config.CLOCKWORK_MOTOR_CHARGE_CAPACITY.getAsDouble());
            if (Math.abs(charge) < maxCharge) charge += Mth.sign(speed) * CHARGE_GAIN;

            if (!level.isClientSide) RotationPropagator.handleRemoved(level, getBlockPos(), this);
            tryingToRelease = true;
            cooldown = COOLDOWN;

        } else if (charge != 0 && !overStressed) {
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
        Direction inputDirection = ClockworkMotorBlock.getInputDirection(getBlockState());
        return source != null && source.equals(getBlockPos().relative(inputDirection));
    }

    public boolean isReleasing() {
        return !tryingToRelease && charge != 0 && !isWindingUp();
    }

    public int getChargeDrain() {
        final float speed = configuredSpeed.get();
        final int evenPoint = Config.CLOCKWORK_MOTOR_EVEN_POINT.getAsInt();

        // A ratio of the wind-down duration to the wind-up time
        final double speedFactor = (speed >= (1 << (evenPoint - 1)))
                ? speed / (double)(1 << evenPoint)
                : 1 / (evenPoint + 1 - Math.log(speed) / LOG_2);

        double drain = Math.round(CHARGE_GAIN * speedFactor) / Config.CLOCKWORK_MOTOR_EFFICIENCY.getAsDouble();
        return Math.max(1, (int)drain);
    }

    @Override
    public float getGeneratedSpeed() {
        if (!getBlockState().is(ClockworkMotorBlock.BLOCK) || !isReleasing()) return 0;
        return -Mth.sign(charge) * configuredSpeed.get();
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
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip, 0);

        addGoggleTooltipBody(tooltip, isPlayerSneaking);

        return true;
    }
    public void addGoggleTooltipBody(List<Component> tooltip, boolean isPlayerSneaking) {
        final float duration = Math.abs(charge) / (getChargeDrain() * 20f); // in seconds
        final String baseLangKey = isReleasing() ? "tooltip.get_creative.clockwork_charge.releasing"
                                                 : "tooltip.get_creative.clockwork_charge.winding";
        CreateLang.builder()
            .add(Component.translatable(baseLangKey, Component.translatable("tooltip.get_creative.clockwork_charge.info",
                    Component.translatable("tooltip.get_creative.time_in_seconds", String.format("%.1f", duration))
                        .withStyle(ChatFormatting.AQUA),
                    Component.translatable("tooltip.get_creative.speed_in_rpm", LangNumberFormat.format(configuredSpeed.get()))
                        .withStyle(SpeedLevel.of(configuredSpeed.get()).getTextColor())
                ).withStyle(ChatFormatting.GRAY)))
            .style(ChatFormatting.DARK_GRAY)
            .forGoggles(tooltip, 1);
    }

    // Hide overstressed warning if winding up
    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        final boolean isOverstressed = overStressed && AllConfigs.client().enableOverstressedTooltip.get();
        if (isOverstressed && isWindingUp()) return false;

        return super.addToTooltip(tooltip, isPlayerSneaking);
    }
}