package amaryllis.get_creative.block_breaking;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class KineticBlockBreaking {

    public static TagKey<Block> DRILL_MINEABLE_TAG = TagKey.create(Registries.BLOCK, GetCreative.ID("create", "mineable/mechanical_drill"));
    public static TagKey<Block> SAW_MINEABLE_TAG = TagKey.create(Registries.BLOCK, GetCreative.ID("create", "mineable/mechanical_saw"));

    public static float getBreakSpeedModifier(Block blockBreaker, BlockState target) {
        TagKey<Block> mineableTag = null;
        float speedModifier = 1;

        if (blockBreaker instanceof DrillBlock) {
            speedModifier = Config.DRILL_SPEED_MODIFIER.get().floatValue();
            mineableTag = DRILL_MINEABLE_TAG;
        } else if (blockBreaker instanceof SawBlock) {
            speedModifier = Config.SAW_SPEED_MODIFIER.get().floatValue();
            mineableTag = SAW_MINEABLE_TAG;
        }

        if (mineableTag != null && Float.compare(speedModifier, 1) != 0) {
            if (!target.is(mineableTag)) return speedModifier;
        }

        return 1;
    }

}
