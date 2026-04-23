package amaryllis.get_creative;

import amaryllis.get_creative.contraptions.CustomInteractionBehaviours;
import amaryllis.get_creative.contraptions.hinge_bearing.HandleBlock;
import amaryllis.get_creative.contraptions.hinge_bearing.HingeBearingBlock;
import amaryllis.get_creative.encapsulation.CapsuleItem;
import amaryllis.get_creative.encapsulation.EncapsulatorBlock;
import amaryllis.get_creative.encapsulation.GlueSpreaderBlock;
import amaryllis.get_creative.fluid_barrel.FluidBarrelBlock;
import amaryllis.get_creative.fluid_barrel.FluidBarrelBlockEntity;
import amaryllis.get_creative.generators.breeze_whirler.BreezeWhirlerBlock;
import amaryllis.get_creative.generators.clockwork_motor.ClockworkMotorBlock;
import amaryllis.get_creative.generators.clockwork_motor.wind_up_key.WindUpKeyBlock;
import amaryllis.get_creative.generators.haunted_cogwheel.HauntedCogwheelBlock;
import amaryllis.get_creative.industrial_fan.IndustrialFanBlock;
import amaryllis.get_creative.linked_controller.AllLinkedDevices;
import amaryllis.get_creative.linked_controller.lectern.LecternDeviceBlock;
import amaryllis.get_creative.recipes.CustomCreateRecipeTypes;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(GetCreative.MOD_ID)
@EventBusSubscriber
public class GetCreative {
    public static final String MOD_ID = "get_creative";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final Supplier<CreativeModeTab> CREATIVE_MODE_TAB = CREATIVE_MODE_TABS.register(MOD_ID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MOD_ID))
            .icon(() -> new ItemStack(BreezeWhirlerBlock.ITEM.get()))
            .build()
    );;

    public GetCreative(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.STARTUP, Config.SPEC);

        // Generating blocks
        HauntedCogwheelBlock.register();
        WindUpKeyBlock.register();
        ClockworkMotorBlock.register();
        BreezeWhirlerBlock.register();
        // Consuming blocks
        IndustrialFanBlock.register();
        // Misc
        FluidBarrelBlock.register();
        // Glue Spreader + Encapsulator + Capsules
        GlueSpreaderBlock.register();
        EncapsulatorBlock.register();
        CapsuleItem.register();
        // Hinge Bearing + Handles
        HingeBearingBlock.register();
        HandleBlock.TYPES.forEach(HandleBlock::register);

        AllLinkedDevices.register();
        LecternDeviceBlock.register();

        CustomCreateRecipeTypes.register(modEventBus);

        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        CustomPackets.register();

        // Register blocks that should automatically attach to Create contraptions
        BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) ->
            (state.getBlock() instanceof HandleBlock && state.getValue(HandleBlock.FACING) == direction.getOpposite())
                ? BlockMovementChecks.CheckResult.SUCCESS : BlockMovementChecks.CheckResult.PASS
        );
    }

    @SubscribeEvent
    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CustomInteractionBehaviours.registerDefaults();

            BlockStressValues.IMPACTS.register(IndustrialFanBlock.BLOCK.get(), Config.INDUSTRIAL_FAN_STRESS_IMPACT::get);
            BlockStressValues.CAPACITIES.register(HauntedCogwheelBlock.BLOCK.get(), Config.HAUNTED_COGWHEEL_STRESS_CAPACITY::get);
            BlockStressValues.CAPACITIES.register(WindUpKeyBlock.BLOCK.get(), Config.WIND_UP_KEY_STRESS_CAPACITY::get);
            BlockStressValues.CAPACITIES.register(ClockworkMotorBlock.BLOCK.get(), Config.CLOCKWORK_MOTOR_STRESS_CAPACITY::get);
            BlockStressValues.CAPACITIES.register(BreezeWhirlerBlock.BLOCK.get(), Config.BREEZE_WHIRLER_STRESS_CAPACITY::get);
        });
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        FluidBarrelBlockEntity.registerCapabilities(event);
    }

    public static ResourceLocation ID(String path) {
        return ResourceLocation.fromNamespaceAndPath(GetCreative.MOD_ID, path);
    }
    public static ResourceLocation ID(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static boolean shouldRegisterActor(Block block, MovementBehaviour behaviour) {
        final String blockID = BuiltInRegistries.BLOCK.getKey(block).toString();
        return !Config.ACTOR_BLACKLIST.get().contains(blockID) || Config.GRAVITY_ONLY_ACTORS.get().contains(blockID);
    }
    public static boolean shouldDisableActor(Block block, MovementContext context) {
        boolean isFalling = context.motion.normalize().dot( new Vec3(0, -1, 0)) > 0.7;
        if (isFalling) return false;

        final String blockID = BuiltInRegistries.BLOCK.getKey(block).toString();
        return Config.GRAVITY_ONLY_ACTORS.get().contains(blockID);
    }

    public static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ID(name)));
    }
}
