package amaryllis.get_creative.control_seat;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlSeatBlock extends SeatBlock implements IBE<ControlSeatBlockEntity> {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static TagKey<Item> TAG = ItemTags.create(GetCreative.ID("control_seats"));

    public static final Map<DyeColor, PartialModel> MODELS = new HashMap<>();

    public static Map<DyeColor, DeferredBlock<ControlSeatBlock>> BLOCKS = new HashMap<>();
    public static Map<DyeColor, DeferredItem<BlockItem>> ITEMS = new HashMap<>();

    public static void register() {
        for (DyeColor color: DyeColor.values()) {
            String colorName = color.getSerializedName();
            var block = GetCreative.BLOCKS.registerBlock(
                    colorName + "_control_seat", p -> new ControlSeatBlock(p, color),
                    Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).mapColor(color));
            BLOCKS.put(color, block);
            ITEMS.put(color, GetCreative.ITEMS.registerSimpleBlockItem(block));
            MODELS.put(color, PartialModel.of(GetCreative.ID("block/control_seat/" + colorName + "_control_seat")));
        }
        ControlSeatBlockEntity.register();
    }

    public ControlSeatBlock(Properties properties, DyeColor color) {
        super(properties, color);
    }

    //region Facing property
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @NotNull BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            facing = facing.getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }
    //endregion

    @Override
    public int getSignal(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Direction oppositeDir) {
        if (!(level instanceof CommonLevelAccessor entityGetter)) return 0;
        if (!(level.getBlockEntity(pos) instanceof ControlSeatBlockEntity controlSeatBE)) return 0;

        Entity passenger = getPassenger(entityGetter, pos);
        if (passenger == null) return 0;

        Direction facing = state.getValue(FACING);
        Direction relativeDirection = getRelativeDirection(oppositeDir.getOpposite(), facing);
        return getSignalForSide(controlSeatBE, passenger, relativeDirection, facing);
    }

    protected Direction getRelativeDirection(Direction direction, Direction facing) {
        return switch (direction.get2DDataValue() - facing.get2DDataValue()) {
            case 0 -> Direction.UP;
            case 1, -3 -> Direction.EAST;
            case 2, -2 -> Direction.DOWN;
            case 3, -1 -> Direction.WEST;
            default -> null;
        };
    }

    protected int getSignalForSide(ControlSeatBlockEntity be, Entity passenger, Direction side, Direction facing) {

        switch (side) {
            // Pitch
            case Direction.UP, Direction.DOWN:
                float xRot = passenger.getXRot();
                int xRange = be.pitchRange.get();
                xRot = Math.clamp(xRot, -xRange, xRange);
                if (side == Direction.DOWN && xRot <= 0) return 0;
                if (side == Direction.UP && xRot >= 0) return 0;
                return (int)(Math.abs(xRot) * 15 / xRange);
            // Yaw
            case Direction.WEST, Direction.EAST:
                float yRot = (passenger instanceof Player player) ? player.getYHeadRot() : passenger.getYRot();
                int yRange = be.yawRange.get();
                float facingAngle = facing.get2DDataValue() * 90 - 180;
                yRot = (yRot - facingAngle + 1800) % 360 - 180;
                yRot = Math.clamp(yRot, -yRange, yRange);
                if (side == Direction.WEST && yRot >= 0) return 0;
                if (side == Direction.EAST && yRot <= 0) return 0;
                return (int)(Math.abs(yRot) * 15 / yRange);
        }
        return 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || player instanceof FakePlayer)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        DyeColor color = DyeColor.getColor(stack);
        if (color != null && color != this.color) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            BlockState newState = BlockHelper.copyProperties(state, BLOCKS.get(color).get().defaultBlockState());
            level.setBlockAndUpdate(pos, newState);
            return ItemInteractionResult.SUCCESS;
        }

        SeatEntity seatEntity = getEntity(level, pos);
        if (seatEntity != null) {
            List<Entity> passengers = seatEntity.getPassengers();
            if (!passengers.isEmpty() && passengers.getFirst() instanceof Player)
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            if (!level.isClientSide) {
                seatEntity.ejectPassengers();
                player.startRiding(seatEntity);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (!level.isClientSide)
            sitDown(level, pos, getLeashed(level, player).or(player));

        return ItemInteractionResult.SUCCESS;
    }

    public Entity getPassenger(EntityGetter entityGetter, BlockPos pos) {
        SeatEntity seatEntity = getEntity(entityGetter, pos);
        if (seatEntity == null) return null;
        List<Entity> passengers = seatEntity.getPassengers();
        return !passengers.isEmpty() ? passengers.getFirst() : null;
    }

    protected SeatEntity getEntity(EntityGetter level, BlockPos pos) {
        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
        return !seats.isEmpty() ? seats.getFirst() : null;
    }

    @Override
    public Class<ControlSeatBlockEntity> getBlockEntityClass() {
        return ControlSeatBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ControlSeatBlockEntity> getBlockEntityType() {
        return ControlSeatBlockEntity.BLOCK_ENTITY.get();
    }
}
