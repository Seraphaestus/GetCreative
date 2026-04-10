package amaryllis.get_creative.industrial_fan;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.AirFlowParticleData;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AreaAirCurrent extends AirCurrent {

    public int radius;

    public AreaAirCurrent(IAirCurrentSource source, int radius) {
        super(source);
        this.radius = radius;
    }

    @Override
    public void tick() {
        if (direction == null) rebuild();
        Level world = source.getAirCurrentWorld();

        if (world != null && world.isClientSide) {
            float offset = pushing ? 0.5f : maxDistance + .5f;
            for (int u = -radius; u <= radius; u++) {
                for (int v = -radius; v <= radius; v++) {
                    Vec3 areaOffset = switch (direction) {
                        case Direction.EAST, Direction.WEST -> new Vec3(0, u, v);
                        case Direction.NORTH, Direction.SOUTH -> new Vec3(u, v, 0);
                        default -> new Vec3(u, 0, v);
                    };

                    Vec3 pos = VecHelper.getCenterOf(source.getAirCurrentPos())
                            .add(Vec3.atLowerCornerOf(direction.getNormal())
                            .add(areaOffset)
                            .scale(offset));
                    if (world.random.nextFloat() < AllConfigs.client().fanParticleDensity.get() / (radius * 2 + 1))
                        world.addParticle(new AirFlowParticleData(source.getAirCurrentPos()), pos.x, pos.y, pos.z, 0, 0, 0);
                }
            }
        }

        tickAffectedEntities(world);
        tickAffectedHandlers();
    }

    @Override
    public void findAffectedHandlers() {
        // Rebuild bounding box
        if (maxDistance >= 0.25f && radius >= 1) {
            bounds = IndustrialFanBlock.expandAABB(bounds, direction, radius);
        }

        super.findAffectedHandlers();
    }
}
