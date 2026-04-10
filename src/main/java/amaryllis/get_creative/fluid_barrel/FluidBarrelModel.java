package amaryllis.get_creative.fluid_barrel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import amaryllis.get_creative.CTHelper;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.*;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class FluidBarrelModel extends CTModel {

    public static final CTSpriteShiftEntry
            CT_BLOCK = CTHelper.get(AllCTTypes.RECTANGLE, "fluid_barrel/block"),
            CT_TOP   = CTHelper.get(AllCTTypes.RECTANGLE, "fluid_barrel/top"),
            CT_INNER = CTHelper.get(AllCTTypes.RECTANGLE, "fluid_barrel/inner");

    protected static final ModelProperty<CullData> CULL_PROPERTY = new ModelProperty<>();

    public FluidBarrelModel(BakedModel originalModel) {
        super(originalModel, new FluidTankCTBehaviour(CT_BLOCK, CT_TOP, CT_INNER));
    }

    @Override
    protected ModelData.Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
        super.gatherModelData(builder, world, pos, state, blockEntityData);
        CullData cullData = new CullData();
        for (Direction side: Iterate.horizontalDirections)
            cullData.setCulled(side, ConnectivityHandler.isConnected(world, pos, pos.relative(side)));
        return builder.with(CULL_PROPERTY, cullData);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
        if (side != null) return Collections.emptyList();

        List<BakedQuad> quads = new ArrayList<>();
        for (Direction size: Iterate.directions) {
            if (extraData.has(CULL_PROPERTY) && extraData.get(CULL_PROPERTY).isCulled(size)) continue;
            quads.addAll(super.getQuads(state, size, rand, extraData, renderType));
        }
        quads.addAll(super.getQuads(state, null, rand, extraData, renderType));
        return quads;
    }

    protected static class CullData {
        boolean[] culledFaces;

        public CullData() {
            culledFaces = new boolean[4];
            Arrays.fill(culledFaces, false);
        }

        void setCulled(Direction face, boolean cull) {
            if (face.getAxis().isVertical()) return;
            culledFaces[face.get2DDataValue()] = cull;
        }

        boolean isCulled(Direction face) {
            if (face.getAxis().isVertical()) return false;
            return culledFaces[face.get2DDataValue()];
        }
    }
}
