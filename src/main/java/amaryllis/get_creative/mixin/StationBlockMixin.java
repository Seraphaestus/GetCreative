package amaryllis.get_creative.mixin;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlock;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StationBlock.class)
public class StationBlockMixin extends Block implements IBE<StationBlockEntity>, IWrenchable, ProperWaterloggedBlock {

    public StationBlockMixin(Properties properties) { super(properties); }

    // Ported for 1.21.1 from Create: Steam 'n' Rails

    @Inject(method = "useItemOn", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
    private void getCreative$deployersAssemble(ItemStack stack, BlockState stationState, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (level.isClientSide ||
            Config.DEPLOYERS_DISASSEMBLE_TRAINS.isFalse() ||
            !(player instanceof DeployerFakePlayer deployerPlayer) ||
            !(level.getBlockEntity(pos) instanceof StationBlockEntity stationBE))
                return;

        cir.setReturnValue(ItemInteractionResult.CONSUME);

        GlobalStation station = stationBE.getStation();
        final boolean isAssemblyMode = stationState.getValue(StationBlock.ASSEMBLING);

        if (station != null && station.getPresentTrain() == null) {
            // Assemble
            if (stationBE.isAssembling() || stationBE.tryEnterAssemblyMode()) {
                // Fix blockstate
                stationBE.assemble(deployerPlayer.getUUID());
                cir.setReturnValue(ItemInteractionResult.SUCCESS);

                if (isAssemblyMode) {
                    level.setBlock(pos, stationState.setValue(StationBlock.ASSEMBLING, false), 3);
                    stationBE.refreshBlockState();
                }
            }
            return;
        }

        final BlockState newState = isAssemblyMode ? null : stationState.setValue(StationBlock.ASSEMBLING, true);
        if (getCreative$disassembleAndEnterMode(deployerPlayer, stationBE)) {
            if (newState != null) {
                level.setBlock(pos, newState, 3);
                stationBE.refreshBlockState();

                stationBE.refreshAssemblyInfo();
            }
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
        }
    }

    private boolean getCreative$disassembleAndEnterMode(ServerPlayer sender, StationBlockEntity stationBE) {
        final GlobalStation station = stationBE.getStation();
        if (station == null) return stationBE.tryEnterAssemblyMode();

        final Train train = station.getPresentTrain();
        final BlockPos trackPosition = stationBE.edgePoint.getGlobalPosition();
        final ItemStack schedule = (train == null) ? ItemStack.EMPTY : train.runtime.returnSchedule(stationBE.getLevel().registryAccess());
        if (train != null && !train.disassemble(stationBE.getAssemblyDirection(), trackPosition.above())) return false;

        getCreative$dropSchedule(sender, stationBE, schedule);
        return stationBE.tryEnterAssemblyMode();
    }

    private void getCreative$dropSchedule(ServerPlayer sender, StationBlockEntity stationBE, ItemStack schedule) {
        if (schedule.isEmpty()) return;
        if (sender.getMainHandItem().isEmpty()) {
            sender.getInventory().placeItemBackInInventory(schedule);
            return;
        }
        final Vec3 pos = VecHelper.getCenterOf(stationBE.getBlockPos());
        final ItemEntity itemEntity = new ItemEntity(stationBE.getLevel(), pos.x, pos.y, pos.z, schedule);
        itemEntity.setDeltaMovement(Vec3.ZERO);
        stationBE.getLevel().addFreshEntity(itemEntity);
    }

    @Shadow
    public Class<StationBlockEntity> getBlockEntityClass() { return null; }

    @Shadow
    public BlockEntityType<? extends StationBlockEntity> getBlockEntityType() { return null; }
}
