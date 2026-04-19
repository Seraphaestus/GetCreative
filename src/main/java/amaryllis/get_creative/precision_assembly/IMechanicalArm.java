package amaryllis.get_creative.precision_assembly;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public interface IMechanicalArm {
    boolean canProcessArmAssembly(BlockPos beltPos);
    void startArmAssembly(BlockPos beltPos);
    void completeArmAssembly();

    ItemStack getCreative$getHeldItem();
    boolean getCreative$setHeldItem(ItemStack item);
    void getCreative$damageHeldItem();
}
