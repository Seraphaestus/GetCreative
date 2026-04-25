package amaryllis.get_creative;

import java.util.List;

import amaryllis.get_creative.encapsulation.CapsuleItem;
import amaryllis.get_creative.encapsulation.EncapsulatorBlock;
import amaryllis.get_creative.generators.breeze_whirler.BreezeWhirlerBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;

import static com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory.RECIPES;

public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ACTOR_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GRAVITY_ONLY_ACTORS;

    public static final ModConfigSpec.BooleanValue SAW_CAN_MUTLIBREAK;
    public static final ModConfigSpec.BooleanValue SAW_CAN_BREAK_ALL_BLOCKS;

    public static final ModConfigSpec.DoubleValue DRILL_SPEED_MODIFIER;
    public static final ModConfigSpec.DoubleValue SAW_SPEED_MODIFIER;

    public static final ModConfigSpec.BooleanValue DEPLOYERS_DISASSEMBLE_TRAINS;
    public static final ModConfigSpec.BooleanValue CAN_GIVE_WRENCH_TO_DEPLOYER;

    public static final ModConfigSpec.ConfigValue<String> JEI_FAN_WASHING_CATALYST;
    public static final ModConfigSpec.ConfigValue<String> JEI_FAN_BLASTING_CATALYST;
    public static final ModConfigSpec.ConfigValue<String> JEI_FAN_SMOKING_CATALYST;
    public static final ModConfigSpec.ConfigValue<String> JEI_FAN_HAUNTING_CATALYST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MYSTERIOUS_CONVERSIONS;

    public static final ModConfigSpec.BooleanValue PRECISION_ASSEMBLY_BULK_PROCESSING;

    public static final ModConfigSpec.DoubleValue INDUSTRIAL_FAN_STRESS_IMPACT;

    public static final ModConfigSpec.DoubleValue HAUNTED_COGWHEEL_MIN_ROTATION;
    public static final ModConfigSpec.DoubleValue HAUNTED_COGWHEEL_MAX_ROTATION;
    public static final ModConfigSpec.DoubleValue HAUNTED_COGWHEEL_DISTRIBUTION_FACTOR;
    public static final ModConfigSpec.DoubleValue HAUNTED_COGWHEEL_VOLATILITY;
    public static final ModConfigSpec.DoubleValue HAUNTED_COGWHEEL_STRESS_CAPACITY;
    public static final ModConfigSpec.BooleanValue HAUNTED_COGWHEEL_REQUIRES_UNIQUITY;

    public static final ModConfigSpec.DoubleValue CLOCKWORK_MOTOR_STRESS_CAPACITY;
    public static final ModConfigSpec.DoubleValue CLOCKWORK_MOTOR_CHARGE_CAPACITY;
    public static final ModConfigSpec.IntValue CLOCKWORK_MOTOR_EVEN_POINT;
    public static final ModConfigSpec.DoubleValue CLOCKWORK_MOTOR_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue WIND_UP_KEY_STRESS_CAPACITY;

    public static final ModConfigSpec.DoubleValue BREEZE_WHIRLER_STRESS_CAPACITY;
    public static final ModConfigSpec.DoubleValue BREEZE_WHIRLER_ROTATION_SPEED;

    public static final ModConfigSpec.DoubleValue LECTERN_CONTROLLER_REACH;

    public static final ModConfigSpec.IntValue FLUID_BARREL_CAPACITY;
    public static final ModConfigSpec.IntValue FLUID_BARREL_MAX_HEIGHT;
    public static final ModConfigSpec.IntValue FLUID_BARREL_MAX_TEMPERATURE;
    public static final ModConfigSpec.IntValue FLUID_BARREL_MIN_BURN_TEMPERATURE;
    public static final ModConfigSpec.IntValue FLUID_BARREL_MAX_BURN_TEMPERATURE;
    public static final ModConfigSpec.BooleanValue FLUID_BARREL_CAN_STORE_GAS;

    static {
        BUILDER
            .comment(" The following Create config options are also recommended:")
            .comment("   moveItemsToStorage: Make mined blocks drop items in-world for manual collection")
            .comment("   harvesterReplants: Prevent Harvesters from automatically replanting");

        ACTOR_BLACKLIST = BUILDER
                .comment("")
                .comment(" A list of blocks to remove from being registered as contraption actors.")
                .defineListAllowEmpty("actor_blacklist", List.of(), () -> "", (id) -> true);
        GRAVITY_ONLY_ACTORS = BUILDER
                .comment(" A list of blocks whose contraption actors should only be allowed to work while they are facing and moving downwards")
                .comment(" Note that the actor will still visually spin/animate regardless of context, except for the Mechanical Drill which has special handling")
                .defineListAllowEmpty("gravity_only_actors", List.of(), () -> "", (id) -> true);

        BUILDER.push("Mechanical Saw");
        SAW_CAN_MUTLIBREAK = BUILDER
                .comment(" If mining a tree will a Mechanical Saw will fell the whole tree")
                .comment("   Applies to both blocks and contraption actors")
                .define("saw_can_multibreak", true);
        SAW_CAN_BREAK_ALL_BLOCKS = BUILDER
                .comment("")
                .comment(" If true, the Mechanical Saw can break all block types, like the Mechanical Drill")
                .comment("   Saplings remain unmineable by Mechanical Saws, as in base Create")
                .define("saw_can_break_all_blocks", true);
        BUILDER.pop();

        BUILDER.push("Drill/Saw Specialties");
        DRILL_SPEED_MODIFIER = BUILDER
                .comment(" If a Mechanical Drill is breaking a block that is not in #create:mineable/mechanical_drill")
                .comment("   Default values: #mineable/pickaxe + #mineable/shovel")
                .defineInRange("drill_speed_modifier", 0.5, 0.0, 64.0);
        SAW_SPEED_MODIFIER = BUILDER
                .comment("")
                .comment(" If a Mechanical Saw is breaking a block that is not in #create:mineable/mechanical_saw")
                .comment("   Default values: #mineable/axe + #mineable/hoe")
                .defineInRange("saw_speed_modifier", 0.5, 0.0, 64.0);
        BUILDER.pop();

        BUILDER.push("Automatic Train Assembly");
        DEPLOYERS_DISASSEMBLE_TRAINS = BUILDER
                .comment(" Ports the feature of Deployers being able to assemble/disassemble trains from Create: Steam 'n' Rails")
                .comment("   Has no effect if Create: Steam 'n' Rails is loaded (with mod id 'railways')")
                .define("deployers_disassemble_trains", true);
        CAN_GIVE_WRENCH_TO_DEPLOYER = BUILDER
                .comment("")
                .comment(" If true, when the player Shift-R-Clicks a Wrench on a Deployer, it will give the Deployer the Wrench, instead of breaking the Deployer.")
                .define("can_give_wrench_to_deployer", true);
        BUILDER.pop();

        BUILDER.push("Recipe Viewers");
        JEI_FAN_WASHING_CATALYST = BUILDER
                .comment(" Block ID for overriding the Fan Washing catalyst in Recipe Viewers. Defaults to 'minecraft:water'")
                .define("jei_fan_washing_catalyst", "");
        JEI_FAN_BLASTING_CATALYST = BUILDER
                .comment("")
                .comment(" Block ID for overriding the Fan Blasting catalyst in Recipe Viewers. Defaults to 'minecraft:lava'")
                .define("jei_fan_blasting_catalyst", "");
        JEI_FAN_SMOKING_CATALYST = BUILDER
                .comment("")
                .comment(" Block ID for overriding the Fan Smoking catalyst in Recipe Viewers. Defaults to 'minecraft:fire'")
                .define("jei_fan_smoking_catalyst", "");
        JEI_FAN_HAUNTING_CATALYST = BUILDER
                .comment("")
                .comment(" Block ID for overriding the Fan Haunting catalyst in Recipe Viewers. Defaults to 'minecraft:soul_fire'")
                .define("jei_fan_haunting_catalyst", "");
        MYSTERIOUS_CONVERSIONS = BUILDER
                .comment("")
                .comment(" A list of mysterious conversions to add to the Recipe Viewer category, e.g. 'create:empty_blaze_burner -> create:blaze_burner'")
                .defineListAllowEmpty("mysterious_conversions", List.of(
                    "get_creative:empty_breeze_whirler -> get_creative:breeze_whirler",
                    "get_creative:encapsulator -> get_creative:structure_capsule"
                ), () -> "", (id) -> true);
        BUILDER.pop();

        BUILDER.push("Precision Assembly");
        PRECISION_ASSEMBLY_BULK_PROCESSING = BUILDER
                .define("precision_assembly_bulk_processing", false);
        BUILDER.pop();

        //region Appliances
        BUILDER.push("Appliances");

        INDUSTRIAL_FAN_STRESS_IMPACT = BUILDER
                .defineInRange("industrial_fan_stress_impact", 8d, 0d, 16384d);

        BUILDER.pop();
        //endregion

        //region Generators
        BUILDER.push("Generators");

        BUILDER.push("Haunted Cogwheel");
        HAUNTED_COGWHEEL_MIN_ROTATION = BUILDER
                .defineInRange("haunted_cogwheel_min_rotation_speed", 4d, 0d, 256d);
        HAUNTED_COGWHEEL_MAX_ROTATION = BUILDER
                .comment("")
                .defineInRange("haunted_cogwheel_max_rotation_speed", 48d, 0d, 256d);
        HAUNTED_COGWHEEL_DISTRIBUTION_FACTOR = BUILDER
                .comment("")
                .comment(" When a new speed is chosen, a value in [0, 1] will be raised to this power before interpolating [min_rotation, max_rotation]")
                .defineInRange("haunted_cogwheel_distribution_factor", 2d, 0d, 256d);
        HAUNTED_COGWHEEL_VOLATILITY = BUILDER
                .comment("")
                .comment(" Chance each tick that the Haunted Cogwheel changes to a new speed")
                .defineInRange("haunted_cogwheel_volatility", 0.005d, 0d, 1d);
        HAUNTED_COGWHEEL_STRESS_CAPACITY = BUILDER
                .comment("")
                .defineInRange("haunted_cogwheel_stress_capacity", 8d, 0d, 16384d);
        HAUNTED_COGWHEEL_REQUIRES_UNIQUITY = BUILDER
                .comment("")
                .comment(" If true, the Haunted Cogwheel will break if other generators are added to the network")
                .define("haunted_cogwheel_requires_uniquity", true);
        BUILDER.pop();

        BUILDER.push("Clockwork Motor and Wind-Up Key");
        CLOCKWORK_MOTOR_STRESS_CAPACITY = BUILDER
                .defineInRange("clockwork_motor_stress_capacity", 32d, 0d, 16384d);
        WIND_UP_KEY_STRESS_CAPACITY = BUILDER
                .defineInRange("wind_up_key_stress_capacity", 8d, 0d, 16384d);
        CLOCKWORK_MOTOR_CHARGE_CAPACITY = BUILDER
                .comment("")
                .comment(" A relative multiplier on the maximum charge the motor can be wound up to")
                .defineInRange("clockwork_motor_charge_capacity", 1, 0d, 1024d);
        CLOCKWORK_MOTOR_EVEN_POINT = BUILDER
                .comment("")
                .comment(" By default, the ratio of wind-down output time to wind-up input time follows the following pattern:")
                .comment(" 16 RPM -> 3:1, 32 RPM -> 2:1, 64 RPM -> 1:1, 128 RPM -> 1:2, 256 RPM -> 1:3")
                .comment("")
                .comment(" This determines the power of 2 at which the motor has a 1:1 ratio. The default 6 -> 2^6 = 64 RPM")
                .defineInRange("clockwork_motor_even_point", 6, 1, 8);
        CLOCKWORK_MOTOR_EFFICIENCY = BUILDER
                .comment("")
                .comment(" The baseline wind-down duration is then multiplied by this value")
                .defineInRange("clockwork_motor_efficiency", 1.5d, 0d, 16384d);
        BUILDER.pop();

        BUILDER.push("Breeze Whirler");
        BREEZE_WHIRLER_ROTATION_SPEED = BUILDER
                .defineInRange("breeze_whirler_rotation_speed", 16d, 0d, 256d);
        BREEZE_WHIRLER_STRESS_CAPACITY = BUILDER
                .comment("")
                .defineInRange("breeze_whirler_stress_capacity", 32d, 0d, 16384d);
        BUILDER.pop();

        BUILDER.pop();
        //endregion

        BUILDER.push("Linked Devices");
        LECTERN_CONTROLLER_REACH = BUILDER
                .comment(" The max range at which a player can interact with a Lectern with Linked Controller, as a multiplier to their base block interaction range")
                .defineInRange("lectern_controller_reach", 0.4d, 0d, 1024d);
        BUILDER.pop();

        BUILDER.push("Fluid Barrel");
        FLUID_BARREL_CAPACITY = BUILDER
                .comment(" How many buckets of fluid a Fluid Barrel can hold per block")
                .defineInRange("fluid_barrel_capacity", 4, 1, 1024);
        FLUID_BARREL_MAX_HEIGHT = BUILDER
                .comment("")
                .comment(" The maximum height a Fluid Barrel can reach")
                .defineInRange("fluid_barrel_max_height", 32, 1, 1024);
        FLUID_BARREL_MAX_TEMPERATURE = BUILDER
                .comment("")
                .comment(" Max temperature in Kelvin that the Fluid Barrel may store")
                .comment(" For reference, the default fluid temperature is 300 (~27C), while lava is 1300 (~1000C). 340 is ~68C")
                .defineInRange("fluid_barrel_max_temperature", 340, Integer.MIN_VALUE, Integer.MAX_VALUE);
        FLUID_BARREL_MIN_BURN_TEMPERATURE = BUILDER
                .comment("")
                .comment(" Temperature in Kelvin that a player will receive the minimum damage from being burned by a fluid")
                .defineInRange("fluid_barrel_min_burn_temperature", 340, Integer.MIN_VALUE, Integer.MAX_VALUE);
        FLUID_BARREL_MAX_BURN_TEMPERATURE = BUILDER
                .comment("")
                .comment(" Temperature in Kelvin that a player will receive the full damage from being burned by a fluid")
                .defineInRange("fluid_barrel_max_burn_temperature", 1000, Integer.MIN_VALUE, Integer.MAX_VALUE);
        FLUID_BARREL_CAN_STORE_GAS = BUILDER
                .comment("")
                .comment(" Whether the Fluid Barrel may contain lighter-than-air fluids")
                .define("fluid_barrel_can_store_gas", false);

        SPEC = BUILDER.build();
    }


    public static BlockState getBlockStateOrDefault(ModConfigSpec.ConfigValue<String> configValue, Block defaultValue) {
        return getBlockOrDefault(configValue, defaultValue).defaultBlockState();
    }
    public static Block getBlockOrDefault(ModConfigSpec.ConfigValue<String> configValue, Block defaultValue) {
        return (configValue.get().isEmpty()) ? defaultValue
               : BuiltInRegistries.BLOCK.get(ResourceLocation.parse(configValue.get()));
    }
}
