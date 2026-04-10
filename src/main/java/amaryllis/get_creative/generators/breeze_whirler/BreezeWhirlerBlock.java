package amaryllis.get_creative.generators.breeze_whirler;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.generators.EntityCaptureItem;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VoxelShaper;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class BreezeWhirlerBlock extends RotatedPillarKineticBlock implements IBE<BreezeWhirlerBlockEntity>, ICogWheel {

    public static final PartialModel GEAR_MODEL = PartialModel.of(GetCreative.ID("block/breeze_whirler/gear"));
    public static final PartialModel BREEZE_MODEL = PartialModel.of(GetCreative.ID("block/breeze_whirler/breeze_head"));
    public static final PartialModel BREEZE_WIND = PartialModel.of(GetCreative.ID("block/breeze_whirler/breeze_wind"));
    public static final SpriteShiftEntry WIND_SPRITE_SHIFT = SpriteShifter.get(
            GetCreative.ID("block/breeze_wind"), GetCreative.ID("block/breeze_wind_scroll"));

    public static final VoxelShaper SHAPE = new AllShapes.Builder(Block.box(0, 2, 0, 16, 14, 16)).forAxis();

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;

    public static DeferredBlock<Block> EMPTY_BLOCK;
    public static DeferredItem<BlockItem> EMPTY_ITEM;

    public static void register() {
        EMPTY_BLOCK = GetCreative.BLOCKS.registerBlock(
                "empty_breeze_whirler", RotatedPillarBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
        EMPTY_ITEM = GetCreative.ITEMS.registerItem("empty_breeze_whirler",
                properties -> new EntityCaptureItem(EMPTY_BLOCK.get(), properties, ITEM, "breeze_whirler")
                        .setParticles(ParticleTypes.CLOUD, ParticleTypes.WHITE_SMOKE)
                        .setSounds(SoundSource.HOSTILE, SoundEvents.BREEZE_HURT, SoundEvents.BREEZE_CHARGE),
                new Item.Properties());

        BLOCK = GetCreative.BLOCKS.registerBlock(
                "breeze_whirler", BreezeWhirlerBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
        ITEM = GetCreative.ITEMS.registerSimpleBlockItem(BLOCK);
        BreezeWhirlerBlockEntity.register();
    }

    public BreezeWhirlerBlock(Properties properties) {
        super(properties);
    }

    public boolean isLargeCog() {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(AXIS));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!CogWheelBlock.isValidCogwheelPosition(true, level, pos, getRotationAxis(state))) return false;

        for (Direction direction: Iterate.directions) {
            BlockPos neighbourPos = pos.relative(direction);
            BlockState neighbourState = level.getBlockState(neighbourPos);
            if (!neighbourState.is(BLOCK)) continue;

            Direction.Axis axis = state.getValue(AXIS);
            if (neighbourState.getValue(AXIS) != axis || axis != direction.getAxis()) return false;
        }
        return true;
    }

    protected Direction.Axis getAxisForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            return context.getClickedFace().getAxis();

        Level world = context.getLevel();

        BlockPos placedOnPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState placedAgainst = world.getBlockState(placedOnPos);

        Block block = placedAgainst.getBlock();
        if (ICogWheel.isSmallCog(placedAgainst)) return ((IRotate) block).getRotationAxis(placedAgainst);

        Direction.Axis preferredAxis = getPreferredAxis(context);
        return preferredAxis != null ? preferredAxis : context.getClickedFace().getAxis();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(AXIS, getAxisForPlacement(context));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override public float getParticleTargetRadius() { return 1.125f; }
    @Override public float getParticleInitialRadius() { return 1f; }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    public Class<BreezeWhirlerBlockEntity> getBlockEntityClass() {
        return BreezeWhirlerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BreezeWhirlerBlockEntity> getBlockEntityType() {
        return BreezeWhirlerBlockEntity.BLOCK_ENTITY.get();
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return false;
    }

}
