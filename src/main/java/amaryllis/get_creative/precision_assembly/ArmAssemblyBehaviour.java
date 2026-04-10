package amaryllis.get_creative.precision_assembly;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class ArmAssemblyBehaviour extends FlexibleBeltProcessingBehaviour {

    public static final int CYCLE = 240;

    public ArmBlockEntity mechanicalArm;
    public int prevRunningTicks;
    public int runningTicks;
    public boolean running;
    public boolean finished;

    public <T extends ArmBlockEntity> ArmAssemblyBehaviour(T be) {
        super(be);
        mechanicalArm = be;
        whenItemEnters((s, i) -> BeltArmAssemblyCallbacks.onItemReceived(s, i, this));
        whileItemHeld((s, i) -> BeltArmAssemblyCallbacks.whenItemHeld(s, i, this));
    }

    @Override
    public boolean canTargetBelt(Vec3i offsetFromBelt) {
        if (!ADJACENT_OFFSETS.contains(offsetFromBelt)) return false;
        final BlockPos beltPos = mechanicalArm.getBlockPos().offset(Vec3i.ZERO.subtract(offsetFromBelt));
        return ((IArmAssembler)mechanicalArm).canProcessArmAssembly(beltPos);
    }

    @Override
    public void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        running = compound.getBoolean("Running");
        finished = compound.getBoolean("Finished");
        prevRunningTicks = runningTicks = compound.getInt("Ticks");
        super.read(compound, registries, clientPacket);
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putBoolean("Running", running);
        compound.putBoolean("Finished", finished);
        compound.putInt("Ticks", runningTicks);
        super.write(compound, registries, clientPacket);
    }

    public void start() {
        running = true;
        prevRunningTicks = 0;
        runningTicks = 0;
        blockEntity.sendData();
        ((IArmAssembler)mechanicalArm).startArmAssembly();
    }

    @Override
    public void tick() {
        super.tick();

        Level level = getWorld();
        BlockPos worldPosition = getPos();

        if (!running || level == null) return;

        if (level.isClientSide && runningTicks == -CYCLE / 2) {
            prevRunningTicks = CYCLE / 2;
            return;
        }

        if (runningTicks == CYCLE / 2 && mechanicalArm.getSpeed() != 0) {
            AllSoundEvents.CRAFTER_CLICK.playOnServer(level, worldPosition);
            if (!level.isClientSide) blockEntity.sendData();
        }

        if (!level.isClientSide && runningTicks > CYCLE) {
            finished = true;
            running = false;
            blockEntity.sendData();
            ((IArmAssembler)mechanicalArm).completeArmAssembly();
            return;
        }

        prevRunningTicks = runningTicks;
        runningTicks += getRunningTickSpeed();
        if (prevRunningTicks < CYCLE / 2 && runningTicks >= CYCLE / 2) {
            runningTicks = CYCLE / 2;
            // Pause the ticks until a packet is received
            if (level.isClientSide && !blockEntity.isVirtual())
                runningTicks = -(CYCLE / 2);
        }
    }

    public int getRunningTickSpeed() {
        float speed = mechanicalArm.getSpeed();
        if (speed == 0) return 0;
        return (int) Mth.lerp(Mth.clamp(Math.abs(speed) / 512f, 0, 1), 1, 60);
    }

    public interface IArmAssembler {
        boolean canProcessArmAssembly(BlockPos beltPos);
        void startArmAssembly();
        void completeArmAssembly();
    }

}
