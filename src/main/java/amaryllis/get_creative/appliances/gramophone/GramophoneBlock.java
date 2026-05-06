package amaryllis.get_creative.appliances.gramophone;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.SegmentedAnglePrecision;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD;
import static net.minecraft.world.level.block.entity.JukeboxBlockEntity.SONG_ITEM_TAG_ID;

@EventBusSubscriber
public class GramophoneBlock extends Block implements IBE<GramophoneBlockEntity>, IWrenchable {

    public static final IntegerProperty ROTATION_8 = IntegerProperty.create("rotation", 0, 7);
    public static final SegmentedAnglePrecision SEGMENTED_ANGLE_8 = new SegmentedAnglePrecision(3); // 3 bits => 8

    public static final PartialModel MODEL = PartialModel.of(GetCreative.ID("block/gramophone_visual"));
    protected static final VoxelShape SHAPE = Block.box(1, 0, 1,  14, 14, 14);

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;

    public static void register() {
        BLOCK = GetCreative.BLOCKS.registerBlock(
                "gramophone", GramophoneBlock::new,
                Properties.of().strength(2).sound(SoundType.METAL).mapColor(MapColor.TERRACOTTA_YELLOW).noOcclusion()
        );
        ITEM = GetCreative.ITEMS.registerItem("gramophone", GramophoneItem::new);
        GramophoneBlockEntity.register();
    }

    public GramophoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HAS_RECORD, false).setValue(ROTATION_8, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_RECORD, ROTATION_8);
        super.createBlockStateDefinition(builder);
    }

    //#region Rotation
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ROTATION_8, SEGMENTED_ANGLE_8.fromDegrees(context.getRotation()));
    }

    @Override
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION_8, rotation.rotate(state.getValue(ROTATION_8), 8));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION_8, mirror.mirror(state.getValue(ROTATION_8), 8));
    }
    //#endregion

    @Override
    protected boolean canSurvive(@NotNull BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(AllBlocks.TURNTABLE);
    }
    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        return state.canSurvive(level, currentPos)
                ? super.updateShape(state, facing, facingState, level, currentPos, facingPos)
                : Blocks.AIR.defaultBlockState();
    }
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (data.contains(SONG_ITEM_TAG_ID)) {
            level.setBlock(pos, state.setValue(HAS_RECORD, true), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) return;

        if (level.getBlockEntity(pos) instanceof GramophoneBlockEntity gramophoneBE) {
            gramophoneBE.popOutTheItem();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }


    @SubscribeEvent
    public static void onInteractWithTurntable(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) return;

        Level level = event.getLevel();
        if (!level.getBlockState(event.getPos()).is(AllBlocks.TURNTABLE)) return;

        BlockPos gramophonePos = event.getPos().above();
        if (!(level.getBlockEntity(gramophonePos) instanceof GramophoneBlockEntity gramophoneBE)) return;
        BlockState gramophoneState = level.getBlockState(gramophonePos);
        boolean hasRecord = gramophoneState.getValue(HAS_RECORD);

        ItemStack stack = event.getItemStack();
        InteractionResult result;
        if (!stack.isEmpty()) {
            // Inserting record
            if (!hasRecord) {
                ItemInteractionResult iResult = tryInsertRecord(level, gramophonePos, stack, event.getEntity());
                result = iResult.consumesAction() ? iResult.result() : InteractionResult.PASS;
                if (iResult == ItemInteractionResult.CONSUME) event.setUseItem(TriState.TRUE);
            } else {
                result = InteractionResult.PASS;
            }
        } else {
            // Retrieving record
            if (hasRecord) {
                ItemStack record = gramophoneBE.splitTheItem(-1);
                if (!record.isEmpty()) event.getEntity().setItemInHand(event.getHand(), record);

                result = InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                result = InteractionResult.PASS;
            }
        }
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    public static ItemInteractionResult tryInsertRecord(Level level, BlockPos pos, ItemStack stack, Player player) {
        JukeboxPlayable playable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
        if (playable == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        BlockState state = level.getBlockState(pos);
        if (state.is(BLOCK) && !state.getValue(HAS_RECORD)) {
            if (!level.isClientSide) {
                stack = stack.consumeAndReturn(1, player);
                if (level.getBlockEntity(pos) instanceof GramophoneBlockEntity gramophoneBE) {
                    gramophoneBE.setTheItem(stack);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
                }
                player.awardStat(Stats.PLAY_RECORD);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }


    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof GramophoneBlockEntity gramophoneBE) {
            if (gramophoneBE.songPlayer.isPlaying()) return 15;
        }
        return 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GramophoneBlockEntity gramophoneBE) {
            return gramophoneBE.getComparatorOutput();
        }
        return 0;
    }

    public static void spawnMusicParticles(ServerLevel level, BlockPos pos) {
        Vec3 origin = pos.getCenter().add(0, 0.125, 0);
        float offset = level.getRandom().nextInt(4) / 24f;
        level.sendParticles(ParticleTypes.NOTE, origin.x(), origin.y(), origin.z(), 0, offset, 0, 0, 1);
    }

    @Override
    public Class<GramophoneBlockEntity> getBlockEntityClass() {
        return GramophoneBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GramophoneBlockEntity> getBlockEntityType() {
        return GramophoneBlockEntity.BLOCK_ENTITY.get();
    }
}
