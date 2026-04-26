package amaryllis.get_creative.contraptions.hinge_bearing;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;

public class HandleBlock extends WrenchableDirectionalBlock {

    // Used to determine its door-opening sound
    public enum Material { WOODEN, COPPER, IRON }

    public static List<String> WOOD_TYPES = List.of(
            "oak", "spruce", "birch", "jungle",
            "acacia", "dark_oak", "mangrove", "cherry");
    public static Map<String, Material> TYPES = new LinkedHashMap<>();
    static {
        WOOD_TYPES.forEach(type -> TYPES.put(type, Material.WOODEN));
        List.of("bamboo", "crimson", "warped").forEach(type -> TYPES.put(type, Material.WOODEN));
        List.of("copper", "exposed_copper", "weathered_copper", "oxidized_copper").forEach(type -> TYPES.put(type, Material.COPPER));
        List.of("iron", "industrial_iron", "brass").forEach(type -> TYPES.put(type, Material.IRON));
    }
    public static Map<String, DeferredBlock<HandleBlock>> BLOCKS = new HashMap<>();
    public static Map<String, DeferredItem<BlockItem>> ITEMS = new HashMap<>();

    public static VoxelShape[] SHAPES = {
            Block.box(4, 8, 4, 12, 16, 12), // DOWN
            Block.box(4, 0, 4, 12, 8, 12), // UP
            Block.box(4, 4, 8, 12, 12, 16), // NORTH
            Block.box(4, 4, 0, 12, 12, 8), // SOUTH
            Block.box(8, 4, 4, 16, 12, 12), // WEST
            Block.box(0, 4, 4, 8, 12, 12) // EAST
    };

    public Material material;

    public static void register(String type, Material material) {
        final String ID = type + "_handle";
        final var block = GetCreative.BLOCKS.registerBlock(ID, (properties) -> new HandleBlock(properties, material),
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB));
        final var item = GetCreative.ITEMS.registerSimpleBlockItem(block);
        BLOCKS.put(ID, block);
        ITEMS.put(ID, item);
    }

    public HandleBlock(Properties properties, Material material) {
        super(properties);
        this.material = material;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos anchorPos = pos.relative(state.getValue(FACING).getOpposite());
        BlockState anchor = level.getBlockState(anchorPos);
        return !anchor.getCollisionShape(level, anchorPos).isEmpty();
    }
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;

        // Play sound from redstone signal
        int neighborSignal = level.getBestNeighborSignal(pos);
        boolean hasNeighborSignal = neighborSignal > 0;
        if (hasNeighborSignal != state.getValue(POWERED)) {
            if (hasNeighborSignal) playSound(level, pos, neighborSignal > 7);
            level.setBlock(pos, state.setValue(POWERED, hasNeighborSignal), Block.UPDATE_ALL);
        }

        Direction blockFacing = state.getValue(FACING);
        if (fromPos.equals(pos.relative(blockFacing.getOpposite())) && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    // Place sound from interact
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) playSound(level, pos, !player.isShiftKeyDown());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void playSound(Level level, BlockPos pos, boolean isOpening) {
        if (level == null) return;
        SoundEvent sound = switch (material) {
            case Material.WOODEN -> isOpening ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE;
            case Material.COPPER -> isOpening ? SoundEvents.COPPER_DOOR_OPEN : SoundEvents.COPPER_DOOR_CLOSE;
            case Material.IRON ->   isOpening ? SoundEvents.IRON_DOOR_OPEN   : SoundEvents.IRON_DOOR_CLOSE;
        };
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
