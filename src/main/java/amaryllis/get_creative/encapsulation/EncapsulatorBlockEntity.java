package amaryllis.get_creative.encapsulation;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

public class EncapsulatorBlockEntity extends SmartBlockEntity implements IHaveHoveringInformation, IDisplayAssemblyExceptions {

    public static Supplier<BlockEntityType<EncapsulatorBlockEntity>> BLOCK_ENTITY;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "encapsulator", () -> BlockEntityType.Builder.of(
                        EncapsulatorBlockEntity::new, EncapsulatorBlock.BLOCK.get()
                ).build(null));
    }

    protected String customName;
    protected AssemblyException lastException;

    public EncapsulatorBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);
    }

    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void write(CompoundTag data, HolderLookup.Provider registries, boolean clientPacket) {
        if (customName != null) data.putString("CustomName", customName);
        AssemblyException.write(data, registries, lastException);
        super.write(data, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag data, HolderLookup.Provider registries, boolean clientPacket) {
        customName = data.contains("CustomName") ? data.getString("CustomName") : null;
        lastException = AssemblyException.read(data, registries);
        super.read(data, registries, clientPacket);
    }

    public void setCustomName(String name) {
        customName = name;
        sendData();
    }
    public String getCustomName() {
        return customName;
    }

    public void activate(Level level, BlockPos pos, Direction facing) {
        HelperContraption contraption = new HelperContraption(facing);
        try {
            if (!contraption.assemble(level, pos)) return;
            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return;
        }
        if (contraption.getBlocks().size() < 2) return;

        ArbitraryStructureTemplate structure = new ArbitraryStructureTemplate();
        structure.fillFromBlocks(level, pos.relative(facing), contraption.getBlocks().keySet(), false);

        CompoundTag data = structure.saveAndTrim(level);

        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);

        Vec3i size = ((IStructureTemplate)structure).getCreative$getSize();
        ItemStack capsule = CapsuleItem.create(data, facing.get2DDataValue(), size, customName);
        Block.popResourceFromFace(level, pos, facing, capsule);
    }


    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (isPlayerSneaking || customName == null) return false;

        CreateLang.builder()
                .add(Component.translatable("block.get_creative.encapsulator"))
                .style(ChatFormatting.GOLD)
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("tooltip.get_creative.encapsulator", customName))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        return true;
    }

    @Override
    public AssemblyException getLastAssemblyException() {
        return lastException;
    }
}
