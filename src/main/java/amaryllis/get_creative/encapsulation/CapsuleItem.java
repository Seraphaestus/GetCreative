package amaryllis.get_creative.encapsulation;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CapsuleItem extends Item {

    public static DeferredItem<CapsuleItem> ITEM;

    public static DataComponentType<CompoundTag> STORED_STRUCTURE_DATA;
    public static DataComponentType<Integer> STRUCTURE_ROTATION;
    public static DataComponentType<Component> CONTENTS_NAME;
    public static DataComponentType<Vec3i> STRUCTURE_SIZE = AllDataComponents.SCHEMATIC_BOUNDS;

    protected static Rotation[] rotations = new Rotation[]{Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};

    public static void register() {
        ITEM = GetCreative.ITEMS.registerItem(
                "structure_capsule", CapsuleItem::new,
                new Item.Properties().stacksTo(1));

        STORED_STRUCTURE_DATA = DataComponentType.<CompoundTag>builder()
                .persistent(CompoundTag.CODEC)
                .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                .build();
        STRUCTURE_ROTATION = DataComponentType.<Integer>builder()
                .persistent(ExtraCodecs.intRange(0, 3))
                .networkSynchronized(ByteBufCodecs.VAR_INT)
                .build();
        CONTENTS_NAME = DataComponentType.<Component>builder()
                .persistent(ComponentSerialization.FLAT_CODEC)
                .networkSynchronized(ComponentSerialization.STREAM_CODEC)
                .build();
        GetCreative.DATA_COMPONENTS.register("stored_structure", () -> STORED_STRUCTURE_DATA);
        GetCreative.DATA_COMPONENTS.register("structure_rotation", () -> STRUCTURE_ROTATION);
        GetCreative.DATA_COMPONENTS.register("contents_name", () -> CONTENTS_NAME);
    }

    public CapsuleItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(CompoundTag data, int rotation, Vec3i size, String name) {
        ItemStack instance = new ItemStack(CapsuleItem.ITEM.get());
        instance.set(CapsuleItem.STORED_STRUCTURE_DATA, data);
        instance.set(CapsuleItem.STRUCTURE_ROTATION, rotation);
        instance.set(CapsuleItem.STRUCTURE_SIZE, size);
        if (name != null) instance.set(CapsuleItem.CONTENTS_NAME, Component.literal(name).withStyle(ChatFormatting.GOLD));
        return instance;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!stack.has(STORED_STRUCTURE_DATA)) return InteractionResult.SUCCESS;

        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Rotation rotation = getStructureRotation(stack, context.getHorizontalDirection());
        BlockPos anchor = context.getClickedPos().relative(context.getClickedFace())
                .offset(getOffset(stack).rotate(rotation));

        StructureTemplate structure = getStructure(level, stack);
        if (!printStructure(level, structure, anchor, rotation)) return InteractionResult.SUCCESS;

        stack.consume(stack.getCount(), context.getPlayer());
        return InteractionResult.CONSUME;
    }

    protected static boolean printStructure(Level level, StructureTemplate structure, BlockPos anchor, Rotation rotation) {
        SchematicPrinter printer = new SchematicPrinter();
        ((ISchematicPrinter)printer).getCreative$loadStructure(structure, level, anchor, rotation, true);
        if (!printer.isLoaded() || printer.isErrored()) return false;

        while (printer.advanceCurrentPos()) {
            if (!printer.shouldPlaceCurrent(level)) continue;

            printer.handleCurrentTarget((pos, state, blockEntity) -> {
                if (state.isAir()) return;

                BlockState currentBlock = level.getBlockState(pos);
                if (!currentBlock.is(BlockTags.REPLACEABLE)) {
                    Block.dropResources(state, level, pos);
                    return;
                }

                CompoundTag beData = (blockEntity != null) ? blockEntity.saveWithoutMetadata(level.registryAccess()) : null;
                BlockHelper.placeSchematicBlock(level, state, pos, null, beData);
            },(pos, entity) -> {});
        }
        return true;
    }

    public static @Nullable StructureTemplate getStructure(Level anyLevel, ItemStack capsuleItem) {
        if (!capsuleItem.has(STORED_STRUCTURE_DATA)) return null;
        StructureTemplate structure = new StructureTemplate();

        CompoundTag data = fixData(capsuleItem.get(STORED_STRUCTURE_DATA));
        structure.load(anyLevel.holderLookup(Registries.BLOCK), data);
        return structure;
    }

    public static @NotNull Rotation getStructureRotation(ItemStack capsuleItem, Direction playerFacing) {
        Direction structureFacing = Direction.from2DDataValue(capsuleItem.getOrDefault(STRUCTURE_ROTATION, Direction.NORTH.get2DDataValue()));
        return rotations[(playerFacing.get2DDataValue() - structureFacing.get2DDataValue() + 8) % 4];
    }

    public static @NotNull BlockPos getOffset(ItemStack capsuleItem) {
        if (!capsuleItem.has(STORED_STRUCTURE_DATA)) return BlockPos.ZERO;
        CompoundTag data = capsuleItem.get(STORED_STRUCTURE_DATA);
        if (!data.contains("Offset")) return BlockPos.ZERO;

        Tag tag = data.get("Offset");
        if (tag instanceof ListTag iList) {
            return new BlockPos(iList.getInt(0), iList.getInt(1), iList.getInt(2));
        } else if (tag instanceof IntArrayTag iArr) {
            return new BlockPos(iArr.get(0).getAsInt(), iArr.get(1).getAsInt(), iArr.get(2).getAsInt());
        }
        return BlockPos.ZERO;
    }

    @Override
    @OnlyIn(value = Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        CompoundTag data = stack.has(STORED_STRUCTURE_DATA) ? stack.get(STORED_STRUCTURE_DATA) : null;
        Component contentsName = stack.has(CONTENTS_NAME) ? stack.get(CONTENTS_NAME) : null;

        if (data == null) {
            tooltip.add(Component.translatable("tooltip.get_creative.structure_capsule.invalid").withStyle(ChatFormatting.GRAY));
        } else {
            if (contentsName != null) {
                var nameComponent = contentsName.copy();
                tooltip.add(Component.translatable("tooltip.get_creative.structure_capsule.named", nameComponent).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.get_creative.structure_capsule").withStyle(ChatFormatting.GRAY));
            }
        }
        super.appendHoverText(stack, context, tooltip, flagIn);
    }


    protected static CompoundTag fixData(CompoundTag data) {
        // component serialization causes e.g. ListTags of Ints to be converted into a IntArrayTag
        // this causes an issue when a parser is expecting the array tag
        List<Tag> toCheck = new ArrayList<>();
        toCheck.add(data);

        while (!toCheck.isEmpty()) {
            Tag parentTag = toCheck.removeLast();
            if (parentTag.getId() == Tag.TAG_COMPOUND) {
                CompoundTag parent = (CompoundTag)parentTag;
                for (String key: parent.getAllKeys()) {
                    switch (parent.getTagType(key)) {
                        case Tag.TAG_COMPOUND, Tag.TAG_LIST:
                            if (!key.equals("nbt")) toCheck.add(parent.get(key));
                            break;
                        case Tag.TAG_INT_ARRAY:
                            IntArrayTag iArr = (IntArrayTag) parent.get(key);
                            ListTag iList = new ListTag();
                            iList.addAll(iArr);
                            parent.put(key, iList);
                            break;
                    }
                }
            } else if (parentTag.getId() == Tag.TAG_LIST) {
                ListTag parent = (ListTag)parentTag;
                for (int idx = 0; idx < parent.size(); idx++) {
                    Tag child = parent.get(idx);
                    switch (child.getId()) {
                        case Tag.TAG_COMPOUND, Tag.TAG_LIST:
                            toCheck.add(child);
                            break;
                        case Tag.TAG_INT_ARRAY:
                            IntArrayTag iArr = (IntArrayTag)child;
                            ListTag iList = new ListTag();
                            iList.addAll(iArr);
                            parent.set(idx, iList);
                            break;
                    }
                }
            }
        }
        return data;
    }
}
