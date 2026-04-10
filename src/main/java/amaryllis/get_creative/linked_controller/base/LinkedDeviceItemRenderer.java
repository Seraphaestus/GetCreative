package amaryllis.get_creative.linked_controller.base;

import amaryllis.get_creative.GetCreative;
import amaryllis.get_creative.linked_controller.LinkedDevicesClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

import static com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler.Mode.BIND;
import static com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler.Mode.IDLE;

@OnlyIn(Dist.CLIENT)
public class LinkedDeviceItemRenderer extends CustomRenderedItemModelRenderer {

    public enum RenderType { NORMAL, LECTERN }

    public static final float PIXEL = 1 / 16f,
                              BUTTON_HEIGHT = PIXEL * -.75f;
    protected static final float LEFT_EDGE = 12, BOTTOM_EDGE = 7;

    // Linked Controller reference
    //            < -- X -- >
    //      0 1 2 3 4 5 6 7 8 9 A B C
    //    0 . . . . . . . . . . . . .
    // ^  1 . . . . X X . . . . . . .
    // |  2 . . . . @ X . . . X X . .
    // Z  3 . . X X . . X X . @ X . .
    // |  4 . . @ X . . @ X . # # . .
    // v  5 . . . . X X . . . @ # . .
    //    6 . . . . @ X . . . . . . .
    //    7 . . . . . . . . . . . . .

    // Dependency Injected
    protected LinkedDeviceItem linkedDeviceItem;

    protected LerpedFloat equipProgress;
    protected List<LerpedFloat> buttons;

    public LinkedDeviceItemRenderer(LinkedDeviceItem item_instance) {
        linkedDeviceItem = item_instance;

        equipProgress = LerpedFloat.linear().startWithValue(0);

        buttons = new ArrayList<>(item_instance.buttonCount);
        for (int i = 0; i < item_instance.buttonCount; i++) buttons.add(LerpedFloat.linear().startWithValue(0));
    }

    protected void tick() {
        if (Minecraft.getInstance().isPaused()) return;

        var clientHandler = LinkedDevicesClient.getClientHandler(linkedDeviceItem);

        boolean active = clientHandler.MODE != IDLE || linkedDeviceItem.skipActivation();
        equipProgress.chase(active ? 1 : 0, .2f, LerpedFloat.Chaser.EXP);
        equipProgress.tickChaser();

        if (!active) return;

        for (int i = 0; i < buttons.size(); i++) {
            LerpedFloat lerpedFloat = buttons.get(i);
            lerpedFloat.chase(clientHandler.currentlyPressed.contains(i) ? 1 : 0, .4f, LerpedFloat.Chaser.EXP);
            lerpedFloat.tickChaser();
        }
    }

    protected void resetButtons() {
        for (LerpedFloat button : buttons) { button.startWithValue(0); }
    }

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
                          ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light,
                          int overlay) {
        renderNormal(stack, model, renderer, transformType, ms, light);
    }

    protected void renderNormal(ItemStack stack, CustomRenderedItemModel model,
                                       PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms,
                                       int light) {
        render(stack, model, renderer, transformType, ms, light, RenderType.NORMAL, false, false);
    }

    public void renderInLectern(ItemStack stack, CustomRenderedItemModel model,
                                       PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms,
                                       int light, boolean active, boolean renderDepression) {
        render(stack, model, renderer, transformType, ms, light, RenderType.LECTERN, active, renderDepression);
    }

    protected void render(ItemStack stack, CustomRenderedItemModel model,
                                 PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms,
                                 int light, RenderType renderType, boolean active, boolean renderDepression) {
        float pt = AnimationTickHolder.getPartialTicks();
        var msr = TransformStack.of(ms);

        var clientHandler = LinkedDevicesClient.getClientHandler(linkedDeviceItem);

        ms.pushPose();

        if (renderType == RenderType.NORMAL) {
            Minecraft mc = Minecraft.getInstance();
            boolean rightHanded = mc.options.mainHand().get() == HumanoidArm.RIGHT;
            ItemDisplayContext mainHand =
                    rightHanded ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            ItemDisplayContext offHand =
                    rightHanded ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

            active = false;
            boolean noControllerInMain = !mc.player.getMainHandItem().is(linkedDeviceItem);

            if (transformType == mainHand || (transformType == offHand && noControllerInMain)) {
                float equip = linkedDeviceItem.skipActivation() ? 1f : equipProgress.getValue(pt);
                int handModifier = (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) ? -1 : 1;
                linkedDeviceItem.transformActiveHeldDevice(msr, equip, handModifier);
                active = true;
            }

            if (transformType == ItemDisplayContext.GUI) {
                if (stack == mc.player.getMainHandItem()) active = true;
                if (stack == mc.player.getOffhandItem() && noControllerInMain) active = true;
            }

            active &= (clientHandler.MODE != IDLE || linkedDeviceItem.skipActivation());

            renderDepression = true;
        }

        final boolean usePoweredModel = linkedDeviceItem.skipActivation() ? clientHandler.defaultPressCooldown > -2 : active;
        renderer.render(usePoweredModel ? linkedDeviceItem.getPoweredModel() : model.getOriginalModel(), light);

        if (!active) {
            ms.popPose();
            return;
        }

        if (renderType == RenderType.NORMAL) {
            if (clientHandler.MODE == BIND) {
                int i = (int) Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4f) + 1) / 2, 5, 15);
                light = i << 20;
            }
        }

        final var buttons = LinkedDevicesClient.getClientHandler(linkedDeviceItem).BUTTONS;
        for (int buttonIndex = 0; buttonIndex < buttons.size(); buttonIndex++) {
            var button = buttons.get(buttonIndex);
            if (button.getModel() == null) {
                GetCreative.LOGGER.error("Missing model for button {} of {}", buttonIndex, linkedDeviceItem);
                continue;
            }

            ms.pushPose();
            msr.translate(button.posZ() * PIXEL,
                          button.posY() * PIXEL,
                          (LEFT_EDGE - button.posX()) * PIXEL);
            renderButton(renderer, ms, light, pt, button.getModel(), BUTTON_HEIGHT, buttonIndex, renderDepression);
            ms.popPose();
        }

        ms.popPose();
    }

    protected void renderButton(PartialItemModelRenderer renderer, PoseStack ms, int light, float pt, BakedModel button,
                                       float buttonHeight, int index, boolean renderDepression) {
        ms.pushPose();
        if (renderDepression) {
            float depression = buttonHeight * buttons.get(index).getValue(pt);
            ms.translate(0, depression, 0);
        }
        renderer.renderSolid(button, light);
        ms.popPose();
    }

    public static float fromBottom(float zPos) { return BOTTOM_EDGE + zPos; }
    public static float fromRight(float xPos) { return LEFT_EDGE + xPos; }

}
