package amaryllis.get_creative.mixin.encapsulation;

import amaryllis.get_creative.encapsulation.ArbitraryStructureTemplate;
import amaryllis.get_creative.encapsulation.IStructureTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin implements IStructureTemplate {

    @Shadow Vec3i size;
    @Shadow List<StructureTemplate.Palette> palettes;
    @Shadow List<StructureTemplate.StructureEntityInfo> entityInfoList;

    // We can't override this method because we can't get access to Palette's constructor
    // If we pass in a dummy Block type to ignore, then we can conditionally intercept the method caching
    // each block's BlockState with a copy of this dummy Block, making it think that it should ignore this block
    @Redirect(method = "fillFromWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    public BlockState getCreative$filterBlocks(Level level, BlockPos pos) {
        if ((Object) this instanceof ArbitraryStructureTemplate aST) {
            if (!aST.shouldStoreBlock(pos)) return ArbitraryStructureTemplate.IGNORE.defaultBlockState();
        }
        return level.getBlockState(pos);
    }

    // We also want to filter what entities are allowed inside the structure
    @Redirect(method = "fillEntityList", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private <T extends Entity> List<T> getCreative$filterEntities(Level level, Class<T> clazz, AABB area, final Predicate<? super T> filter) {
        if ((Object) this instanceof ArbitraryStructureTemplate aST) {
            return level.getEntitiesOfClass(clazz, area, entity -> filter.test(entity) && aST.shouldStoreEntity(entity));
        }
        return level.getEntitiesOfClass(clazz, area, filter);
    }

    public void getCreative$setSize(Vec3i size) {
        this.size = size;
    }
    public Vec3i getCreative$getSize() {
        return this.size;
    }
}
