package amaryllis.get_creative.mixin;

import amaryllis.get_creative.encapsulation.ISchematicTransformation;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SchematicTransformation.class)
public class SchematicTransformationMixin implements ISchematicTransformation {

    @Shadow BlockPos target;
    @Shadow LerpedFloat rotation;

    public void getCreative$update(BlockPos anchor, Rotation rotation) {
        int r = -rotation.ordinal() * 90;
        this.rotation.chase(0, 3.45f, LerpedFloat.Chaser.EXP).startWithValue(r);
        target = fromAnchor(anchor);
    }

    @Shadow BlockPos fromAnchor(BlockPos pos) { return null; }
}
