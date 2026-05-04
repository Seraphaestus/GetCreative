package amaryllis.get_creative.appliances.encapsulation;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueItem;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.UniqueLinkedList;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.*;

@EventBusSubscriber(Dist.CLIENT)
public interface GlueHighlighter {

    Set<SuperGlueEntity> highlightedGlueEntities = new HashSet<>();
    Set<SuperGlueEntity> outlinedGlueEntities = new HashSet<>();

    static List<SuperGlueEntity> getConnectedGlueEntities(LevelAccessor level, BlockPos origin) {
        Queue<SuperGlueEntity> frontier = new UniqueLinkedList<>();
        frontier.addAll(level.getEntitiesOfClass(SuperGlueEntity.class, new AABB(origin)));

        List<SuperGlueEntity> visited = new ArrayList<>();
        while (!frontier.isEmpty()) {
            SuperGlueEntity glue = frontier.poll();
            visited.add(glue);

            var overlappingGlue = level.getEntitiesOfClass(SuperGlueEntity.class, glue.getBoundingBox(),
                    entity -> !frontier.contains(entity) && !visited.contains(entity));
            frontier.addAll(overlappingGlue);
        }
        return visited;
    }

    static void spawnParticlesForSuperGlue(SuperGlueEntity superGlue, SimpleParticleType... particles) {
        if (!(superGlue.level() instanceof ServerLevel serverLevel)) return;

        AABB bounds = superGlue.getBoundingBox();
        Vec3 origin = new Vec3(bounds.minX, bounds.minY, bounds.minZ);
        Vec3 extents = new Vec3(bounds.getXsize(), bounds.getYsize(), bounds.getZsize());
        for (Direction.Axis axis: Iterate.axes) {
            Direction.AxisDirection positive = Direction.AxisDirection.POSITIVE;
            double max = axis.choose(extents.x, extents.y, extents.z);
            Vec3 normal = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis, positive).getNormal());
            for (Direction.Axis axis2: Iterate.axes) {
                if (axis2 == axis) continue;
                double max2 = axis2.choose(extents.x, extents.y, extents.z);
                Vec3 normal2 = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis2, positive).getNormal());
                for (Direction.Axis axis3: Iterate.axes) {
                    if (axis3 == axis2 || axis3 == axis) continue;
                    double max3 = axis3.choose(extents.x, extents.y, extents.z);
                    Vec3 normal3 = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis3, positive).getNormal());

                    for (int i = 0; i <= max * 2; i++) {
                        for (int o1: Iterate.zeroAndOne) {
                            for (int o2: Iterate.zeroAndOne) {
                                Vec3 v = origin.add(normal.scale(i / 2f))
                                        .add(normal2.scale(max2 * o1))
                                        .add(normal3.scale(max3 * o2));
                                for (SimpleParticleType particle: particles)
                                    serverLevel.sendParticles(particle, v.x, v.y, v.z, 1, 0, 0, 0, 0);
                            }
                        }
                    }
                    break;
                }
                break;
            }
        }
    }


    static boolean shouldUpdateHighlights(Level level) {
        return level != null && level.getGameTime() % 4 == 0;
    }

    @OnlyIn(Dist.CLIENT)
    static void outlineGlueEntity(SuperGlueEntity glueEntity, boolean highlight) {
        AllSpecialTextures faceTex = highlight ? AllSpecialTextures.GLUE : null;
        Outliner.getInstance().showAABB(glueEntity, glueEntity.getBoundingBox())
                .colored(highlight ? 0x68c586 : 0x4D9162)
                .withFaceTextures(faceTex, faceTex)
                .disableLineNormals()
                .lineWidth(highlight ? 1 / 16f : 1 / 64f);
    }

    // Clear cache
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    static void onClientTick(ClientTickEvent.Pre event) {
        if (!shouldUpdateHighlights(Minecraft.getInstance().level)) return;

        highlightedGlueEntities.clear();
        outlinedGlueEntities.clear();
    }

    // Collect targets
    @OnlyIn(Dist.CLIENT)
    default void outlineTargetedGlueEntities(Level level, BlockPos targetPos, boolean doHighlight) {
        if (!shouldUpdateHighlights(level)) return;

        AABB directTarget = new AABB(targetPos);
        for (SuperGlueEntity glueEntity: getConnectedGlueEntities(level, targetPos)) {
            boolean highlight = doHighlight && glueEntity.getBoundingBox().intersects(directTarget);
            if (highlight) highlightedGlueEntities.add(glueEntity);
            else outlinedGlueEntities.add(glueEntity);
        }
    }

    // Render outlines
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) return;

        Player player = Minecraft.getInstance().player;
        boolean holdingSuperGlue = player != null && player.getMainHandItem().getItem() instanceof SuperGlueItem;
        if (holdingSuperGlue) return;

        for (SuperGlueEntity glueEntity: highlightedGlueEntities) {
            outlineGlueEntity(glueEntity, true);
        }
        for (SuperGlueEntity glueEntity: outlinedGlueEntities) {
            if (highlightedGlueEntities.contains(glueEntity)) continue;
            outlineGlueEntity(glueEntity, false);
        }
    }
}
