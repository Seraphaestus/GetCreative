package amaryllis.get_creative.appliances.encapsulation;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.TRIGGERED;

public class GlueCleanerBlock extends WrenchableDirectionalBlock implements IBE<GlueCleanerBlockEntity> {

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;
    public static DeferredHolder<SoundEvent, SoundEvent> ACTIVATE_SOUND;

    public static void register() {
        BLOCK = GetCreative.BLOCKS.registerBlock(
                "glue_cleaner", GlueCleanerBlock::new,
                Properties.of().explosionResistance(6).destroyTime(1.5f).mapColor(MapColor.METAL));
        ITEM = GetCreative.ITEMS.registerSimpleBlockItem(BLOCK);
        GlueCleanerBlockEntity.register();

        ACTIVATE_SOUND = GetCreative.registerSound("glue_cleaner_activates");
    }

    public GlueCleanerBlock(Properties properties) {
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
        Direction direction = context.getNearestLookingDirection();
        boolean isHoldingShift = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        return defaultBlockState().setValue(FACING, isHoldingShift ? direction : direction.getOpposite());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        boolean hasSignal = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        boolean isTriggered = state.getValue(TRIGGERED);
        if (hasSignal && !isTriggered) {
            level.scheduleTick(pos, this, 4);
            level.setBlock(pos, state.setValue(TRIGGERED, true), 2);
        }
        else if (!hasSignal && isTriggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), 2);
        }
    }

    @Override
    protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (level.getBlockEntity(pos) instanceof GlueCleanerBlockEntity glueCleanerBE) {
            glueCleanerBE.activate(level, pos, state.getValue(FACING));
        }
    }

    @Override
    public Class<GlueCleanerBlockEntity> getBlockEntityClass() {
        return GlueCleanerBlockEntity.class;
    }

    @Override
    public BlockEntityType<GlueCleanerBlockEntity> getBlockEntityType() {
        return GlueCleanerBlockEntity.BLOCK_ENTITY.get();
    }
}
