package amaryllis.get_creative.ponder.scenes;

import amaryllis.get_creative.encapsulation.CapsuleItem;
import com.simibubi.create.content.contraptions.glue.SuperGlueItem;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.instruction.DisplayWorldSectionInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class EncapsulationScenes {

    public static void glue_spreader(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("glue_spreader", "Joining Blocks with the Glue Spreader");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        Selection spreadersSelection = util.select().fromTo(3, 1, 3,  1, 1, 3);
        BlockPos spreaderPosition = util.grid().at(2, 1, 3);
        scene.world().showSection(spreadersSelection, Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Adjacent Glue Spreaders can glue together the blocks in front of them")
                .pointAt(util.vector().blockSurface(spreaderPosition, Direction.UP))
                .placeNearTarget();
        scene.idle(80);

        Selection[] blockSelection = new Selection[] {
                util.select().position(3, 1, 2),
                util.select().position(2, 1, 2),
                util.select().position(1, 1, 2)
        };
        scene.world().showSection(blockSelection[0], Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(blockSelection[1], Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(blockSelection[2], Direction.DOWN);
        scene.idle(10);

        Selection buttonSelection = util.select().position(2, 2, 3);
        BlockPos buttonPosition = util.grid().at(2, 2, 3);
        scene.world().showSection(buttonSelection, Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(110)
                .attachKeyFrame()
                .text("When each Glue Spreader is powered, it takes the blocks in front of adjacent Glue Spreaders and glues them to its own block")
                .pointAt(util.vector().blockSurface(spreaderPosition, Direction.WEST))
                .placeNearTarget();
        scene.idle(120);

        // Button pressed
        scene.world().toggleRedstonePower(buttonSelection);
        scene.effects().indicateRedstone(buttonPosition);
        scene.world().modifyBlocks(spreadersSelection, s -> s.setValue(BlockStateProperties.TRIGGERED, true), false);

        glueParticles(scene, new BlockPos(1, 1, 2), Direction.EAST);
        glueParticles(scene, new BlockPos(2, 1, 2), Direction.EAST);

        scene.idle(20);

        // Button depressed
        scene.world().toggleRedstonePower(buttonSelection);
        scene.world().modifyBlocks(spreadersSelection, s -> s.setValue(BlockStateProperties.TRIGGERED, false), false);
        scene.idle(10);

        // Spin bearing
        Selection kinetics = util.select().fromTo(3, 1, 0,  5, 1, 0).add(util.select().position(5, 0, 0).add(util.select().position(3, 1, 1)));
        scene.world().showSection(kinetics, Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The blocks are now glued together into a moveable contraption")
                .pointAt(util.vector().blockSurface(spreaderPosition, Direction.WEST))
                .placeNearTarget();

        BlockPos bearingPos = util.grid().at(3, 1, 1);
        ElementLink<WorldSectionElement> contraption =
                scene.world().makeSectionIndependent(util.select().fromTo(3, 1, 2, 1, 1, 2));
        scene.world().configureCenterOfRotation(contraption, util.vector().centerOf(bearingPos));
        final int angle = -90;
        final int duration = 30;
        scene.world().rotateSection(contraption, 0, 0, angle, duration);
        scene.world().rotateBearing(bearingPos, angle, duration);

        scene.idle(80);
    }

    public static void encapsulator(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("encapsulator", "Bottling Structures with the Encapsulator");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        Selection encapsulatorSelection = util.select().position(2, 1, 3);
        BlockPos encapsulatorPosition = util.grid().at(2, 1, 3);
        scene.world().showSection(encapsulatorSelection, Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(60)
                .attachKeyFrame()
                .text("The Encapsulator can store blocks in front of it into a placeable Capsule")
                .pointAt(util.vector().blockSurface(encapsulatorPosition, Direction.WEST))
                .placeNearTarget();
        scene.idle(70);

        Selection blocksSelection = util.select().fromTo(2, 1, 2,  3, 1, 2).add(util.select().position(3, 2, 2));
        BlockPos blocksPosition = util.grid().at(3, 2, 2);
        scene.world().showSection(blocksSelection, Direction.WEST);
        scene.idle(10);

        scene.overlay().showText(50)
                .text("Blocks must be connected as if assembling a contraption")
                .pointAt(util.vector().blockSurface(blocksPosition, Direction.UP))
                .placeNearTarget();
        scene.idle(70);

        BlockPos buttonPosition = encapsulatorPosition.above();
        Selection buttonSelection = util.select().position(buttonPosition);
        scene.addInstruction(new DisplayWorldSectionInstruction(5, Direction.DOWN, buttonSelection, scene.getScene()::getBaseWorldSection));
        scene.addKeyframe();
        scene.idle(20);

        // Encapsulate
        scene.world().toggleRedstonePower(buttonSelection);
        scene.effects().indicateRedstone(buttonPosition);
        scene.world().modifyBlocks(encapsulatorSelection, s -> s.setValue(BlockStateProperties.TRIGGERED, true), false);

        scene.world().setBlocks(blocksSelection, Blocks.AIR.defaultBlockState(), false);
        ItemStack capsule = CapsuleItem.ITEM.toStack();
        ElementLink<EntityElement> capsuleEntity = scene.world().createItemEntity(
                util.vector().centerOf(encapsulatorPosition.north()),
                util.vector().of(0, 0.1, -0.1), capsule);
        Vec3 capsulePos = new Vec3(2.25, 1.75, 1.5);

        scene.idle(20);
        scene.world().toggleRedstonePower(buttonSelection);
        scene.world().modifyBlocks(encapsulatorSelection, s -> s.setValue(BlockStateProperties.TRIGGERED, false), false);
        scene.idle(20);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Capsules can be then be placed down in one click, consuming the Capsule.")
                .pointAt(capsulePos)
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(130)
                .colored(PonderPalette.RED)
                .text("Capsules are oriented relative to the perspective of the Encapsulator; if you want to place it on the ground, the Encapsulator must be on the ground level.")
                .pointAt(capsulePos)
                .placeNearTarget();
        scene.idle(140);

        scene.world().modifyEntity(capsuleEntity, Entity::discard);
        scene.idle(10);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("The Encapsulator can also be named in order to label created Capsules.")
                .pointAt(util.vector().blockSurface(encapsulatorPosition, Direction.WEST))
                .placeNearTarget();
        scene.idle(20);
        scene.overlay().showControls(new Vec3(2.25, 1.5, 2.5), Pointing.UP, 50).withItem(Items.NAME_TAG.getDefaultInstance());
        scene.idle(60);
    }

    public static void glueParticles(CreateSceneBuilder scene, BlockPos pos, Direction face) {
        scene.effects().emitParticles(pos.getCenter(), (level, x, y, z) ->
                SuperGlueItem.spawnParticles(level, pos, face, true), 1, 1);
    }

}
