package amaryllis.get_creative.mixin;

import amaryllis.get_creative.appliances.gramophone.GramophoneBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow Minecraft minecraft;
    @Shadow ClientLevel level;
    @Shadow Map<BlockPos, SoundInstance> playingJukeboxSongs;

    @Inject(method = "playJukeboxSong", at = @At("HEAD"), cancellable = true)
    public void modifyPitchForGramophone(Holder<JukeboxSong> songHolder, BlockPos pos, CallbackInfo cbi) {
        if (level != null && level.getBlockEntity(pos) instanceof GramophoneBlockEntity gramophoneBE) {
            stopJukeboxSong(pos);
            Vec3 center = Vec3.atCenterOf(pos);
            JukeboxSong song = songHolder.value();
            SoundInstance sound = new SimpleSoundInstance(song.soundEvent().value(), SoundSource.RECORDS, 4, gramophoneBE.getPitch(true),
                    SoundInstance.createUnseededRandom(), center.x, center.y, center.z);

            playingJukeboxSongs.put(pos, sound);
            minecraft.getSoundManager().play(sound);
            minecraft.gui.setNowPlaying(song.description());
            notifyNearbyEntities(level, pos, true);
            cbi.cancel();
        }
    }

    @Shadow private void stopJukeboxSong(BlockPos pos) {}
    @Shadow private void notifyNearbyEntities(Level level, BlockPos pos, boolean playing) {}
}
