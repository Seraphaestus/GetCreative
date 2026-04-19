package amaryllis.get_creative.encapsulation;

import com.simibubi.create.content.schematics.SchematicAndQuillItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3i;

import java.util.HashSet;
import java.util.Set;

public class ArbitraryStructureTemplate extends StructureTemplate {

    public static Block IGNORE = Blocks.AIR;

    protected BlockPos minPos;
    public BlockPos offset;
    protected Set<BlockPos> blockPositions;
    protected Set<EntityType<? extends Entity>> entityWhitelist;

    public void fillFromBlocks(Level level, BlockPos anchor, Set<BlockPos> blockPositions, boolean withEntities) {
        this.blockPositions = new HashSet<>();
        blockPositions.forEach(blockPos -> this.blockPositions.add(blockPos.offset(anchor)));

        Vector3i minBound = new Vector3i(Integer.MAX_VALUE);
        Vector3i maxBound = new Vector3i(Integer.MIN_VALUE);
        for (BlockPos pos: this.blockPositions) {
            if (pos.getX() < minBound.x) minBound.x = pos.getX();
            if (pos.getY() < minBound.y) minBound.y = pos.getY();
            if (pos.getZ() < minBound.z) minBound.z = pos.getZ();
            if (pos.getX() > maxBound.x) maxBound.x = pos.getX();
            if (pos.getY() > maxBound.y) maxBound.y = pos.getY();
            if (pos.getZ() > maxBound.z) maxBound.z = pos.getZ();
        }
        minPos = new BlockPos(minBound.x, minBound.y, minBound.z);
        Vec3i _size = new Vec3i(maxBound.x - minBound.x + 1, maxBound.y - minBound.y + 1, maxBound.z - minBound.z + 1);
        fillFromWorld(level, minPos, _size, withEntities, IGNORE);
        offset = minPos.subtract(anchor);
    }

    public void whitelistEntity(EntityType<? extends Entity> entityType) {
        if (entityWhitelist == null) entityWhitelist = new HashSet<>();
        entityWhitelist.add(entityType);
    }


    public boolean shouldStoreBlock(BlockPos pos) {
        return blockPositions.stream().anyMatch(_pos -> _pos.equals(pos));
    }

    public boolean shouldStoreEntity(Entity entity) {
        return entityWhitelist == null || entityWhitelist.contains(entity.getType());
    }


    public CompoundTag saveAndTrim(Level level) {
        Vec3i size = ((IStructureTemplate)this).getCreative$getSize();
        BlockPos bounds = new BlockPos(size);

        CompoundTag data = save(new CompoundTag());
        SchematicAndQuillItem.replaceStructureVoidWithAir(data);
        SchematicAndQuillItem.clampGlueBoxes(level, new AABB(Vec3.atLowerCornerOf(minPos), Vec3.atLowerCornerOf(minPos.offset(bounds))), data);

        data.put("Offset", NbtUtils.writeBlockPos(offset));
        return data;
    }
}
