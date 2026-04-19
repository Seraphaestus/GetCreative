package amaryllis.get_creative.mixin.precision_assembly;

import amaryllis.get_creative.precision_assembly.ArmAssembly;
import amaryllis.get_creative.precision_assembly.ArmAssemblyBehaviour;
import amaryllis.get_creative.precision_assembly.IArmInteractionPoint;
import amaryllis.get_creative.precision_assembly.IMechanicalArm;
import com.simibubi.create.Create;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.SelectionMode;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ArmBlockEntity.class)
public class MechanicalArmMixin extends KineticBlockEntity implements TransformableBlockEntity, IMechanicalArm {

    @Shadow List<ArmInteractionPoint> inputs;
    @Shadow List<ArmInteractionPoint> outputs;

    @Shadow float chasedPointProgress;
    @Shadow int chasedPointIndex;
    @Shadow ItemStack heldItem;
    @Shadow Phase phase;

    @Shadow int tooltipWarmup;

    @Shadow ScrollOptionBehaviour<SelectionMode> selectionMode;
    @Shadow int lastInputIndex = -1;
    @Shadow int lastOutputIndex = -1;
    @Shadow boolean redstoneLocked;

    protected ArmAssemblyBehaviour armAssemblyBehaviour;
    protected boolean processingArmAssembly = false;

    public MechanicalArmMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) { super(typeIn, pos, state); }

    @Redirect(method = "searchForDestination", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint;isValid()Z"))
    private boolean blacklistAssembleTargetsFromOutputs(ArmInteractionPoint output) {
        // NB the context is !armInteractionPoint.isValid(). Since we're replacing the isValid call, it's still inverted
        return output.isValid() && !((IArmInteractionPoint)output).isAssemblyTarget();
    }


    @Shadow public void transform(BlockEntity blockEntity, StructureTransform transform) {}
    @Shadow private void selectIndex(boolean input, int index) {}
    @Shadow protected void initInteractionPoints() {}
    @Shadow private boolean tickMovementProgress() { return false; }

    @Inject(method = "write", at = @At(value = "HEAD"))
    public void getCreative$write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo cbi) {
        compound.putBoolean("ProcessingArmAssembly", processingArmAssembly);
    }

    @Inject(method = "read", at = @At(value = "HEAD"))
    protected void getCreative$read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo cbi) {
        processingArmAssembly = tag.getBoolean("ProcessingArmAssembly");
    }

    @Inject(method = "addBehaviours", at = @At(value = "RETURN"))
    public void getCreative$addBehaviours(List<BlockEntityBehaviour> behaviours, CallbackInfo cbi) {
        armAssemblyBehaviour = new ArmAssemblyBehaviour((ArmBlockEntity)(Object)this);
        behaviours.add(armAssemblyBehaviour);
    }

    @Inject(method = "collectItem", at = @At(value = "HEAD"), cancellable = true)
    public void getCreative$skipCollectingItem(CallbackInfo cbi) {
        if (processingArmAssembly) cbi.cancel();
    }

    //region Allow the Arm to pick up assembly catalysts (with a valid assembly target but no empty outputs)
    @Inject(method = "searchForItem", at = @At(value = "HEAD"), cancellable = true)
    protected void getCreative$searchItemForAssembly(CallbackInfo cbi) {
        if (redstoneLocked) return;

        int startIndex = (selectionMode.get() == SelectionMode.PREFER_FIRST) ? 0 : lastInputIndex + 1;
        int scanRange = (selectionMode.get() == SelectionMode.FORCED_ROUND_ROBIN) ? lastInputIndex + 2 : inputs.size();
        if (scanRange > inputs.size()) scanRange = inputs.size();

        ArmBlockEntity thisInstance = (ArmBlockEntity)(Object)this;
        for (int i = startIndex; i < scanRange; i++) {
            ArmInteractionPoint inputPoint = inputs.get(i);
            if (!inputPoint.isValid()) continue;
            for (int slot = 0; slot < inputPoint.getSlotCount(thisInstance); slot++) {
                if (getDistributableAmountForAssembly(inputPoint, slot) > 0) {
                    selectIndex(true, i);
                    if (lastInputIndex == inputs.size() - 1) lastInputIndex = -1;
                    cbi.cancel();
                    return;
                }
            }
        }
    }

    @Inject(method = "collectItem", at = @At(value = "HEAD"), cancellable = true)
    protected void getCreative$collectItemForAssembly(CallbackInfo cbi) {
        ArmInteractionPoint inputPoint = getTargetedInteractionPoint();
        if (inputPoint == null || !inputPoint.isValid()) return;

        ArmBlockEntity thisInstance = (ArmBlockEntity)(Object)this;
        for (int slot = 0; slot < inputPoint.getSlotCount(thisInstance); slot++) {
            int amountExtracted = getDistributableAmountForAssembly(inputPoint, slot);
            if (amountExtracted <= 0) continue;

            // Grab item for assembly
            ItemStack prevHeld = heldItem;
            heldItem = inputPoint.extract(thisInstance, slot, amountExtracted, false);
            phase = Phase.SEARCH_OUTPUTS;
            chasedPointProgress = 0;
            chasedPointIndex = -1;
            sendData();
            setChanged();

            if (!ItemStack.isSameItem(heldItem, prevHeld))
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.125f, 0.5f + Create.RANDOM.nextFloat() * 0.25f);

            cbi.cancel();
            return;
        }
    }

    protected int getDistributableAmountForAssembly(ArmInteractionPoint inputPoint, int slot) {
        if (!heldItem.isEmpty()) return -1;
        ArmBlockEntity thisInstance = (ArmBlockEntity)(Object)this;

        ItemStack stackToGrab = inputPoint.extract(thisInstance, slot, true);
        if (stackToGrab.isEmpty()) return -1;

        ItemStack remainder = simulateInsertion(stackToGrab);
        int distributableAmount = stackToGrab.getCount() - (ItemStack.isSameItem(stackToGrab, remainder) ? remainder.getCount() : 0);
        if (distributableAmount != 0) return -1; // If it's non-zero we default to the base input->output behaviour

        // Check if the item in this input is a valid recipe catalyst for an item in an assembly target spot
        for (ArmInteractionPoint outputPoint: outputs) {
            if (!outputPoint.isValid()) continue;
            if (!((IArmInteractionPoint)outputPoint).isAssemblyTarget()) continue;

            ItemStack assemblyInput = outputPoint.extract(thisInstance, slot, true);
            if (assemblyInput.isEmpty()) continue;
            if (ArmAssembly.getRecipe(level, assemblyInput, stackToGrab).isPresent()) {
                // This output requires us to grab this item for assembly, so grab the whole stack
                return stackToGrab.getCount();
            }
        }
        return -1;
    }
    //endregion

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    public void getCreative$skipLockingItem(CallbackInfo cbi) {
        if (processingArmAssembly) {
            // Hacky code reduplication to simulate the origin up until the part we want to exit
            super.tick();
            initInteractionPoints();
            boolean targetReached = tickMovementProgress();
            if (tooltipWarmup > 0) tooltipWarmup--;
            //
            cbi.cancel();
        }
    }

    public ItemStack getCreative$getHeldItem() {
        return heldItem;
    }
    public boolean getCreative$setHeldItem(ItemStack item) {
        if (!heldItem.isEmpty()) return false;
        heldItem = item;
        return true;
    }
    public void getCreative$damageHeldItem() {
        if (heldItem.getMaxDamage() > 0) {
            heldItem.hurtAndBreak(1, (ServerLevel) level, null, item -> {});
            return;
        }
        ItemStack leftover = heldItem.getCraftingRemainingItem();
        heldItem.shrink(1);
        if (!leftover.isEmpty()) {
            if (heldItem.isEmpty()) heldItem = leftover;
            else {
                Block.popResource(level, worldPosition, leftover);
            }
        }
    }

    public boolean canProcessArmAssembly(BlockPos beltPos) {
        if (processingArmAssembly) {
            BlockPos currentTarget = (lastOutputIndex >= 0 && lastOutputIndex < outputs.size())
                                     ? outputs.get(lastOutputIndex).getPos() : null;
            if (!beltPos.equals(currentTarget)) return false;
        }
        if (getSpeed() == 0) return false;

        for (ArmInteractionPoint target: outputs) {
            if (ArmAssembly.isTarget(target, beltPos)) return true;
        }
        return false;
    }

    public void startArmAssembly(BlockPos beltPos) {
        int targetIndex = -1;
        for (int i = 0; i < outputs.size(); i++) {
            if (ArmAssembly.isTarget(outputs.get(i), beltPos)) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex == -1) return;

        processingArmAssembly = true;
        selectIndex(false, targetIndex);
    }

    public void completeArmAssembly() {
        processingArmAssembly = false;
        phase = Phase.SEARCH_INPUTS;
    }

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    public void invalidTargetTooltip(List<Component> tooltip, boolean isPlayerSneaking, CallbackInfoReturnable<Boolean> callback) {
        if (!isPlayerSneaking) {
            for (ArmInteractionPoint target: outputs) {
                if (!((IArmInteractionPoint)target).isAssemblyTarget()) continue;

                String error = ArmAssembly.validateTarget(level, getBlockPos(), target.getPos());
                if (error != null) {
                    CreateLang.translate("hint.mechanical_arm.error_title").style(ChatFormatting.GOLD).forGoggles(tooltip);
                    Component hint = CreateLang.translateDirect("hint.mechanical_arm." + error);
                    List<Component> cutComponent = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
                    for (Component component: cutComponent) CreateLang.builder().add(component).forGoggles(tooltip);

                    callback.setReturnValue(true);
                    callback.cancel();
                }
            }
        }
    }

    @Shadow private ItemStack simulateInsertion(ItemStack stack) { return null; }
    @Shadow @Nullable private ArmInteractionPoint getTargetedInteractionPoint() { return null; }
}
