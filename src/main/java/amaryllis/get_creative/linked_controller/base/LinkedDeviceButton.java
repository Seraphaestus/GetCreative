package amaryllis.get_creative.linked_controller.base;

import amaryllis.get_creative.GetCreative;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public record LinkedDeviceButton(KeyMapping key, float posX, float posY, float posZ, PartialModel model) {

    public static final PartialModel DEFAULT = PartialModel.of(GetCreative.ID("create", "item/linked_controller/button"));
    public static final PartialModel ALTERNATIVE = PartialModel.of(GetCreative.ID("item/linked_controller/button_alternative"));

    public BakedModel getModel() {
        return model.get();
    }

    public String getTranslatedKey() {
        return switch (key.getKey().getValue()) {
            // Abbreviations e.g. "Up Arrow" -> "Up"
            case GLFW.GLFW_KEY_UP -> Component.translatable("key.get_creative.up").getString();
            case GLFW.GLFW_KEY_LEFT -> Component.translatable("key.get_creative.left").getString();
            case GLFW.GLFW_KEY_DOWN -> Component.translatable("key.get_creative.down").getString();
            case GLFW.GLFW_KEY_RIGHT -> Component.translatable("key.get_creative.right").getString();

            default -> key.getTranslatedKeyMessage().getString();
        };
    }

}
