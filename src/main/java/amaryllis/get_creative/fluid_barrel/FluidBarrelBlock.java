package amaryllis.get_creative.fluid_barrel;

import amaryllis.get_creative.GetCreative;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.BitSet;

import static com.simibubi.create.content.fluids.tank.FluidTankBlock.TOP;
import static com.simibubi.create.content.fluids.tank.FluidTankBlock.BOTTOM;

public class FluidBarrelBlock extends Block implements IBE<FluidBarrelBlockEntity> {

    public static DeferredBlock<Block> BLOCK;
    public static DeferredItem<BlockItem> ITEM;
    private static final VoxelShape[] SHAPES;

    public static void register() {
        BLOCK = GetCreative.BLOCKS.registerBlock(
                "fluid_barrel", FluidBarrelBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).noOcclusion()
                        .isRedstoneConductor((p1, p2, p3) -> true)
        );
        ITEM = GetCreative.ITEMS.registerItem("fluid_barrel", (properties) -> new FluidBarrelItem(BLOCK.get(), properties));
        FluidBarrelBlockEntity.register();
    }

    protected FluidBarrelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TOP, true).setValue(BOTTOM, true));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock()) return;
        if (moved) return;
        withBlockEntityDo(level, pos, FluidBarrelBlockEntity::updateConnectivity);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> state) {
        state.add(TOP, BOTTOM);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        FluidBarrelBlockEntity be = ConnectivityHandler.partAt(getBlockEntityType(), level, pos);
        if (be == null || !be.hasLevel()) return 0;

        FluidBarrelBlockEntity controllerBE = be.getControllerBE();
        if (controllerBE == null) return 0;

        return be.luminosity;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        final int shapeIndex =
                  (ConnectivityHandler.isConnected(level, pos, pos.below()) ? 0 : 1 )
                | (ConnectivityHandler.isConnected(level, pos, pos.north()) ? 0 : 2 )
                | (ConnectivityHandler.isConnected(level, pos, pos.east())  ? 0 : 4 )
                | (ConnectivityHandler.isConnected(level, pos, pos.south()) ? 0 : 8 )
                | (ConnectivityHandler.isConnected(level, pos, pos.west())  ? 0 : 16 );
        return SHAPES[shapeIndex];
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        FluidBarrelBlockEntity be = ConnectivityHandler.partAt(getBlockEntityType(), level, pos);
        if (be != null) be.handleEntityInside(state, level, pos, entity);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        boolean onClient = level.isClientSide;

        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        FluidHelper.FluidExchange exchange = null;
        FluidBarrelBlockEntity be = ConnectivityHandler.partAt(getBlockEntityType(), level, pos);
        if (be == null) return ItemInteractionResult.FAIL;

        IFluidHandler tankCapability = level.getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), null);
        if (tankCapability == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        FluidStack prevFluidInTank = tankCapability.getFluidInTank(0).copy();

        // Validate if we can insert the fluid
        if (!player.isCreative() && GenericItemEmptying.canItemBeEmptied(level, stack)) {
            FluidStack fluidToBeEmptied = GenericItemEmptying.emptyItem(level, stack, true).getFirst();
            if (!FluidBarrelBlockEntity.canContainFluid(fluidToBeEmptied)) {
                player.displayClientMessage(Component.translatable("get_creative.fluid_barrel.invalid_fluid",
                                            Component.translatable(fluidToBeEmptied.getDescriptionId())), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, be))
            exchange = FluidHelper.FluidExchange.ITEM_TO_TANK;
        else if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, be))
            exchange = FluidHelper.FluidExchange.TANK_TO_ITEM;

        if (exchange == null) {
            return (GenericItemEmptying.canItemBeEmptied(level, stack) || GenericItemFilling.canItemBeFilled(level, stack))
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        SoundEvent sound = null;
        BlockState fluidState = null;
        FluidStack fluidInTank = tankCapability.getFluidInTank(0);

        if (exchange == FluidHelper.FluidExchange.ITEM_TO_TANK) {
            Fluid fluid = fluidInTank.getFluid();
            fluidState = fluid.defaultFluidState().createLegacyBlock();
            sound = FluidHelper.getEmptySound(fluidInTank);
        }

        if (exchange == FluidHelper.FluidExchange.TANK_TO_ITEM) {
            Fluid fluid = prevFluidInTank.getFluid();
            fluidState = fluid.defaultFluidState().createLegacyBlock();
            sound = FluidHelper.getFillSound(prevFluidInTank);
        }

        if (sound != null && !onClient) {
            float pitch = Mth
                    .clamp(1 - (1f * fluidInTank.getAmount() / (FluidBarrelBlockEntity.getCapacityMultiplier() * 16)), 0, 1);
            pitch /= 1.5f;
            pitch += .5f;
            pitch += (level.random.nextFloat() - .5f) / 4f;
            level.playSound(null, pos, sound, SoundSource.BLOCKS, .5f, pitch);
        }

        if (!FluidStack.isSameFluidSameComponents(fluidInTank, prevFluidInTank)) {
            FluidBarrelBlockEntity controllerBE = be.getControllerBE();
            if (controllerBE != null) {
                if (fluidState != null && onClient) {
                    BlockParticleOption blockParticleData = new BlockParticleOption(ParticleTypes.BLOCK, fluidState);
                    float fluidLevel = (float) fluidInTank.getAmount() / tankCapability.getTankCapacity(0);

                    boolean reversed = fluidInTank.getFluid().getFluidType().isLighterThanAir();
                    if (reversed) fluidLevel = 1 - fluidLevel;

                    Vec3 vec = hitResult.getLocation();
                    vec = new Vec3(vec.x, controllerBE.getBlockPos()
                            .getY() + fluidLevel * (controllerBE.height - .5f) + .25f, vec.z);
                    Vec3 motion = player.position()
                            .subtract(vec)
                            .scale(1 / 20f);
                    vec = vec.add(motion);
                    level.addParticle(blockParticleData, vec.x, vec.y, vec.z, motion.x, motion.y, motion.z);
                    return ItemInteractionResult.SUCCESS;
                }

                controllerBE.sendDataImmediately();
                controllerBE.setChanged();
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof FluidBarrelBlockEntity tankBE)) return;
            level.removeBlockEntity(pos);
            ConnectivityHandler.splitMulti(tankBE);
        }
    }

    @Override
    public Class<FluidBarrelBlockEntity> getBlockEntityClass() {
        return FluidBarrelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FluidBarrelBlockEntity> getBlockEntityType() {
        return FluidBarrelBlockEntity.BLOCK_ENTITY.get();
    }

    // Tanks are less noisy when placed in batch
    public static final SoundType SILENCED_WOOD =
            new DeferredSoundType(0.1F, 1.5F, () -> SoundEvents.WOOD_BREAK, () -> SoundEvents.WOOD_STEP,
                    () -> SoundEvents.WOOD_PLACE, () -> SoundEvents.WOOD_HIT, () -> SoundEvents.WOOD_FALL);

    @Override
    public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, Entity entity) {
        return (entity != null && entity.getPersistentData().contains("SilenceTankSound"))
            ? SILENCED_WOOD : super.getSoundType(state, world, pos, entity);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
        return getBlockEntityOptional(worldIn, pos).map(FluidBarrelBlockEntity::getControllerBE)
                .map(be -> ComparatorUtil.fractionToRedstoneLevel(be.getFillState()))
                .orElse(0);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        if (!(level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity fluidBarrel)) return false;
        return fluidBarrel.getDistanceToInsideWall(entity) < 1 / 16f;
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        if (level.isClientSide) return;

        boolean fillWater = (precipitation == Biome.Precipitation.RAIN) && level.getRandom().nextFloat() < 0.05F;
        if (fillWater) {
            FluidBarrelBlockEntity be = ConnectivityHandler.partAt(getBlockEntityType(), level, pos);
            if (be == null) return;
            IFluidHandler tankCapability = level.getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), null);
            if (tankCapability == null) return;

            FluidStack rainWater = new FluidStack(Fluids.WATER, 250);
            if (rainWater.getAmount() != tankCapability.fill(rainWater, IFluidHandler.FluidAction.SIMULATE)) return;

            tankCapability.fill(rainWater, IFluidHandler.FluidAction.EXECUTE);
            FluidBarrelBlockEntity controllerBE = be.getControllerBE();
            if (controllerBE != null) {
                controllerBE.sendDataImmediately();
                controllerBE.setChanged();
            }
        }
    }

    static {
        SHAPES = new VoxelShape[32];
        var downBox  = Block.box( 0, 0,  0,  16,  2, 16);
        var northBox = Block.box( 0, 0,  0,  16, 16,  2);
        var eastBox  = Block.box(14, 0,  0,  16, 16, 16);
        var southBox = Block.box( 0, 0, 14,  16, 16, 16);
        var westBox  = Block.box( 0, 0,  0,   2, 16, 16);
        for (int i = 0; i < 32; i++) {
            BitSet bitSet = BitSet.valueOf(new long[]{i});
            VoxelShape shape = Shapes.empty();
            if (bitSet.get(0)) shape = Shapes.join(shape, downBox,  BooleanOp.OR);
            if (bitSet.get(1)) shape = Shapes.join(shape, northBox, BooleanOp.OR);
            if (bitSet.get(2)) shape = Shapes.join(shape, eastBox,  BooleanOp.OR);
            if (bitSet.get(3)) shape = Shapes.join(shape, southBox, BooleanOp.OR);
            if (bitSet.get(4)) shape = Shapes.join(shape, westBox,  BooleanOp.OR);
            SHAPES[i] = shape;
        }
    }
}
