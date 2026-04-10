package amaryllis.get_creative.fluid_barrel;

import amaryllis.get_creative.Config;
import amaryllis.get_creative.GetCreative;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static com.simibubi.create.content.fluids.tank.FluidTankBlock.BOTTOM;
import static com.simibubi.create.content.fluids.tank.FluidTankBlock.TOP;

public class FluidBarrelBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IMultiBlockEntityContainer.Fluid {

    public static Supplier<BlockEntityType<FluidBarrelBlockEntity>> BLOCK_ENTITY;

    public static void register() {
        BLOCK_ENTITY = GetCreative.BLOCK_ENTITY_TYPES.register(
                "fluid_barrel", () -> BlockEntityType.Builder.of(
                        FluidBarrelBlockEntity::new, FluidBarrelBlock.BLOCK.get()
                ).build(null));
    }

    private static final int MAX_SIZE = 3;
    public static final float FLOOR_HEIGHT = 4 / 16f;
    public static final float TOP_MARGIN = 1 / 16f;
    public static final float HULL_WIDTH = 2 / 16f + 1 / 128f;
    public static final float MIN_PUDDLE_HEIGHT = 1 / 16f;

    protected IFluidHandler fluidCapability;
    protected boolean forceFluidLevelUpdate;
    protected FluidTank tankInventory;
    protected BlockPos controller;
    protected BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected boolean updateCapability;
    protected int luminosity;
    protected int width;
    protected int height;

    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean queuedSync;

    // For rendering purposes only
    private LerpedFloat fluidLevel;

    public FluidBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY.get(), pos, state);
        tankInventory = createInventory();
        forceFluidLevelUpdate = true;
        updateConnectivity = false;
        updateCapability = false;
        height = 1;
        width = 1;
        refreshCapability();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BLOCK_ENTITY.get(),
                (be, context) -> {
                    if (be.fluidCapability == null) be.refreshCapability();
                    return be.fluidCapability;
                }
        );
    }

    protected SmartFluidTank createInventory() {
        return new SmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level.isClientSide) return;
        if (!isController()) return;
        ConnectivityHandler.formMulti(this);
    }

    @Override
    public void tick() {
        super.tick();

        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync) sendData();
        }

        if (lastKnownPos == null)
            lastKnownPos = getBlockPos();
        else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
            onPositionChanged();
            return;
        }

        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
        if (updateConnectivity) updateConnectivity();
        if (fluidLevel != null) fluidLevel.tickChaser();
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX()
                && worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
    }

    @Override
    public void initialize() {
        super.initialize();
        sendData();
        if (level.isClientSide) invalidateRenderBoundingBox();
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    protected void onFluidStackChanged(FluidStack newFluidStack) {
        if (!hasLevel()) return;

        FluidType attributes = newFluidStack.getFluid().getFluidType();
        int luminosity = (int) (attributes.getLightLevel(newFluidStack) / 1.2f);
        boolean isFluidFloating = attributes.isLighterThanAir();
        int maxY = (int) ((getFillState() * height) + 1);

        for (int yOffset = 0; yOffset < height; yOffset++) {
            boolean isBright = isFluidFloating ? (height - yOffset <= maxY) : (yOffset < maxY);
            int actualLuminosity = isBright ? luminosity : luminosity > 0 ? 1 : 0;

            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    FluidBarrelBlockEntity tankAt = ConnectivityHandler.partAt(getType(), level, pos);
                    if (tankAt == null) continue;
                    level.updateNeighbourForOutputSignal(pos, tankAt.getBlockState().getBlock());
                    if (tankAt.luminosity == actualLuminosity) continue;
                    tankAt.setLuminosity(actualLuminosity);
                }
            }
        }

        if (!level.isClientSide) {
            setChanged();
            sendData();
        }

        if (isVirtual()) {
            if (fluidLevel == null) fluidLevel = LerpedFloat.linear().startWithValue(getFillState());
            fluidLevel.chase(getFillState(), .5f, LerpedFloat.Chaser.EXP);
        }
    }

    protected void setLuminosity(int luminosity) {
        if (level.isClientSide) return;
        if (this.luminosity == luminosity) return;
        this.luminosity = luminosity;
        sendData();
    }

    @SuppressWarnings("unchecked")
    @Override
    public FluidBarrelBlockEntity getControllerBE() {
        if (isController() || !hasLevel()) return this;
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof FluidBarrelBlockEntity barrelBE) return barrelBE;
        return null;
    }

    public void applyFluidTankSize(int blocks) {
        tankInventory.setCapacity(blocks * getCapacityMultiplier());
        int overflow = tankInventory.getFluidAmount() - tankInventory.getCapacity();
        if (overflow > 0) tankInventory.drain(overflow, IFluidHandler.FluidAction.EXECUTE);
        forceFluidLevelUpdate = true;
    }

    public void removeController(boolean keepFluids) {
        if (level.isClientSide) return;
        updateConnectivity = true;
        if (!keepFluids) applyFluidTankSize(1);
        controller = null;
        width = 1;
        height = 1;
        onFluidStackChanged(tankInventory.getFluid());

        BlockState state = getBlockState();
        if (state.getBlock() instanceof FluidBarrelBlock) {
            state = state.setValue(BOTTOM, true);
            state = state.setValue(TOP, true);
            getLevel().setBlock(worldPosition, state, 22);
        }

        refreshCapability();
        setChanged();
        sendData();
    }

    public void sendDataImmediately() {
        syncCooldown = 0;
        queuedSync = false;
        sendData();
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    @Override
    public void setController(BlockPos controller) {
        if (level.isClientSide && !isVirtual()) return;
        if (controller.equals(this.controller)) return;
        this.controller = controller;
        refreshCapability();
        setChanged();
        sendData();
    }

    void refreshCapability() {
        fluidCapability = handlerForCapability();
        invalidateCapabilities();
    }

    private IFluidHandler handlerForCapability() {
        if (isController()) return tankInventory;
        return (getControllerBE() != null) ? getControllerBE().handlerForCapability()
                : new FluidTank(0, FluidBarrelBlockEntity::canContainFluid);
    }

    public static boolean canContainFluid(FluidStack fluid) {
        FluidType fluidType = fluid.getFluidType();
        return fluidType.getTemperature(fluid) < Config.FLUID_BARREL_MAX_TEMPERATURE.getAsInt() &&
               !(fluidType.isLighterThanAir() && Config.FLUID_BARREL_CAN_STORE_GAS.isFalse());
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return (isController())
            ? super.createRenderBoundingBox().expandTowards(width - 1, height - 1, width - 1)
            : super.createRenderBoundingBox();
    }

    @Nullable
    public FluidBarrelBlockEntity getOtherFluidBarrelBlockEntity(Direction direction) {
        BlockEntity otherBE = level.getBlockEntity(worldPosition.relative(direction));
        if (otherBE instanceof FluidBarrelBlockEntity barrelBE) return barrelBE;
        return null;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        FluidBarrelBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null) return false;
        return containedFluidTooltip(tooltip, isPlayerSneaking,
                level.getCapability(Capabilities.FluidHandler.BLOCK, controllerBE.getBlockPos(), null));
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        BlockPos controllerBefore = controller;
        int prevSize = width;
        int prevHeight = height;
        int prevLum = luminosity;

        updateConnectivity = compound.contains("Uninitialized");
        luminosity = compound.getInt("Luminosity");

        lastKnownPos = null;
        if (compound.contains("LastKnownPos")) lastKnownPos = NBTHelper.readBlockPos(compound, "LastKnownPos");

        controller = null;
        if (compound.contains("Controller")) controller = NBTHelper.readBlockPos(compound, "Controller");

        if (isController()) {
            width = compound.getInt("Size");
            height = compound.getInt("Height");
            tankInventory.setCapacity(getTotalTankSize() * getCapacityMultiplier());

            tankInventory.readFromNBT(registries, compound.getCompound("TankContent"));
            if (tankInventory.getSpace() < 0)
                tankInventory.drain(-tankInventory.getSpace(), IFluidHandler.FluidAction.EXECUTE);
        }

        if (compound.contains("ForceFluidLevel") || fluidLevel == null)
            fluidLevel = LerpedFloat.linear().startWithValue(getFillState());

        updateCapability = true;

        if (!clientPacket) return;

        boolean changeOfController = !Objects.equals(controllerBefore, controller);
        if (changeOfController || prevSize != width || prevHeight != height) {
            if (hasLevel()) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
            if (isController()) tankInventory.setCapacity(getCapacityMultiplier() * getTotalTankSize());
            invalidateRenderBoundingBox();
        }
        if (isController()) {
            float fillState = getFillState();
            if (compound.contains("ForceFluidLevel") || fluidLevel == null)
                fluidLevel = LerpedFloat.linear().startWithValue(fillState);
            fluidLevel.chase(fillState, 0.5f, LerpedFloat.Chaser.EXP);
        }
        if (luminosity != prevLum && hasLevel())
            level.getChunkSource().getLightEngine().checkBlock(worldPosition);

        if (compound.contains("LazySync"))
            fluidLevel.chase(fluidLevel.getChaseTarget(), 0.125f, LerpedFloat.Chaser.EXP);
    }

    public float getFillState() {
        return (float) tankInventory.getFluidAmount() / tankInventory.getCapacity();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        if (updateConnectivity) compound.putBoolean("Uninitialized", true);
        if (lastKnownPos != null) compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        if (isController()) {
            compound.put("TankContent", tankInventory.writeToNBT(registries, new CompoundTag()));
            compound.putInt("Size", width);
            compound.putInt("Height", height);
        } else {
            compound.put("Controller", NbtUtils.writeBlockPos(controller));
        }
        compound.putInt("Luminosity", luminosity);
        super.write(compound, registries, clientPacket);

        if (!clientPacket) return;
        if (forceFluidLevelUpdate) compound.putBoolean("ForceFluidLevel", true);
        if (queuedSync) compound.putBoolean("LazySync", true);
        forceFluidLevelUpdate = false;
    }

    @Override
    public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
        if (isController()) {
            compound.putInt("Size", width);
            compound.putInt("Height", height);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        registerAwardables(behaviours, AllAdvancements.STEAM_ENGINE_MAXED, AllAdvancements.PIPE_ORGAN);
    }

    public FluidTank getTankInventory() {
        return tankInventory;
    }

    public int getTotalTankSize() {
        return width * width * height;
    }

    public static int getMaxSize() {
        return MAX_SIZE;
    }

    public static int getCapacityMultiplier() {
        return Config.FLUID_BARREL_CAPACITY.get() * 1000;
    }

    public static int getMaxHeight() {
        return Config.FLUID_BARREL_MAX_HEIGHT.get();
    }

    public LerpedFloat getFluidLevel() {
        return fluidLevel;
    }

    public void setFluidLevel(LerpedFloat fluidLevel) {
        this.fluidLevel = fluidLevel;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = this.getBlockState();
        if (state.getBlock() instanceof FluidBarrelBlock) { // safety
            state = state.setValue(BOTTOM, getController().getY() == getBlockPos().getY());
            state = state.setValue(TOP, getController().getY() + height - 1 == getBlockPos().getY());
            level.setBlock(getBlockPos(), state, 6);
        }
        onFluidStackChanged(tankInventory.getFluid());
        setChanged();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return Direction.Axis.Y;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        return (longAxis == Direction.Axis.Y) ? getMaxHeight() : getMaxWidth();
    }

    @Override
    public int getMaxWidth() {
        return MAX_SIZE;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public int getTankSize(int tank) {
        return getCapacityMultiplier();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyFluidTankSize(blocks);
    }

    @Override
    public IFluidTank getTank(int tank) {
        return tankInventory;
    }

    @Override
    public FluidStack getFluid(int tank) {
        return tankInventory.getFluid().copy();
    }

    public float[] getFluidRenderY(float partialTicks, boolean client) {
        var be = isController() ? this : getControllerBE();

        float level;
        if (client) {
            LerpedFloat fluidLevel = be.getFluidLevel();
            if (fluidLevel == null) return null;
            level = fluidLevel.getValue(partialTicks);
        } else {
            level = be.getFillState();
        }

        float totalHeight = be.height - FLOOR_HEIGHT - TOP_MARGIN - MIN_PUDDLE_HEIGHT;
        if (level < 1 / (512f * totalHeight)) return null;

        FluidStack fluidStack = be.tankInventory.getFluid();
        if (fluidStack.isEmpty()) return null;

        float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);

        boolean isFluidFloating = fluidStack.getFluid().getFluidType().isLighterThanAir();
        float floatingOffset = (isFluidFloating) ? (totalHeight - clampedLevel) : 0;

        float yMin = totalHeight + FLOOR_HEIGHT + MIN_PUDDLE_HEIGHT - clampedLevel + floatingOffset;
        return new float[] { yMin, yMin + clampedLevel };
    }
    public float getFluidTopY() {
        var be = isController() ? this : getControllerBE();
        float baseY = be.getBlockPos().getY();
        float[] fluidBounds = be.getFluidRenderY(0, false);
        return (fluidBounds == null) ? baseY
               : baseY + be.height - fluidBounds[0] + FLOOR_HEIGHT;
    }

    public float getDistanceToInsideWall(Entity entity) {
        final FluidBarrelBlockEntity controller = getControllerBE();
        final AABB entityBounds = entity.getBoundingBox();
        final float minX = controller.getBlockPos().getX() + 2/16f;
        final float maxX = controller.getBlockPos().getX() + controller.width - 2/16f;
        final float minZ = controller.getBlockPos().getZ() + 2/16f;
        final float maxZ = controller.getBlockPos().getZ() + controller.width - 2/16f;
        double output = Float.MAX_VALUE;
        output = Math.min(output, Math.abs(entityBounds.minX - minX));
        output = Math.min(output, Math.abs(entityBounds.maxX - maxX));
        output = Math.min(output, Math.abs(entityBounds.minZ - minZ));
        output = Math.min(output, Math.abs(entityBounds.maxZ - maxZ));
        return (float)output;
    }

    public void handleEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        var controller = isController() ? this : getControllerBE();
        if (controller.tankInventory.isEmpty()) return;

        FluidStack fluidStack = controller.tankInventory.getFluid();
        FluidType fluidType = fluidStack.getFluidType();

        float fluidTopY = getFluidTopY();
        if (entity.getY() > fluidTopY) return;

        // Make entity float in fluid
        // Add levitation to simulate bobbing to the top of the fluid
        if (entity instanceof LivingEntity livingEntity && !level.isClientSide) {
            if (!(entity instanceof Mob mob) || mob.getNavigation().canFloat()) {
                double submersionThreshold = Mth.lerp(0.5, entity.getY(), entity.getEyeY());
                if (submersionThreshold < fluidTopY) {
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 3, 0, true, false, false));
                }
            }
        } else if (entity instanceof ItemEntity itemEntity) {
            if (entity.getBoundingBox().maxY < fluidTopY && level.getGameTime() % 4 != 0) {
                var deltaMovement = itemEntity.getDeltaMovement();
                // Trying to decrease this speed or add it to the existing delta results in nothing happening
                // Instead we only apply it every few ticks. Anything below 3/4 ticks loses height
                itemEntity.setDeltaMovement(new Vec3(deltaMovement.x, 0.05, deltaMovement.z));
            }
        }

        if (fluidType.canExtinguish(entity)) {
            entity.extinguishFire();
            return;
        }

        int temperature = fluidType.getTemperature(fluidStack);
        final int minBurnTemperature = Config.FLUID_BARREL_MIN_BURN_TEMPERATURE.getAsInt();
        if (!entity.fireImmune() && temperature >= minBurnTemperature) {
            final int maxBurnTemperature = Config.FLUID_BARREL_MAX_BURN_TEMPERATURE.getAsInt();
            int damage = (int)(1 + 3 * Mth.inverseLerp(temperature, minBurnTemperature, maxBurnTemperature));
            entity.igniteForSeconds(15f);
            if (entity.hurt(entity.damageSources().lava(), damage)) {
                entity.playSound(SoundEvents.GENERIC_BURN, 0.4f, 2f + entity.getRandom().nextFloat() * 0.4f);
            }
        }
    }
}
