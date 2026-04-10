package amaryllis.get_creative.mixin.contraptions;

import amaryllis.get_creative.contraptions.hinge_bearing.IControlledContraptionEntityAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ControlledContraptionEntity.class)
public class ControlledContraptionEntityAccessor extends AbstractContraptionEntity
    implements IControlledContraptionEntityAccessor
{
    @Shadow protected BlockPos controllerPos;

    public ControlledContraptionEntityAccessor(EntityType<?> type, Level level) {
        super(type, level);
    }

    public BlockPos getCreative$getControllerPos() {
        return controllerPos;
    }

    @Shadow protected void tickContraption() {}
    @Shadow public Vec3 applyRotation(Vec3 localPos, float partialTicks) { return null; }
    @Shadow public Vec3 reverseRotation(Vec3 localPos, float partialTicks) { return null; }
    @Shadow protected StructureTransform makeStructureTransform() { return null; }
    @Shadow protected float getStalledAngle() { return 0; }
    @Shadow protected void handleStallInformation(double x, double y, double z, float angle) {}
    @Shadow public ContraptionRotationState getRotationState() { return null; }
    @Shadow public void applyLocalTransforms(PoseStack matrixStack, float partialTicks) {}
}
