package amaryllis.get_creative.mixin.encapsulation;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.encapsulation.ISchematicTransformation;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SchematicTransformation.class)
public class SchematicTransformationMixin implements ISchematicTransformation {

    @Shadow Vec3 chasingPos;
    @Shadow Vec3 prevChasingPos;
    @Shadow BlockPos target;

    @Shadow LerpedFloat rotation;
    @Shadow double xOrigin;
    @Shadow double zOrigin;

    public void getCreative$update(BlockPos anchor, Vec3i anchorOffset, boolean positionHasChanged,
                                   Rotation rotation, Rotation prevRotation) {
        boolean rotationHasChanged = !rotation.equals(prevRotation);
        if (rotationHasChanged) {
            xOrigin = -anchorOffset.getX() + 0.5;
            zOrigin = -anchorOffset.getZ() + 0.5;

            int deltaR = (rotation.ordinal() + 4 - prevRotation.ordinal()) % 4;
            this.rotation.updateChaseTarget(this.rotation.getChaseTarget() - 90 * deltaR);
        }

        target = fromAnchor(anchor);
        if (rotationHasChanged && !positionHasChanged) {
            chasingPos = Vec3.atLowerCornerOf(target);
            prevChasingPos = chasingPos;
        }
    }

    @Shadow BlockPos fromAnchor(BlockPos pos) { return null; }
}
