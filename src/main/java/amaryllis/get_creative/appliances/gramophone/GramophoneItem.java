package amaryllis.get_creative.appliances.gramophone;

import com.simibubi.create.AllBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class GramophoneItem extends BlockItem {

    public GramophoneItem(Block block, Item.Properties properties) {
        super(block, properties);
    }
    public GramophoneItem(Item.Properties properties) {
        super(GramophoneBlock.BLOCK.get(), properties);
    }

    @Override
    public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace().getOpposite());

        BlockState state = context.getLevel().getBlockState(pos);
        if (!state.is(AllBlocks.TURNTABLE)) {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                var message = Component.translatable("get_creative.gramophone.invalid_placement").withStyle(ChatFormatting.RED);
                serverPlayer.sendSystemMessage(message, true);
            }
            return null;
        }

        return BlockPlaceContext.at(context, pos.above(), Direction.UP);
    }

}
