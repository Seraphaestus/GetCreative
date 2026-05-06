package amaryllis.get_creative.appliances.gramophone;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.content.kinetics.turntable.TurntableBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static net.minecraft.world.level.block.entity.JukeboxBlockEntity.SONG_ITEM_TAG_ID;
import static net.minecraft.world.level.block.entity.JukeboxBlockEntity.TICKS_SINCE_SONG_STARTED_TAG_ID;

public class GramophoneBlockEntity extends SmartBlockEntity implements Clearable, ContainerSingleItem.BlockContainerSingleItem {

    public static Supplier<BlockEntityType<GramophoneBlockEntity>> BLOCK_ENTITY;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "gramophone", () -> BlockEntityType.Builder.of(
                        GramophoneBlockEntity::new, GramophoneBlock.BLOCK.get()
                ).build(null));
    }

    private static final double LOG_2 = Math.log(2);
    public static final Map<Integer, Float> PITCH_FOR_SPEED = new HashMap<>();

    public ItemStack record;

    public final JukeboxSongPlayer songPlayer;
    protected int playbackSpeed = 0;

    protected int changeSpeedCooldown = 1;

    public GramophoneBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);

        record = ItemStack.EMPTY;
        songPlayer = new JukeboxSongPlayer(this::onSongChanged, getBlockPos());
    }

    @Override
    public void tick() {
        var turntable = getTurntable();
        if (turntable == null) {
            remove();
            return;
        }

        if (changeSpeedCooldown > 0) {
            changeSpeedCooldown -= 1;
            return;
        }

        int turntableSpeed = Math.abs(Math.round(turntable.getSpeed()));
        if (turntableSpeed != playbackSpeed) {
            playbackSpeed = turntableSpeed;
            changeSpeedCooldown = 10;
            sendData();

            if (level.isClientSide) return;

            Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(level.registryAccess(), record);
            if (playbackSpeed <= 0) {
                songPlayer.stop(level, getBlockState());
            } else if (!record.isEmpty() && song.isPresent()) {
                songPlayer.play(level, song.get());
            }
            return;
        }

        if (playbackSpeed > 0) songPlayer.tick(level, getBlockState());
    }

    public float getPitch(boolean recalculateSpeed) {
        if (recalculateSpeed) {
            var turntable = getTurntable();
            playbackSpeed = (turntable != null) ? Math.abs(Math.round(turntable.getSpeed())) : 0;
        }
        if (playbackSpeed == 0) return 0.5f;

        if (!PITCH_FOR_SPEED.containsKey(playbackSpeed)) {
            double pitch = 0.25 * (Math.log(0.14 * Math.pow(playbackSpeed, 1.35) + 1)) / LOG_2;
            if (Math.abs(Math.round(pitch) - pitch) < 0.01) pitch = Math.round(pitch); // If approx to an integer value, round exactly
            pitch = Math.clamp(pitch, 0.5, 2);
            PITCH_FOR_SPEED.put(playbackSpeed, (float)pitch);
        }
        return PITCH_FOR_SPEED.get(playbackSpeed);
    }

    public void onSongChanged() {
        level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        setChanged();
    }

    public int getComparatorOutput() {
        return JukeboxSong.fromStack(level.registryAccess(), record)
                .map(Holder::value)
                .map(JukeboxSong::comparatorOutput)
                .orElse(0);
    }

    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        if (!record.isEmpty()) tag.put(SONG_ITEM_TAG_ID, record.save(registries));

        if (songPlayer.getSong() != null) {
            tag.putLong(TICKS_SINCE_SONG_STARTED_TAG_ID, songPlayer.getTicksSinceSongStarted());
        }

        tag.putInt("PlaybackSpeed", playbackSpeed);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        record = tag.contains(SONG_ITEM_TAG_ID, Tag.TAG_COMPOUND)
            ? ItemStack.parse(registries, tag.getCompound(SONG_ITEM_TAG_ID)).orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;

        if (tag.contains(TICKS_SINCE_SONG_STARTED_TAG_ID, Tag.TAG_LONG)) {
            JukeboxSong.fromStack(registries, record).ifPresent(song ->
                    songPlayer.setSongWithoutPlaying(song, tag.getLong(TICKS_SINCE_SONG_STARTED_TAG_ID)));
        }

        playbackSpeed = tag.contains("PlaybackSpeed") ? tag.getInt("PlaybackSpeed") : 0;
    }

    protected void notifyRecordChanged(boolean hasRecord) {
        if (level != null && level.getBlockState(getBlockPos()) == getBlockState()) {
            level.setBlock(getBlockPos(), getBlockState().setValue(JukeboxBlock.HAS_RECORD, hasRecord), 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, getBlockPos(), GameEvent.Context.of(getBlockState()));
        }
    }

    public TurntableBlockEntity getTurntable() {
        return (level.getBlockEntity(getBlockPos().below()) instanceof TurntableBlockEntity turntableBE)
            ? turntableBE : null;
    }

    //#region Container
    @Override
    public @NotNull ItemStack getTheItem() {
        return record;
    }

    @Override
    public @NotNull ItemStack splitTheItem(int amount) {
        ItemStack stack = record;
        setTheItem(ItemStack.EMPTY);
        return stack;
    }

    public void setTheItem(@NotNull ItemStack stack) {
        record = stack;
        Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(level.registryAccess(), record);
        notifyRecordChanged(!record.isEmpty());
        if (!record.isEmpty() && song.isPresent() && playbackSpeed > 0) {
            songPlayer.play(level, song.get());
        } else {
            songPlayer.stop(level, getBlockState());
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public @NotNull BlockEntity getContainerBlockEntity() {
        return this;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var turnTable = getTurntable();
        return turnTable != null && stack.has(DataComponents.JUKEBOX_PLAYABLE) && getItem(slot).isEmpty();
    }

    @Override
    public boolean canTakeItem(Container target, int slot, @NotNull ItemStack stack) {
        return target.hasAnyMatching(ItemStack::isEmpty);
    }

    public void popOutTheItem() {
        if (level == null || level.isClientSide) return;

        ItemStack stack = getTheItem();
        if (stack.isEmpty()) return;

        removeTheItem();
        Vec3 pos = Vec3.atLowerCornerWithOffset(getBlockPos(), 0.5, -0.45, 0.5).offsetRandom(level.random, 0.15f);
        ItemEntity entity = new ItemEntity(this.level, pos.x(), pos.y(), pos.z(), stack.copy());
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
    //#endregion

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(0, 1, 0);
    }
}
