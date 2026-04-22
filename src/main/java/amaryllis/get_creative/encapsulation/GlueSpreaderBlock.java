package amaryllis.get_creative.encapsulation;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.TRIGGERED;

public class GlueSpreaderBlock extends WrenchableDirectionalBlock implements IBE<GlueSpreaderBlockEntity> {

    public static final PartialModel SIDE_MODEL_OPEN = PartialModel.of(GetCreative.ID("block/glue_spreader/side_open"));
    public static final PartialModel SIDE_MODEL_SHUT = PartialModel.of(GetCreative.ID("block/glue_spreader/side_shut"));

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;

    public static void register() {
        BLOCK = GetCreative.BLOCKS.registerBlock(
                "glue_spreader", GlueSpreaderBlock::new,
                Properties.of().explosionResistance(6).destroyTime(1.5f).mapColor(MapColor.GOLD).noOcclusion());
        ITEM = GetCreative.ITEMS.registerSimpleBlockItem(BLOCK);
        GlueSpreaderBlockEntity.register();
    }

    public GlueSpreaderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TRIGGERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        boolean hasSignal = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        boolean isTriggered = state.getValue(TRIGGERED);
        if (hasSignal && !isTriggered) {
            if (!level.isClientSide) level.scheduleTick(pos, this, 4);
            level.setBlock(pos, state.setValue(TRIGGERED, true), 2);
        }
        else if (!hasSignal && isTriggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), 2);
        }
    }

    @Override
    protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (level.getBlockEntity(pos) instanceof GlueSpreaderBlockEntity glueSpreaderBE) {
            glueSpreaderBE.activate(level, pos, state.getValue(FACING));
        }
    }

    @Override
    public Class<GlueSpreaderBlockEntity> getBlockEntityClass() {
        return GlueSpreaderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GlueSpreaderBlockEntity> getBlockEntityType() {
        return GlueSpreaderBlockEntity.BLOCK_ENTITY.get();
    }

    //region Bounciness
    protected boolean isBouncy(BlockGetter level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        return blockState.is(BLOCK) && blockState.getValue(FACING) == Direction.UP;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!isBouncy(level, pos) || entity.isSuppressingBounce()) super.fallOn(level, state, pos, entity, fallDistance);
        entity.causeFallDamage(fallDistance, 1.0F, level.damageSources().fall());
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (!isBouncy(level, entity.blockPosition().below()) || entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
        } else {
            // Bounce up
            Vec3 deltaMovement = entity.getDeltaMovement();
            if (deltaMovement.y < 0) {
                double speed = (entity instanceof LivingEntity) ? 1 : 0.8;
                entity.setDeltaMovement(deltaMovement.x, -deltaMovement.y * speed, deltaMovement.z);
            }
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        double delta = Math.abs(entity.getDeltaMovement().y);
        if (delta < 0.1 && !entity.isSteppingCarefully() && isBouncy(level, pos)) {
            delta = delta * 0.2 + 0.4;
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(delta, 1.0D, delta));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
        if (!isBouncy(level, pos)) return super.addLandingEffects(state1, level, pos, state2, entity, numberOfParticles);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.defaultBlockState()),
                entity.getX(), entity.getY(), entity.getZ(), numberOfParticles, 0, 0, 0, 0.15);
        return true;
    }

    @Override
    public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(FACING) != Direction.UP) return super.addRunningEffects(state, level, pos, entity);

        Vec3 deltaMovement = entity.getDeltaMovement();
        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.defaultBlockState()).setPos(pos),
                entity.getX() + ((double)level.random.nextFloat() - 0.5) * (double)entity.getBbWidth(),
                entity.getY() + 0.1,
                entity.getZ() + ((double)level.random.nextFloat() - 0.5) * (double)entity.getBbWidth(),
                deltaMovement.x * -4, 1.5, deltaMovement.z * -4);
        return true;
    }
    //endregion
}