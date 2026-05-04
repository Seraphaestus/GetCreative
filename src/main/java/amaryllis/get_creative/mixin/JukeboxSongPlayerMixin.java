package amaryllis.get_creative.mixin;

import amaryllis.get_creative.appliances.gramophone.GramophoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(JukeboxSongPlayer.class)
public class JukeboxSongPlayerMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/JukeboxSongPlayer;spawnMusicParticles(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V"))
    private void injected(LevelAccessor level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverlevel)) return;

        if (level.getBlockState(pos).is(GramophoneBlock.BLOCK)) {
            GramophoneBlock.spawnMusicParticles(serverlevel, pos);
        }
        else {
            // Vanilla code from private method JukeboxSongPlayer::spawnMusicParticles
            Vec3 origin = Vec3.atBottomCenterOf(pos).add(0, 1.2, 0);
            float offset = level.getRandom().nextInt(4) / 24f;
            serverlevel.sendParticles(ParticleTypes.NOTE, origin.x(), origin.y(), origin.z(), 0, offset, 0, 0, 1);
        }
    }
}
