package amaryllis.get_creative.contraptions.hinge_bearing;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.value_settings.ToggleSwitchHandler;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

public class HingeBearingBlock extends BearingBlock implements IBE<HingeBearingBlockEntity> {

    public static final IntegerProperty SELECTED_VALUE_PANEL = IntegerProperty.create("selected_value_panel", 0, 2);

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;

    public static void register() {
        BLOCK = GetCreative.BLOCKS.registerBlock(
                "hinge_bearing", HingeBearingBlock::new,
                Properties.of().explosionResistance(6).destroyTime(1.5f).mapColor(MapColor.PODZOL).noOcclusion());
        ITEM = GetCreative.ITEMS.registerSimpleBlockItem(BLOCK);
        HingeBearingBlockEntity.register();
    }

    public HingeBearingBlock(Properties properties) {
        super(properties);
        registerDefaultState(super.defaultBlockState().setValue(SELECTED_VALUE_PANEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SELECTED_VALUE_PANEL);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.mayBuild() || player.isShiftKeyDown()) return ItemInteractionResult.FAIL;
        if (stack.isEmpty()) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            withBlockEntityDo(level, pos, blockEntity -> {

                if (ToggleSwitchHandler.isHovered(blockEntity, hitResult.getLocation(), hitResult.getDirection(), 3)) {
                    // Immediately clear the active value panel, set the next one after a delay
                    // This accounts for Create being slow to clean up its rendering, so the new panel doesn't overlap the old
                    int nextIndex = blockEntity.swappableScrollValues.clearIndex();
                    level.scheduleTick(pos, this, 6);

                    BlockState newState = state.setValue(SELECTED_VALUE_PANEL, nextIndex);
                    level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
                    level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS);
                }
                else if (blockEntity.running) {
                    if (!blockEntity.tryDisassemble())
                        player.displayClientMessage(Component.translatable("get_creative.hinge_bearing.cannot_disassemble", Component.translatable("block.get_creative.hinge_bearing")), true);
                }
                else blockEntity.assembleNextTick = true;

            });
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (level.getBlockEntity(pos) instanceof HingeBearingBlockEntity hingeBearing) {
            hingeBearing.swappableScrollValues.setIndex(state.getValue(SELECTED_VALUE_PANEL));
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }
    @Override
    protected int getAnalogOutputSignal(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos pos) {
        if (level.isClientSide()) return 0;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof HingeBearingBlockEntity hingeBearing)) return 0;
        return hingeBearing.isHingeTurning() ? 15 : 0;
    }

    @Override
    public Class<HingeBearingBlockEntity> getBlockEntityClass() {
        return HingeBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HingeBearingBlockEntity> getBlockEntityType() {
        return HingeBearingBlockEntity.BLOCK_ENTITY.get();
    }

}
