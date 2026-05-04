package amaryllis.get_creative.appliances.encapsulation;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Supplier;

public class GlueCleanerBlockEntity extends SmartBlockEntity implements GlueHighlighter {

    public static Supplier<BlockEntityType<GlueCleanerBlockEntity>> BLOCK_ENTITY;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "glue_cleaner", () -> BlockEntityType.Builder.of(
                        GlueCleanerBlockEntity::new, GlueCleanerBlock.BLOCK.get()
                ).build(null));
    }

    protected static int PARTICLE_DURATION = 15;
    protected int emittingParticles = 0;

    public GlueCleanerBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        Direction facing = getBlockState().getValue(GlueCleanerBlock.FACING);
        if (level.isClientSide) {
            outlineTargetedGlueEntities(level, getBlockPos().relative(facing), true);
        }
        else if (emittingParticles > 0 && level instanceof ServerLevel serverLevel) {
            emittingParticles -= 1;
            spawnParticles(serverLevel, getBlockPos(), facing);
        }
    }

    public void activate(ServerLevel level, BlockPos pos, Direction facing) {
        BlockPos targetPos = pos.relative(facing);
        AABB directTarget = new AABB(targetPos);
        var targetedGlue = GlueHighlighter.getConnectedGlueEntities(level, targetPos);
        targetedGlue.stream()
                .filter(glue -> glue.getBoundingBox().intersects(directTarget))
                .forEach(superGlue -> {
                    GlueHighlighter.spawnParticlesForSuperGlue(superGlue, ParticleTypes.BUBBLE_POP, ParticleTypes.END_ROD);
                    superGlue.discard();
                });

        emittingParticles = PARTICLE_DURATION;
        playSound(level);
    }

    protected void spawnParticles(ServerLevel level, BlockPos blockPos, Direction facing) {
        Vec3 pos = blockPos.getCenter().relative(facing, 0.5);
        pos = VecHelper.offsetRandomly(pos, level.random, 0.375f);
        Vec3 facingNormal = Vec3.atLowerCornerOf(facing.getNormal());

        double t = Math.pow((PARTICLE_DURATION - emittingParticles) / (double)PARTICLE_DURATION, 1.5);
        pos = pos.add(facingNormal.multiply(t, t, t)); // Slide forward
        Vec3 motion = new Vec3(0, t, 0); // Rise up
        level.sendParticles(ParticleTypes.BUBBLE_POP, pos.x, pos.y, pos.z, 0, motion.x, motion.y, motion.z, 0.15);
    }
    protected void playSound(ServerLevel level) {
        level.playSound(null, getBlockPos(), GlueCleanerBlock.ACTIVATE_SOUND.get(), SoundSource.BLOCKS);
    }

    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

}
