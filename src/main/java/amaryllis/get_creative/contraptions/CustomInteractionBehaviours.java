package amaryllis.get_creative.contraptions;

import amaryllis.get_creative.contraptions.hinge_bearing.HandleBlock;
import amaryllis.get_creative.contraptions.moving_interaction.HandleMovingInteraction;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlock;
import amaryllis.get_creative.contraptions.moving_interaction.LecternControllerMovingInteraction;
import amaryllis.get_creative.contraptions.moving_interaction.LecternDeviceMovingInteraction;
import amaryllis.get_creative.contraptions.moving_interaction.LecternMovingInteraction;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.tuple.MutablePair;

import static com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour.REGISTRY;

public class CustomInteractionBehaviours {

    public static void registerDefaults() {
        REGISTRY.register(Blocks.LECTERN, new LecternMovingInteraction());
        REGISTRY.register(AllBlocks.LECTERN_CONTROLLER.get(), new LecternControllerMovingInteraction());
        REGISTRY.register(LecternDeviceBlock.BLOCK.get(), new LecternDeviceMovingInteraction());

        HandleBlock.BLOCKS.forEach((ID, block) -> {
                REGISTRY.register(block.get(), new HandleMovingInteraction());
        });
    }

    // Helper method for replacing block states on a contraptions
    public static void replaceBlock(AbstractContraptionEntity contraptionEntity, BlockPos pos, BlockState state, CompoundTag data, BlockEntity blockEntity) {
        final var contraption = contraptionEntity.getContraption();

        final var prevBlockEntity = contraption.presentBlockEntities.get(pos);
        if (prevBlockEntity != null) {
            contraption.presentBlockEntities.remove(pos);
            contraption.renderedBlockEntities.remove(prevBlockEntity);
        }
        contraption.getActors().removeIf(actor -> actor.left.pos().equals(pos));
        contraption.getInteractors().remove(pos);

        final var info = new StructureTemplate.StructureBlockInfo(pos, state, data);
        if (!contraptionEntity.level().isClientSide()) contraptionEntity.setBlock(pos, info);

        if (blockEntity != null) {
            contraption.presentBlockEntities.put(pos, blockEntity);
            contraption.renderedBlockEntities.add(blockEntity);
        }
        final var actor = MovementBehaviour.REGISTRY.get(state.getBlock());
        if (actor != null) contraption.getActors().add(new MutablePair<>(info, new MovementContext(contraptionEntity.level(), info, contraption)));
        final var interactor = REGISTRY.get(state.getBlock());
        if (interactor != null) contraption.getInteractors().put(pos, interactor);
    }

}
