package amaryllis.get_creative.ponder.scenes;

import amaryllis.get_creative.ponder.MulticolorChaseAABBInstruction;
import amaryllis.get_creative.precision_assembly.ArmAssembly;
import amaryllis.get_creative.precision_assembly.IMechanicalArm;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderSceneBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MechanicalArmScenes {

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mechanical_arm_processing", "Processing Items using Mechanical Arms");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);

        Selection depotSelection = util.select().position(2, 1, 2);
        BlockPos depotPosition = util.grid().at(2, 1, 2);
        scene.world().showSection(depotSelection, Direction.DOWN);
        scene.idle(5);

        Selection armSelection = util.select().position(2, 1, 3);
        BlockPos armPosition = util.grid().at(2, 1, 3);
        scene.world().setKineticSpeed(armSelection, 0);
        scene.world().showSection(armSelection, Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Mechanical Arms can process items on directly adjacent belts or Depots.")
                .pointAt(util.vector().blockSurface(armPosition, Direction.WEST))
                .placeNearTarget();
        scene.idle(80);

        scene.world().hideSection(depotSelection, Direction.UP);
        scene.idle(5);

        scene.effects().indicateRedstone(armPosition);
        scene.overlay().showOutlineWithText(armSelection, 70)
                .colored(PonderPalette.RED)
                .text("First, before it's placed, the Mechanical Arm must be assigned its targets.")
                .pointAt(util.vector().blockSurface(armPosition, Direction.WEST))
                .placeNearTarget();
        scene.idle(10);
        scene.addKeyframe();
        scene.idle(100);

        scene.world().hideSection(armSelection, Direction.UP);
        scene.idle(5);
        scene.world().showSection(depotSelection, Direction.DOWN);
        scene.idle(20);

        ItemStack armItem = AllBlocks.MECHANICAL_ARM.asStack();
        AABB depotBounds = AllShapes.CASING_13PX.get(Direction.UP).bounds().move(2, 1, 2);
        Object outline = new Object();

        //region Cycling Arm target
        Vec3 targetControlsPos = util.vector().blockSurface(depotPosition, Direction.NORTH);
        scene.overlay().showControls(targetControlsPos, Pointing.RIGHT, 15).rightClick().withItem(armItem);
        scene.idle(7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, outline, depotBounds, 100);
        scene.idle(20);

        scene.overlay().showControls(targetControlsPos, Pointing.RIGHT, 15).rightClick().withItem(armItem);
        scene.idle(7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outline, depotBounds, 100);
        scene.idle(20);

        scene.overlay().showControls(targetControlsPos, Pointing.RIGHT, 30).rightClick().withItem(armItem);
        scene.idle(7);
        chaseBoundingBoxOutline(scene, ArmAssembly.OUTLINE_COLOR, outline, depotBounds, 280);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .colored(PonderPalette.FAST)
                .text("Right-Click to cycle between Input (Blue), Output (Yellow), and Assembly (Purple)")
                .pointAt(util.vector().blockSurface(depotPosition, Direction.WEST))
                .placeNearTarget();
        scene.idle(110);
        //endregion

        scene.world().showSection(armSelection, Direction.DOWN);
        scene.idle(10);
        Vec3 armTop = util.vector().blockSurface(armPosition, Direction.WEST).add(0.5, 1.5, 0);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .text("Once placed, the Arm will try to process items on adjacent selected Assembly targets.")
                .pointAt(armTop)
                .placeNearTarget();
        scene.idle(160);

        scene.effects().indicateSuccess(armPosition);
        scene.world().showSection(util.select().fromTo(2, 1, 4,  2, 1, 5), Direction.DOWN);
        scene.world().showSection(util.select().position(2, 0, 5), Direction.DOWN);
        scene.world().createItemOnBeltLike(depotPosition, Direction.SOUTH, AllItems.GOLDEN_SHEET.asStack());
        scene.idle(10);

        scene.world().setKineticSpeed(armSelection, -48);
        scene.idle(20);
        startArmProcessing(scene, armPosition, depotPosition);
        scene.idle(24);

        ItemStack output = AllItems.INCOMPLETE_PRECISION_MECHANISM.asStack();
        scene.world().removeItemsFromBelt(depotPosition);
        scene.world().createItemOnBeltLike(depotPosition, Direction.UP, output);
        //endArmProcessing(scene, armPosition);
        scene.idle(10);
        scene.world().instructArm(armPosition, ArmBlockEntity.Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);
        scene.overlay().showControls(new Vec3(2.5, 1.5, 2.5), Pointing.UP, 50).withItem(output);
    }

    protected static void chaseBoundingBoxOutline(PonderSceneBuilder builder, int color, Object slot, AABB boundingBox, int duration) {
        builder.addInstruction(new MulticolorChaseAABBInstruction(color, slot, boundingBox, duration));
    }

    protected static void startArmProcessing(CreateSceneBuilder scene, BlockPos armPosition, BlockPos targetPos) {
        scene.world().modifyBlockEntity(armPosition, ArmBlockEntity.class, (armBlockEntity -> {
            ((IMechanicalArm)armBlockEntity).startArmAssembly(targetPos);
        }));
    }
    protected static void endArmProcessing(CreateSceneBuilder scene, BlockPos armPosition) {
        scene.world().modifyBlockEntity(armPosition, ArmBlockEntity.class, (armBlockEntity -> {
            ((IMechanicalArm)armBlockEntity).completeArmAssembly();
        }));
    }
}
