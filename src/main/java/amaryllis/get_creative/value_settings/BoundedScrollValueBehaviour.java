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

    private final String simpleName;
    private final int minValue;
    private final int maxValue;

    public BoundedScrollValueBehaviour(String simpleName, int minValue, int defaultValue, int maxValue, Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        this.simpleName = simpleName;
        this.minValue = minValue;
        value = defaultValue - minValue;
        this.maxValue = maxValue;
        between(0, maxValue - minValue);
        withFormatter(v -> LangNumberFormat.format(v + minValue));
    }

    public int get() {
        return value + minValue;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        final int range = maxValue - minValue;
        return new ValueSettingsBoard(label, range, Math.max(range / 8, 1),
                ImmutableList.of(Component.literal("Select")),
                new ValueSettingsFormatter(this::formatSettings));
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return CreateLang.number(settings.value() + minValue).component();
    }

    @Override
    public String getClipboardKey() {
        return simpleName;
    }

}