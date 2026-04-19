package amaryllis.get_creative.ponder;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.world.phys.AABB;

public class MulticolorChaseAABBInstruction extends TickingInstruction {

    private final AABB bb;
    private final Object slot;
    private final int color;

    public MulticolorChaseAABBInstruction(int color, Object slot, AABB bb, int ticks) {
        super(false, ticks);
        this.color = color;
        this.slot = slot;
        this.bb = bb;
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        scene.getOutliner()
                .chaseAABB(slot, bb)
                .lineWidth(1 / 16f)
                .colored(color);
    }
}