package amaryllis.get_creative.generators.haunted_cogwheel;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public class HauntedCogwheelBlock extends DirectionalKineticBlock implements IBE<HauntedCogwheelBlockEntity>, ICogWheel, ProperWaterloggedBlock {

    public static final PartialModel MODEL = PartialModel.of(GetCreative.ID("item/haunted_cogwheel"));

    protected static final VoxelShape GEAR_SHAPE = Block.box(2, 6, 2, 14, 10, 14);
    protected static final VoxelShaper HALF_SHAFT_SHAPE = new AllShapes.Builder(Block.box(5, 0, 5, 11, 8, 11)).forDirectional();
    public static final VoxelShaper SHAPE = new AllShapes.Builder(GEAR_SHAPE).add(HALF_SHAFT_SHAPE.get(Direction.Axis.Y)).forDirectional();

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;
    public static DeferredHolder<SoundEvent, SoundEvent> REFRESH_SOUND;

    public static void register() {
        BLOCK = GetCreative.BLOCKS.registerBlock(
                "haunted_cogwheel", HauntedCogwheelBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).sound(SoundType.WOOD).mapColor(MapColor.DIRT)
        );
        ITEM = GetCreative.ITEMS.registerSimpleBlockItem(BLOCK);
        HauntedCogwheelBlockEntity.register();

        REFRESH_SOUND = GetCreative.registerSound("haunted_cogwheel_refresh");
    }


    public HauntedCogwheelBlock(Properties properties) {
        super(properties);
        registerDefaultState(super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(BlockStateProperties.WATERLOGGED));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
        return CogWheelBlock.isValidCogwheelPosition(false, worldIn, pos, getRotationAxis(state));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        final boolean shouldWaterlog = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, shouldWaterlog)
                .setValue(FACING, getDirectionForPlacement(context));
    }

    protected Direction getDirectionForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            return context.getClickedFace();

        Level world = context.getLevel();

        BlockPos placedOnPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState placedAgainst = world.getBlockState(placedOnPos);

        Block block = placedAgainst.getBlock();
        if (ICogWheel.isSmallCog(placedAgainst)) {
            final Direction.Axis cogAxis = ((IRotate) block).getRotationAxis(placedAgainst);
            final boolean againstShaft = context.getClickedFace().getAxis() == cogAxis;
            if (!againstShaft) return switch(cogAxis) {
                case Direction.Axis.X -> Direction.EAST;
                case Direction.Axis.Z -> Direction.SOUTH;
                default -> Direction.UP;
            };
        }

        return context.getClickedFace();
    }

    @Override
    public Class<HauntedCogwheelBlockEntity> getBlockEntityClass() {
        return HauntedCogwheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HauntedCogwheelBlockEntity> getBlockEntityType() {
        return HauntedCogwheelBlockEntity.BLOCK_ENTITY.get();
    }
}
