package amaryllis.get_creative.value_settings;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class BoundedScrollValueBehaviour extends ScrollValueBehaviour {

    // In order to format milestone spacing correctly, a bound e.g. [1, 256] should be given as [0, 256]
    // With the default allowZero = false, the 0 value is converted to an extraneous 1 value, as in base Create

    private final String simpleName;
    private final int minValue;
    private final int maxValue;
    private boolean allowZero = false;

    public BoundedScrollValueBehaviour(String simpleName, int minValue, int defaultValue, int maxValue, Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        this.simpleName = simpleName;
        this.minValue = minValue;
        value = defaultValue - minValue;
        this.maxValue = maxValue;
        between(0, maxValue - minValue);
        withFormatter(v -> LangNumberFormat.format(v + minValue));
    }
    public BoundedScrollValueBehaviour(String simpleName, int defaultValue, int maxValue, Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        this(simpleName, 0, defaultValue, maxValue, label, be, slot);
    }
    public BoundedScrollValueBehaviour allowZero() {
        allowZero = true;
        return this;
    }

    public int get() {
        return value + minValue;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        final int range = maxValue - minValue;
        int milestoneInterval = getMilestoneInterval(range);
        return new ValueSettingsBoard(label, range, milestoneInterval,
                ImmutableList.of(Component.literal("Select")),
                new ValueSettingsFormatter(this::formatSettings));
    }

    public static int getMilestoneInterval(int range) {
        int milestoneInterval = 1;
        if (range % 8 == 0) milestoneInterval = range / 8;
        else if (range % 12 == 0) milestoneInterval = range / 12;
        else milestoneInterval = range / 4;
        return Math.max(milestoneInterval, 1);
    }

    public int fixValue(int value) {
        return (value == 0 && !allowZero) ? 1 : value;
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
        if (!valueSetting.equals(getValueSettings())) playFeedbackSound(this);
        setValue(fixValue(valueSetting.value()));
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return CreateLang.number(minValue + fixValue(settings.value())).component();
    }

    @Override
    public String getClipboardKey() {
        return simpleName;
    }

}