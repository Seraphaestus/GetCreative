package amaryllis.get_creative.control_seat;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class ControlSeatEntity extends SeatEntity {

    public static Supplier<EntityType<? extends Entity>> ENTITY_TYPE;

    public static void register() {
        ENTITY_TYPE = GetCreative.ENTITY_TYPES.register("control_seat",
            () -> EntityType.Builder.of(ControlSeatEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.35f)
                    .setTrackingRange(5)
                    .setUpdateInterval(Integer.MAX_VALUE)
                    .setShouldReceiveVelocityUpdates(false)
                    .fireImmune()
                    .build("control_seat")
        );
    }

    public ControlSeatEntity(EntityType<? extends Entity> entityType, Level level) {
        super(entityType, level);
    }

    public ControlSeatEntity(Level level) {
        this(ENTITY_TYPE.get(), level);
        noPhysics = true;
    }

    public static class Render extends EntityRenderer<ControlSeatEntity> {
        public Render(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public boolean shouldRender(ControlSeatEntity entity, Frustum frustum, double camX, double camY, double camZ) {
            return false;
        }

        @Override
        public ResourceLocation getTextureLocation(ControlSeatEntity entity) {
            return null;
        }
    }
}
