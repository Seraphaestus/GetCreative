package amaryllis.get_creative.mixin;

import amaryllis.get_creative.Config;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DeployerBlock.class)
public class DeployerMixin extends DirectionalAxisKineticBlock implements IBE<DeployerBlockEntity> {

    public DeployerMixin(Properties properties) { super(properties); }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (Config.CAN_GIVE_WRENCH_TO_DEPLOYER.isFalse())
            return super.onSneakWrenched(state, context);

        final Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        final ItemStack heldByPlayer = context.getItemInHand().copy();

        if (!context.getLevel().isClientSide) {
            withBlockEntityDo(context.getLevel(), context.getClickedPos(), deployerBE -> {
                DeployerFakePlayer deployerPlayer = deployerBE.getPlayer();
                ItemStack heldByDeployer = deployerPlayer.getMainHandItem().copy();
                if (heldByDeployer.isEmpty() && heldByPlayer.isEmpty()) return;

                player.setItemInHand(context.getHand(), heldByDeployer);
                deployerPlayer.setItemInHand(InteractionHand.MAIN_HAND, heldByPlayer);
                deployerBE.sendData();
            });
        }

        return InteractionResult.SUCCESS;
    }

    @Shadow
    public Class<DeployerBlockEntity> getBlockEntityClass() { return null; }

    @Shadow
    public BlockEntityType<? extends DeployerBlockEntity> getBlockEntityType() { return null; }
}
