package amaryllis.get_creative.generators.haunted_cogwheel;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.GetCreative;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class HauntedCogwheelBlockEntity extends GeneratingKineticBlockEntity {

    public static Supplier<BlockEntityType<HauntedCogwheelBlockEntity>> BLOCK_ENTITY;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "haunted_cogwheel", () -> BlockEntityType.Builder.of(
                        HauntedCogwheelBlockEntity::new, HauntedCogwheelBlock.BLOCK.get()
                ).build(null));
    }

    protected int hauntedSpeed = 0;

    public HauntedCogwheelBlockEntity(BlockPos pos, BlockState blockState) {
        super(BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("HauntedSpeed", hauntedSpeed);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        hauntedSpeed = compound.getInt("HauntedSpeed");
    }

    public void randomizeSpeed(RandomSource random) {
        final double minRotation = Config.HAUNTED_COGWHEEL_MIN_ROTATION.getAsDouble();
        final double maxRotation = Config.HAUNTED_COGWHEEL_MAX_ROTATION.getAsDouble();
        final double t = Math.pow(random.nextDouble(), Config.HAUNTED_COGWHEEL_DISTRIBUTION_FACTOR.getAsDouble());
        final int newSpeed = (int)(minRotation + (maxRotation - minRotation) * t);
        if (hauntedSpeed == newSpeed) return;

        hauntedSpeed = newSpeed;
        updateGeneratedRotation();
        playConvertSound(random);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null) randomizeSpeed(level.random);
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (!getBlockState().is(HauntedCogwheelBlock.BLOCK)) return 0;
        return convertToDirection(hauntedSpeed, getBlockState().getValue(HauntedCogwheelBlock.FACING).getOpposite());
    }

    @Override
    public void tick() {
        super.tick();

        final RandomSource random = level.getRandom();

        if (level.isClientSide) {
            if (random.nextFloat() <= 0.1f) spawnParticle(random);
            return;
        }

        final boolean speedIsBeingOverridden = Math.abs(hauntedSpeed) != Math.abs(getSpeed());
        if (speedIsBeingOverridden && random.nextFloat() < 0.02f) {
            level.destroyBlock(getBlockPos(), true);
            return;
        }

        if (random.nextDouble() < Config.HAUNTED_COGWHEEL_VOLATILITY.getAsDouble()) randomizeSpeed(random);
    }

    protected void spawnParticle(RandomSource random) {
        level.addParticle(
                ParticleTypes.SOUL,
                worldPosition.getX() + random.nextDouble(),
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + random.nextDouble(),
                random.nextDouble() * 0.04 - 0.02,
                0.05,
                random.nextDouble() * 0.04 - 0.02
        );
        playParticleSound(random);
    }

    protected void playParticleSound(RandomSource random) {
        float vol = random.nextFloat() * 0.4f + random.nextFloat() > 0.9f ? 0.6f : 0.0f;
        float pitch = 0.6f + random.nextFloat() * 0.4f;
        level.playSound(null, worldPosition, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, vol, pitch);
    }

    protected void playConvertSound(RandomSource random) {
        float pitch = 0.75f + random.nextFloat() * 0.5f;
        level.playSound(null, worldPosition, AllSoundEvents.HAUNTED_BELL_CONVERT.getMainEvent(), SoundSource.BLOCKS, 0.1f, pitch);
    }

}
