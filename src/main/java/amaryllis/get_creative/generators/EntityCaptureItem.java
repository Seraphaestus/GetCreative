package amaryllis.get_creative.generators;

import amaryllis.get_creative.GetCreative;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.Optional;

public class EntityCaptureItem extends BlockItem {

    protected DeferredItem<?> captureResult;
    public TagKey<EntityType<?>> CAPTURABLE;

    protected ParticleOptions particle1 = ParticleTypes.FLAME;
    protected ParticleOptions particle2 = ParticleTypes.SMOKE;
    protected SoundSource soundSource = SoundSource.HOSTILE;
    protected SoundEvent sound1 = SoundEvents.BLAZE_HURT;
    protected SoundEvent sound2 = SoundEvents.FIRE_EXTINGUISH;

    public EntityCaptureItem(Block block, Properties properties, DeferredItem<?> captureResult, String resultID) {
        super(block, properties);
        this.captureResult = captureResult;
        CAPTURABLE = TagKey.create(Registries.ENTITY_TYPE, GetCreative.ID( resultID + "_capturable"));
    }

    public EntityCaptureItem setParticles(ParticleOptions particle1, ParticleOptions particle2) {
        this.particle1 = particle1;
        this.particle2 = particle2;
        return this;
    }
    public EntityCaptureItem setSounds(SoundSource soundSource, SoundEvent sound1, SoundEvent sound2) {
        this.soundSource = soundSource;
        this.sound1 = sound1;
        this.sound2 = sound2;
        return this;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = world.getBlockEntity(pos);
        Player player = context.getPlayer();

        if (!(be instanceof SpawnerBlockEntity)) return super.useOn(context);

        BaseSpawner spawner = ((SpawnerBlockEntity) be).getSpawner();

        List<SpawnData> possibleSpawns = ((ISpawner)spawner).getCreative$getPossibleSpawns();

        for (SpawnData e : possibleSpawns) {
            Optional<EntityType<?>> optionalEntity = EntityType.by(e.entityToSpawn());
            if (optionalEntity.isEmpty() || !optionalEntity.get().is(CAPTURABLE)) continue;

            spawnCaptureEffects(world, VecHelper.getCenterOf(pos));
            if (world.isClientSide || player == null) return InteractionResult.SUCCESS;

            transformItem(player, context.getItemInHand(), context.getHand());
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack heldItem, Player player, LivingEntity entity, InteractionHand hand) {
        if (!entity.getType().is(CAPTURABLE)) return InteractionResult.PASS;

        Level level = player.level();
        spawnCaptureEffects(level, entity.position());
        if (level.isClientSide) return InteractionResult.FAIL;

        transformItem(player, heldItem, hand);
        entity.discard();
        return InteractionResult.FAIL;
    }

    protected void transformItem(Player player, ItemStack heldItem, InteractionHand hand) {
        ItemStack filled = captureResult.toStack();
        if (!player.isCreative()) heldItem.shrink(1);
        if (heldItem.isEmpty()) {
            player.setItemInHand(hand, filled);
            return;
        }
        player.getInventory().placeItemBackInInventory(filled);
    }

    protected void spawnCaptureEffects(Level level, Vec3 vec) {
        if (level.isClientSide) {
            for (int i = 0; i < 40; i++) {
                Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, level.random, .125f);
                level.addParticle(particle1, vec.x, vec.y, vec.z, motion.x, motion.y, motion.z);

                Vec3 circle = motion.multiply(1, 0, 1).normalize().scale(.5f);
                level.addParticle(particle2, circle.x, vec.y, circle.z, 0, -0.125, 0);
            }
            return;
        }

        BlockPos soundPos = BlockPos.containing(vec);
        level.playSound(null, soundPos, sound1, soundSource, .25f, .75f);
        level.playSound(null, soundPos, sound2, soundSource, .5f, .75f);
    }

    public interface ISpawner {
        List<SpawnData> getCreative$getPossibleSpawns();
    }

}
