package amaryllis.get_creative;

import amaryllis.get_creative.contraptions.hinge_bearing.HandleBlock;
import amaryllis.get_creative.contraptions.hinge_bearing.HingeBearingBlockEntity;
import amaryllis.get_creative.contraptions.hinge_bearing.HingeBearingRenderer;
import amaryllis.get_creative.contraptions.hinge_bearing.HingeBearingVisual;
import amaryllis.get_creative.fluid_barrel.FluidBarrelBlock;
import amaryllis.get_creative.fluid_barrel.FluidBarrelBlockEntity;
import amaryllis.get_creative.fluid_barrel.FluidBarrelModel;
import amaryllis.get_creative.fluid_barrel.FluidBarrelRenderer;
import amaryllis.get_creative.generators.breeze_whirler.BreezeWhirlerBlockEntity;
import amaryllis.get_creative.generators.breeze_whirler.BreezeWhirlerRenderer;
import amaryllis.get_creative.generators.clockwork_motor.ClockworkMotorBlockEntity;
import amaryllis.get_creative.generators.clockwork_motor.ClockworkMotorRenderer;
import amaryllis.get_creative.generators.clockwork_motor.ClockworkMotorVisual;
import amaryllis.get_creative.generators.clockwork_motor.wind_up_key.WindUpKeyBlockEntity;
import amaryllis.get_creative.generators.clockwork_motor.wind_up_key.WindUpKeyRenderer;
import amaryllis.get_creative.generators.clockwork_motor.wind_up_key.WindUpKeyVisual;
import amaryllis.get_creative.generators.haunted_cogwheel.HauntedCogwheelBlock;
import amaryllis.get_creative.generators.haunted_cogwheel.HauntedCogwheelBlockEntity;
import amaryllis.get_creative.generators.haunted_cogwheel.HauntedCogwheelRenderer;
import amaryllis.get_creative.industrial_fan.IndustrialFanBlockEntity;
import amaryllis.get_creative.industrial_fan.IndustrialFanRenderer;
import amaryllis.get_creative.industrial_fan.IndustrialFanVisual;
import amaryllis.get_creative.linked_controller.AllLinkedDevices;
import amaryllis.get_creative.linked_controller.base.LinkedDeviceClientHandler;
import amaryllis.get_creative.linked_controller.base.LinkedKeyContext;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlockEntity;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.jarjar.nio.util.Lazy;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod(value = GetCreative.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT)
public class GetCreativeClient {

    // Keybinds
    private static final List<Lazy<KeyMapping>> ALL_KEYBINDS = new ArrayList<>();
    public static final Lazy<KeyMapping> ARROW_UP = createKeybind("arrow_up", GLFW.GLFW_KEY_UP);
    public static final Lazy<KeyMapping> ARROW_LEFT = createKeybind("arrow_left", GLFW.GLFW_KEY_LEFT);
    public static final Lazy<KeyMapping> ARROW_DOWN = createKeybind("arrow_down", GLFW.GLFW_KEY_DOWN);
    public static final Lazy<KeyMapping> ARROW_RIGHT = createKeybind("arrow_right", GLFW.GLFW_KEY_RIGHT);
    public static final Lazy<KeyMapping> EXTRA_1 = createKeybind("extra_1", GLFW.GLFW_KEY_Q);
    public static final Lazy<KeyMapping> EXTRA_2 = createKeybind("extra_2", GLFW.GLFW_KEY_E);
    public static final Lazy<KeyMapping> EXTRA_3 = createKeybind("extra_3", GLFW.GLFW_KEY_Z);
    public static final Lazy<KeyMapping> EXTRA_4 = createKeybind("extra_4", GLFW.GLFW_KEY_X);

    public GetCreativeClient(ModContainer container) {
        // Register Config Screen
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        BlockEntityRenderers.register( HauntedCogwheelBlockEntity.BLOCK_ENTITY.get(), HauntedCogwheelRenderer::new);
        simpleVisualizer( HauntedCogwheelBlockEntity.BLOCK_ENTITY.get(), simpleVisual(HauntedCogwheelBlock.MODEL));

        BlockEntityRenderers.register( ClockworkMotorBlockEntity.BLOCK_ENTITY.get(), ClockworkMotorRenderer::new);
        simpleVisualizer( ClockworkMotorBlockEntity.BLOCK_ENTITY.get(), ClockworkMotorVisual::new);

        BlockEntityRenderers.register( WindUpKeyBlockEntity.BLOCK_ENTITY.get(), WindUpKeyRenderer::new);
        simpleVisualizer( WindUpKeyBlockEntity.BLOCK_ENTITY.get(), WindUpKeyVisual::new);

        BlockEntityRenderers.register( BreezeWhirlerBlockEntity.BLOCK_ENTITY.get(), BreezeWhirlerRenderer::new);
        // Note: the Visual had major issues so we just use the Renderer
        //simpleVisualizer( BreezeWhirlerBlockEntity.BLOCK_ENTITY.get(), BreezeWhirlerVisual::new);

        BlockEntityRenderers.register( HingeBearingBlockEntity.BLOCK_ENTITY.get(), HingeBearingRenderer::new);
        simpleVisualizer( HingeBearingBlockEntity.BLOCK_ENTITY.get(), HingeBearingVisual::new);

        BlockEntityRenderers.register( LecternDeviceBlockEntity.BLOCK_ENTITY.get(), LecternDeviceRenderer::new);
        AllLinkedDevices.registerClient();

        BlockEntityRenderers.register( IndustrialFanBlockEntity.BLOCK_ENTITY.get(), IndustrialFanRenderer::new);
        simpleVisualizer( IndustrialFanBlockEntity.BLOCK_ENTITY.get(), IndustrialFanVisual::new);

        BlockEntityRenderers.register( FluidBarrelBlockEntity.BLOCK_ENTITY.get(), FluidBarrelRenderer::new);
        CreateRegistrate.blockModel(() -> FluidBarrelModel::new).accept(FluidBarrelBlock.BLOCK.get());
        ItemBlockRenderTypes.setRenderLayer(FluidBarrelBlock.BLOCK.get(), RenderType.CUTOUT_MIPPED);
    }

    private static <T extends BlockEntity> void simpleVisualizer(BlockEntityType<T> blockEntity, SimpleBlockEntityVisualizer.Factory<T> visualFactory) {
        SimpleBlockEntityVisualizer.builder(blockEntity)
                .factory(visualFactory)
                .skipVanillaRender(be -> true)
                .apply();
    }
    private static <T extends KineticBlockEntity> SimpleBlockEntityVisualizer.Factory<T> simpleVisual(PartialModel partial) {
        return (context, blockEntity, partialTick) -> {
            Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.FACING);
            return new OrientedRotatingVisual<>(context, blockEntity, partialTick, Direction.UP, facing, Models.partial(partial));
        };
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        AllLinkedDevices.onTick();
    }

    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        AllLinkedDevices.onClickInput();
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, GetCreative.ID("linked_device"), LinkedDeviceClientHandler.OVERLAY);
    }

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        ALL_KEYBINDS.forEach(key -> event.register(key.get()));
    }

    private static Lazy<KeyMapping> createKeybind(String ID, int keyCode) {
        final var key = Lazy.of(() -> new KeyMapping(
                "key." + GetCreative.MOD_ID + "." + ID,
                LinkedKeyContext.INSTANCE,
                InputConstants.Type.KEYSYM, keyCode,
                "key.categories.linked_controller"
        ));
        ALL_KEYBINDS.add(key);
        return key;
    }

    @SubscribeEvent
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() != GetCreative.CREATIVE_MODE_TAB.get()) return;

        GetCreative.ITEMS.getEntries().forEach(entry -> {
            event.accept(entry.get().asItem());
        });
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        Block block = (item instanceof BlockItem blockItem) ? blockItem.getBlock() : null;

        String namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
        if (!namespace.equals(GetCreative.MOD_ID)) return;

        // Overrides -> multiple items can share the same tooltip
        if (block instanceof HandleBlock) {
            item = BuiltInRegistries.ITEM.get(GetCreative.ID("oak_handle"));
        }

        // Data-driven Create tooltips
        TooltipModifier tooltip = new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE);
        if (event.getEntity() != null) {
            tooltip = tooltip.andThen(TooltipModifier.mapNull(KineticStats.create(item)));
            tooltip.modify(event);
        }
    }
}
