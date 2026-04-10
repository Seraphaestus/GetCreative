package amaryllis.get_creative.linked_controller;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.linked_controller.base.LinkedDeviceItem;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LinkedMacroControllerItem extends LinkedDeviceItem {

    public LinkedMacroControllerItem(Properties properties, int deviceIndex) {
        super(properties, deviceIndex, 14);
    }

    //            < -- X -- >
    //        0 1 2 3 4 5 6 7 8 9 A B C
    //   -2 . . . . . . . . . . . . . .
    // ^ -1 . . . W W . . . . . U U . . .
    // |  0 . . . @ W . . . . . @ U . . .
    // Z  1 . A A . . D D . L L . . R R .
    // |  2 . @ A . . @ D . @ L . . @ R .
    // v  3 . . . S S . . . . . D D . . .
    //    4 . . . @ S .# #.# #. @ D . . .
    //    5 .Q.Q. . . .@ #.@ #. . . .X.X.
    //    6 .@.Q.E.E.           .Z.Z.@.X.
    //    7   . .@.E.           .@.Z. .

    protected static final PartialModel POWERED = PartialModel.of(GetCreative.ID( "item/linked_macro_controller_powered"));
    @Override protected BakedModel getPoweredModel() { return POWERED.get(); }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void transformActiveHeldDevice(PoseTransformStack msr, float equipProgress, float handModifier) {
        msr.translate(0, equipProgress / 3, equipProgress / 4 * handModifier);
        msr.rotateYDegrees(equipProgress * -15 * handModifier);
        msr.rotateZDegrees(equipProgress * -30);
    }
}
