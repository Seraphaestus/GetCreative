package amaryllis.get_creative.industrial_fan;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.Supplier;

public class IndustrialFanBlockEntity extends EncasedFanBlockEntity {

    public static Supplier<BlockEntityType<IndustrialFanBlockEntity>> BLOCK_ENTITY;

    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, GetCreative.ID("industrial_fan"));

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "industrial_fan", () -> BlockEntityType.Builder.of(
                        IndustrialFanBlockEntity::new, IndustrialFanBlock.BLOCK.get()
                ).build(null));
    }

    public IndustrialFanBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);
        airCurrent = new AreaAirCurrent(this, 1);
    }

    @Override
    public void tick() {
        super.tick();

        if (getLevel() == null) return;

        final AABB damageArea = getDamageArea();
        final int damage = getDamage();
        final var damageSource = new DamageSource(getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE), worldPosition.getCenter());

        getLevel().getEntities(null, damageArea).forEach(entity -> {
            if (entity.isAlive() && entity.getBoundingBox().intersects(damageArea)) {
                entity.hurt(damageSource, damage);
            }
        });
    }

    public Iterable<BlockPos> getFacingBlocks() {
        Direction facing = getBlockState().getValue(EncasedFanBlock.FACING);
        BlockPos origin = getBlockPos().relative(facing);
        int x = (facing.getAxis() == Direction.Axis.X) ? 0 : 1;
        int y = (facing.getAxis() == Direction.Axis.Y) ? 0 : 1;
        int z = (facing.getAxis() == Direction.Axis.Z) ? 0 : 1;
        return BlockPos.MutableBlockPos.betweenClosed(
                origin.getX() - x, origin.getY() - y, origin.getZ() - z,
                origin.getX() + x, origin.getY() + y, origin.getZ() + z
        );
    }

    protected AABB getDamageArea() {
        final Direction facing = this.getBlockState().getValue(EncasedFanBlock.FACING);
        AABB collision = switch (facing) {
            case Direction.EAST, Direction.WEST ->
                    new AABB(worldPosition.getX() + 0.375, worldPosition.getY(), worldPosition.getZ(),
                             worldPosition.getX() + 0.625, worldPosition.getY() + 1, worldPosition.getZ() + 1);
            case Direction.NORTH, Direction.SOUTH ->
                    new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() + 0.375,
                         worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + 0.625);
            default ->
                    new AABB(worldPosition.getX(), worldPosition.getY() + 0.375, worldPosition.getZ(),
                         worldPosition.getX() + 1, worldPosition.getY() + 0.625, worldPosition.getZ() + 1);
        };
        return IndustrialFanBlock.expandAABB(collision, facing, 1);
    }

    protected int getDamage() {
        final float speed = Math.abs(this.speed);
        if (speed <= 8) return 0;
        if (speed <= 32) return 1;
        if (speed <= 64) return 2;
        if (speed <= 128) return 3;
        return 4;
    }

}
