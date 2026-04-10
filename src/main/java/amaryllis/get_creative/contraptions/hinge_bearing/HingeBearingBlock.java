package amaryllis.get_creative.contraptions.hinge_bearing;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

public class HingeBearingBlock extends BearingBlock implements IBE<HingeBearingBlockEntity> {

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
                if (blockEntity.running) blockEntity.disassemble();
                else blockEntity.assembleNextTick = true;
            });
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
