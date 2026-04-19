package amaryllis.get_creative.encapsulation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public interface ISchematicPrinter {
    void getCreative$loadStructure(StructureTemplate structure, Level level, BlockPos anchor, Rotation rotation, boolean processNBT);
}
