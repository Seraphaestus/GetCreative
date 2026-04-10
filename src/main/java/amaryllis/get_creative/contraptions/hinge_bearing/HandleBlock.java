package amaryllis.get_creative.contraptions.hinge_bearing;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
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

        Direction blockFacing = state.getValue(FACING);
        if (fromPos.equals(pos.relative(blockFacing.getOpposite())) &&
            !canSurvive(state, level, pos)) {
                level.destroyBlock(pos, true);
        }
    }

}
