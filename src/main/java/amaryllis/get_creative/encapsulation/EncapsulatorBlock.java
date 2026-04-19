package amaryllis.get_creative.encapsulation;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.TRIGGERED;

public class EncapsulatorBlock extends WrenchableDirectionalBlock implements IBE<EncapsulatorBlockEntity> {

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;

    public static void register() {
        BLOCK = GetCreative.BLOCKS.registerBlock(
                "encapsulator", EncapsulatorBlock::new,
                Properties.of().explosionResistance(6).destroyTime(1.5f).mapColor(MapColor.GOLD));
        ITEM = GetCreative.ITEMS.registerSimpleBlockItem(BLOCK);
        EncapsulatorBlockEntity.register();
    }

    public EncapsulatorBlock(Properties properties) {
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity source, ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_NAME)) return;
        if (level.getBlockEntity(pos) instanceof EncapsulatorBlockEntity encapsulatorBE) {
            encapsulatorBE.setCustomName(stack.get(DataComponents.CUSTOM_NAME).getString());
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME) && level.getBlockEntity(pos) instanceof EncapsulatorBlockEntity encapsulatorBE) {
            encapsulatorBE.setCustomName(stack.get(DataComponents.CUSTOM_NAME).getString());
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
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
        if (level.getBlockEntity(pos) instanceof EncapsulatorBlockEntity encapsulatorBE) {
            encapsulatorBE.activate(level, pos, state.getValue(FACING));
        }
    }

    @Override
    public Class<EncapsulatorBlockEntity> getBlockEntityClass() {
        return EncapsulatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EncapsulatorBlockEntity> getBlockEntityType() {
        return EncapsulatorBlockEntity.BLOCK_ENTITY.get();
    }
}