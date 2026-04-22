package amaryllis.get_creative.value_settings;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.GetCreativeClient;
import amaryllis.get_creative.contraptions.hinge_bearing.HingeBearingBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

import static com.simibubi.create.CreateClient.VALUE_SETTINGS_HANDLER;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = GetCreative.MOD_ID, value = Dist.CLIENT)
public class ToggleSwitchRenderer {

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ToggleSwitchRenderer.tick();
    }
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (isSwitchHovered()) {
            List<MutableComponent> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("get_creative.hinge_bearing.toggle_config"));
            tooltip.add(Component.translatable("get_creative.gui.value_settings.toggle_switch"));
            VALUE_SETTINGS_HANDLER.showHoverTip(tooltip);
        }
    }

    public static boolean isSwitchHovered() {
        HitResult target = Minecraft.getInstance().hitResult;
        if (target == null || !(target instanceof BlockHitResult result)) return false;

        ClientLevel level = Minecraft.getInstance().level;
        BlockPos pos = result.getBlockPos();

        if (!(level.getBlockEntity(pos) instanceof HingeBearingBlockEntity hingeBearing)) return false;

        return ToggleSwitchHandler.isHovered(hingeBearing, target.getLocation(), result.getDirection(), 3);
    }
}