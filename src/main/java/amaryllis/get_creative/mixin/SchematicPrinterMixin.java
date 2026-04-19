package amaryllis.get_creative.mixin;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.encapsulation.ISchematicPrinter;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.SchematicProcessor;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.math.BBHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(SchematicPrinter.class)
public class SchematicPrinterMixin implements ISchematicPrinter {

    @Shadow boolean schematicLoaded;
    @Shadow boolean isErrored;
    @Shadow SchematicLevel blockReader;
    @Shadow BlockPos schematicAnchor;

    @Shadow BlockPos currentPos;
    @Shadow int printingEntityIndex;
    @Shadow SchematicPrinter.PrintStage printStage;
    @Shadow List<BlockPos> deferredBlocks;

    public void getCreative$loadStructure(StructureTemplate structure, Level level, BlockPos anchor, Rotation rotation, boolean processNBT) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        if (processNBT) settings.addProcessor(SchematicProcessor.INSTANCE);

        schematicAnchor = anchor;
        blockReader = new SchematicLevel(schematicAnchor, level);

        try {
            structure.placeInWorld(blockReader, schematicAnchor, schematicAnchor, settings,
                    blockReader.getRandom(), Block.UPDATE_CLIENTS);
        } catch (Exception e) {
            GetCreative.LOGGER.error("Failed to load structure for Printing", e);
            schematicLoaded = true;
            isErrored = true;
            return;
        }

        BlockPos extraBounds = StructureTemplate.calculateRelativePosition(settings, new BlockPos(structure.getSize())
                .offset(-1, -1, -1));
        blockReader.setBounds(BBHelper.encapsulate(blockReader.getBounds(), extraBounds));

        StructureTransform transform = new StructureTransform(settings.getRotationPivot(), Direction.Axis.Y,
                settings.getRotation(), settings.getMirror());
        for (BlockEntity be: blockReader.getBlockEntities()) transform.apply(be);

        printingEntityIndex = -1;
        printStage = SchematicPrinter.PrintStage.BLOCKS;
        deferredBlocks.clear();
        BoundingBox bounds = blockReader.getBounds();
        currentPos = new BlockPos(bounds.minX() - 1, bounds.minY(), bounds.minZ());
        schematicLoaded = true;
    }
}
