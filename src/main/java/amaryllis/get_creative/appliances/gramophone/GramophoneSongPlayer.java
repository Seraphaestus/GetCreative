package amaryllis.get_creative.appliances.gramophone;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class GramophoneSongPlayer extends JukeboxSongPlayer {

    public GramophoneSongPlayer(OnSongChanged onSongChanged, BlockPos blockPos) {
        super(onSongChanged, blockPos);
    }

    public void tick(LevelAccessor level, @Nullable BlockState state, float pitch) {
        JukeboxSong song = this.getSong();
        if (song != null && hasFinished(song, getTicksSinceSongStarted(), pitch)) {
            stop(level, state);
            return;
        }
        super.tick(level, state);
    }

    public static boolean hasFinished(JukeboxSong song, long ticksSinceStarted, float pitch) {
        int playbackTime = Mth.ceil(song.lengthInTicks() / pitch);
        return ticksSinceStarted >= (long)(playbackTime + 20);
    }
}
