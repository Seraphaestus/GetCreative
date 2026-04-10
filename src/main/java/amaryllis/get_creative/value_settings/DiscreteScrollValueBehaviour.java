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

public class DiscreteScrollValueBehaviour<E extends Enum<E> & IDiscreteValueOptions> extends ScrollValueBehaviour {

    private E[] options;

    public DiscreteScrollValueBehaviour(Class<E> enum_, Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        options = enum_.getEnumConstants();
        between(0, options.length - 1);
        withFormatter(v -> LangNumberFormat.format(options[v].getValue()));
    }

    public E get() {
        return options[value];
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(label, max, 1,
                ImmutableList.of(Component.literal("Select")),
                new ValueSettingsFormatter(this::formatSettings));
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return CreateLang.builder()
                .text(" " + LangNumberFormat.format(options[settings.value()].getValue()) + " ")
                .component();
    }

    @Override
    public String getClipboardKey() {
        return options[0].getClass().getSimpleName();
    }

}