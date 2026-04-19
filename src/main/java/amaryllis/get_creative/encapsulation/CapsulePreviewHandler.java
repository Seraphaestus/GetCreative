package amaryllis.get_creative.encapsulation;

import amaryllis.get_creative.GetCreative;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = GetCreative.MOD_ID, value = Dist.CLIENT)
public class CapsulePreviewHandler {

    public static CapsulePreviewHandler INSTANCE = new CapsulePreviewHandler();

    // Active if non-null, inactive if null
    protected ItemStack capsuleStack;

    protected CompoundTag structureData;
    protected BlockPos prevAnchor;
    protected Rotation prevRotation;

    private AABB bounds;
    private AABBOutline outline;
    protected SchematicRenderer renderer;
    private SchematicTransformation transformation = new SchematicTransformation();

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        INSTANCE.tick();
    }
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            if (capsuleStack != null) capsuleStack = null;
            return;
        }

        if (capsuleStack != null && transformation != null) {
            if (!updateTransform(mc.player, capsuleStack, false)) {
                capsuleStack = null;
                return;
            }
            transformation.tick();
        }

        ItemStack prevCapsuleStack = capsuleStack;
        capsuleStack = findHeldStructureCapsule(mc.player);
        if (capsuleStack == null) return;

        boolean hasChanged = prevCapsuleStack == null || !prevCapsuleStack.equals(capsuleStack) ||
                             !capsuleStack.get(CapsuleItem.STORED_STRUCTURE_DATA).equals(structureData);
        if (hasChanged) {
            if (!reloadStructure(mc.player)) capsuleStack = null;
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        PoseStack ms = event.getPoseStack();
        ms.pushPose();
        var buffer = DefaultSuperRenderTypeBuffer.getInstance();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float partialTicks = event.getPartialTick().getRealtimeDeltaTicks();
        INSTANCE.render(ms, buffer, camera, partialTicks);
        buffer.draw();
        RenderSystem.enableCull();
        ms.popPose();
    }
    public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float partialTicks) {
        if (capsuleStack == null) return;

        ms.pushPose();
        transformation.applyTransformations(ms, camera);
        if (renderer != null) renderer.render(ms, buffer);

        if (outline != null) {
            outline.render(ms, buffer, Vec3.ZERO, partialTicks);
            outline.getParams().clearTextures();
        }

        ms.popPose();
    }

    public void updateRenderer() {
        if (renderer != null) renderer.update();
    }

    protected boolean reloadStructure(LocalPlayer player) {
        Level level = Minecraft.getInstance().level;

        if (!updateTransform(player, capsuleStack, true)) return false;

        structureData = capsuleStack.get(CapsuleItem.STORED_STRUCTURE_DATA);
        StructureTemplate structure = CapsuleItem.getStructure(level, capsuleStack);

        Vec3i size = capsuleStack.has(CapsuleItem.STRUCTURE_SIZE) ? capsuleStack.get(CapsuleItem.STRUCTURE_SIZE) : structure.getSize();
        if (size.equals(Vec3i.ZERO)) return false;

        SchematicLevel schematicLevel = new SchematicLevel(level);
        StructurePlaceSettings placeSettings = new StructurePlaceSettings();

        try {
            structure.placeInWorld(schematicLevel, BlockPos.ZERO, BlockPos.ZERO, placeSettings, schematicLevel.getRandom(), Block.UPDATE_CLIENTS);
            for (BlockEntity blockEntity: schematicLevel.getBlockEntities()) blockEntity.setLevel(schematicLevel);
            fixControllerBlockEntities(schematicLevel);
        } catch (Exception e) {
            Minecraft.getInstance().player.displayClientMessage(CreateLang.translate("schematic.error").component(), false);
            GetCreative.LOGGER.error("Failed to load structure from capsule for Previewing", e);
            return false;
        }

        renderer = new SchematicRenderer(schematicLevel);
        return true;
    }

    protected boolean updateTransform(LocalPlayer player, ItemStack capsuleStack, boolean reinitialize) {
        Vec3i size = capsuleStack.get(CapsuleItem.STRUCTURE_SIZE);
        if (size == null) return false;

        HitResult hitResult = Minecraft.getInstance().hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return false;
        BlockHitResult blockHit = (BlockHitResult)hitResult;

        Rotation rotation = CapsuleItem.getStructureRotation(capsuleStack, player.getDirection());
        BlockPos offset = CapsuleItem.getOffset(capsuleStack);
        BlockPos anchor = blockHit.getBlockPos().relative(blockHit.getDirection()).offset(offset.rotate(rotation));

        if (!reinitialize && anchor.equals(prevAnchor) && rotation.equals(prevRotation)) return true;

        if (reinitialize) {
            transformation = new SchematicTransformation();
            bounds = new AABB(0, 0, 0, size.getX(), size.getY(), size.getZ());
            outline = new AABBOutline(bounds);
            outline.getParams()
                    .colored(0x6886c5)
                    .lineWidth(1 / 16f);
            transformation.init(anchor, new StructurePlaceSettings().setRotation(rotation), bounds);
        } else {
            ((ISchematicTransformation)transformation).getCreative$update(anchor, rotation);
        }
        return true;
    }

    protected ItemStack findHeldStructureCapsule(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.is(CapsuleItem.ITEM) && stack.has(CapsuleItem.STORED_STRUCTURE_DATA)) {
            return stack;
        }
        return null;
    }

    public static void fixControllerBlockEntities(SchematicLevel level) {
        for (BlockEntity blockEntity : level.getBlockEntities()) {
            if (!(blockEntity instanceof IMultiBlockEntityContainer multiBE) || multiBE.isController()) continue;
            BlockPos lastKnown = multiBE.getLastKnownPos();
            BlockPos current = blockEntity.getBlockPos();
            if (lastKnown == null || current == null || lastKnown.equals(current)) continue;

            BlockPos newControllerPos = multiBE.getController().offset(current.subtract(lastKnown));
            if (multiBE instanceof SmartBlockEntity smartBE) smartBE.markVirtual();
            multiBE.setController(newControllerPos);
        }
    }

}
