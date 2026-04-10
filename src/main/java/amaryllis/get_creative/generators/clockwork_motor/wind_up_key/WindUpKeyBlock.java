package amaryllis.get_creative.generators.clockwork_motor.wind_up_key;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber
public class WindUpKeyBlock extends HandCrankBlock {

    public static final PartialModel MODEL = PartialModel.of(GetCreative.ID("block/wind_up_key"));

    protected static final VoxelShape TOP_SHAPE = Block.box(2, 9, 2, 14, 15, 14);
    protected static final VoxelShaper HALF_SHAFT_SHAPE = new AllShapes.Builder(Block.box(5, 0, 5, 11, 9, 11)).forDirectional();
    public static final VoxelShaper SHAPE = new AllShapes.Builder(TOP_SHAPE).add(HALF_SHAFT_SHAPE.get(Direction.Axis.Y)).forDirectional();

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;

    public static void register() {
        BLOCK = GetCreative.BLOCKS.registerBlock(
                "wind_up_key", WindUpKeyBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).sound(SoundType.WOOD).mapColor(MapColor.DIRT)
        );
        ITEM = GetCreative.ITEMS.registerSimpleBlockItem(BLOCK);
        WindUpKeyBlockEntity.register();
    }

    public WindUpKeyBlock(Properties properties) {
        super(properties);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockActivated(PlayerInteractEvent.RightClickBlock event) {
        final BlockPos pos = event.getPos();
        final Level level = event.getLevel();
        final Player player = event.getEntity();
        final BlockState blockState = level.getBlockState(pos);

        if (!(blockState.getBlock() instanceof WindUpKeyBlock windUpKey)) return;
        if (!player.mayBuild()) return;
        if (AllItems.WRENCH.isIn(player.getItemInHand(event.getHand())) && player.isShiftKeyDown()) return;

        if (windUpKey.interacted(level, pos, blockState, player, event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    public boolean interacted(Level level, BlockPos pos, BlockState blockState, Player player, InteractionHand hand) {
        onBlockEntityUse(level, pos,
                be -> (be instanceof WindUpKeyBlockEntity windUpKeyBE) &&
                                          windUpKeyBE.activate(player.isShiftKeyDown())
                        ? InteractionResult.SUCCESS : InteractionResult.PASS);
        return true;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }

    @Override
    public BlockEntityType<? extends HandCrankBlockEntity> getBlockEntityType() {
        return WindUpKeyBlockEntity.BLOCK_ENTITY.get();
    }
}
