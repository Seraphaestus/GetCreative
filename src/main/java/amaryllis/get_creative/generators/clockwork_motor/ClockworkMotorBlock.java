package amaryllis.get_creative.generators.clockwork_motor;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class ClockworkMotorBlock extends DirectionalKineticBlock implements IBE<ClockworkMotorBlockEntity>  {

    public static final PartialModel MODEL = PartialModel.of(GetCreative.ID("block/clockwork_motor_mechanism"));

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;

    public static void register() {
        BLOCK = GetCreative.BLOCKS.registerBlock(
                "clockwork_motor", ClockworkMotorBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).sound(SoundType.WOOD).mapColor(MapColor.DIRT).noOcclusion()
        );
        ITEM = GetCreative.ITEMS.registerSimpleBlockItem(BLOCK);
        ClockworkMotorBlockEntity.register();
    }

    public ClockworkMotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) { return state.getValue(FACING).getAxis(); }

    public static Direction getInputDirection(BlockState state) { return state.getValue(FACING); }
    public static Direction getOutputDirection(BlockState state) { return state.getValue(FACING).getOpposite(); }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == getInputDirection(state) || face == getOutputDirection(state);
    }

    @Override
    public Direction getPreferredFacing(BlockPlaceContext context) {
        return null;
    }

    @Override
    public Class<ClockworkMotorBlockEntity> getBlockEntityClass() { return ClockworkMotorBlockEntity.class; }

    @Override
    public BlockEntityType<? extends ClockworkMotorBlockEntity> getBlockEntityType() {
        return ClockworkMotorBlockEntity.BLOCK_ENTITY.get();
    }
}
