package amaryllis.get_creative.value_settings;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class SwappableScrollValues {

    protected SmartBlockEntity blockEntity;
    protected List<ScrollValueBehaviour> behaviours;
    protected int currentIndex;

    public SwappableScrollValues(SmartBlockEntity blockEntity, List<ScrollValueBehaviour> behaviours) {
        this.blockEntity = blockEntity;
        this.behaviours = behaviours;
    }

    public int clearIndex() {
        int nextIndex = (currentIndex + 1) % behaviours.size();
        currentIndex = -1;
        blockEntity.setChanged();
        blockEntity.sendData();
        return nextIndex;
    }
    public void setIndex(int nextIndex) {
        if (currentIndex == nextIndex) return;
        currentIndex = nextIndex;
        blockEntity.setChanged();
        blockEntity.sendData();
    }
    public int getIndex() {
        return currentIndex;
    }

    public <T extends BlockEntityBehaviour> T getBehaviour(BehaviourType<T> type, Supplier<T> defaultBehaviour) {
        return (type == ScrollValueBehaviour.TYPE && currentIndex >= 0) ? (T)behaviours.get(currentIndex) : defaultBehaviour.get();
    }
    public Collection<BlockEntityBehaviour> getAllBehaviours(Collection<BlockEntityBehaviour> defaultBehaviours) {
        var output = new ArrayList<>(defaultBehaviours);
        if (currentIndex >= 0) output.add(behaviours.get(currentIndex));
        return output;
    }

    public void write(CompoundTag tag) {
        tag.putInt("SelectedValuePanel", currentIndex);
    }
    public void read(CompoundTag tag) {
        currentIndex = tag.getInt("SelectedValuePanel");
    }
}
