package amaryllis.get_creative.industrial_fan;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class IndustrialFanBlock extends DirectionalKineticBlock implements IBE<IndustrialFanBlockEntity>  {

    public static final PartialModel MODEL = PartialModel.of(GetCreative.ID("item/industrial_fan"));

    // Idk why but these PartialModels are temperamental about what class they want to be in, so here it goes
    public static final PartialModel MECHANICAL_ARM_MODEL = PartialModel.of(GetCreative.ID( "create", "block/mechanical_arm/arm_only"));

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;

    public static void register() {
        BLOCK = GetCreative.BLOCKS.registerBlock(
                "industrial_fan", IndustrialFanBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()
        );
        ITEM = GetCreative.ITEMS.registerSimpleBlockItem(BLOCK);
        IndustrialFanBlockEntity.register();
    }

    public IndustrialFanBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, worldIn, pos, oldState, isMoving);
        blockUpdate(state, worldIn, pos);
    }

    @Override
    public void updateIndirectNeighbourShapes(BlockState stateIn, LevelAccessor worldIn, BlockPos pos, int flags, int count) {
        super.updateIndirectNeighbourShapes(stateIn, worldIn, pos, flags, count);
        blockUpdate(stateIn, worldIn, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        blockUpdate(state, worldIn, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                ? context.getClickedFace().getOpposite() : context.getClickedFace());
    }

    protected void blockUpdate(BlockState state, LevelAccessor worldIn, BlockPos pos) {
        if (worldIn instanceof WrappedLevel) return;
        notifyFanBlockEntity(worldIn, pos);
    }

    protected void notifyFanBlockEntity(LevelAccessor world, BlockPos pos) {
        withBlockEntityDo(world, pos, IndustrialFanBlockEntity::blockInFrontChanged);
    }

    @Override
    public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
        blockUpdate(newState, context.getLevel(), context.getClickedPos());
        return newState;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING) || face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Class<IndustrialFanBlockEntity> getBlockEntityClass() {
        return IndustrialFanBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends IndustrialFanBlockEntity> getBlockEntityType() {
        return IndustrialFanBlockEntity.BLOCK_ENTITY.get();
    }

    public static AABB expandAABB(AABB bounds, Direction facing, int radius) {
        return bounds.inflate(
                (facing != Direction.EAST && facing != Direction.WEST) ? radius : 0,
                (facing != Direction.UP && facing != Direction.DOWN) ? radius : 0,
                (facing != Direction.NORTH && facing != Direction.SOUTH) ? radius : 0);
    }

}
