package amaryllis.get_creative.contraptions;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.GetCreative;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

public class ActorConfigHandler {

    private static final HashSet<Block> disabledActors = new HashSet<>();
    private static HashSet<Block> gravityOnlyActors;

    private static void cacheGravityActors() {
        gravityOnlyActors = new HashSet<>();
        Config.GRAVITY_ONLY_ACTORS.get().forEach(blockID -> {
            var block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(blockID));
            block.ifPresent(value -> gravityOnlyActors.add(value));
        });
    }

    public static boolean isActorDisabled(Block block) {
        return disabledActors.contains(block);
    }

    public static boolean isActorGravityOnly(Block block) {
        if (gravityOnlyActors == null) cacheGravityActors();
        return gravityOnlyActors.contains(block);
    }

    public static boolean shouldRegisterActor(Block block, MovementBehaviour behaviour) {
        if (!Config.isLoaded()) {
            GetCreative.LOGGER.warn("Could not check to disable actor {} because config isn't loaded yet", block);
            return true;
        }

        // Can't cache like gravityOnlyActors because the block's actor is registered before the block itself is registered
        final String blockID = BuiltInRegistries.BLOCK.getKey(block).toString();
        boolean isDisabled = Config.ACTOR_BLACKLIST.get().contains(blockID) && !Config.GRAVITY_ONLY_ACTORS.get().contains(blockID);
        if (isDisabled) disabledActors.add(block);
        return !isDisabled;
    }

    public static boolean shouldDisableActor(Block block, MovementContext context) {
        boolean isFalling = context.motion.normalize().dot( new Vec3(0, -1, 0)) > 0.7;
        if (isFalling) return false;

        return isActorGravityOnly(block);
    }

}
