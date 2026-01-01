package com.vibey.imitari.client;

import com.google.common.collect.Lists;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Wrapper for MultiPartBakedModel that applies CopyBlock texture remapping to all sub-models.
 */
public class CopyBlockMultiPartModel implements BakedModel {

    private final BakedModel baseModel;
    private final List<Pair<Predicate<BlockState>, BakedModel>> selectors;

    public CopyBlockMultiPartModel(BakedModel baseModel, List<Pair<Predicate<BlockState>, BakedModel>> selectors) {
        this.baseModel = baseModel;
        this.selectors = selectors;
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        return baseModel.getQuads(state, side, rand);
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    @NotNull RandomSource rand, @NotNull ModelData extraData,
                                    @Nullable RenderType renderType) {
        if (state == null) {
            return Collections.emptyList();
        }

        // Collect quads from all matching sub-models
        List<BakedQuad> quads = Lists.newArrayList();

        for (Pair<Predicate<BlockState>, BakedModel> pair : selectors) {
            if (pair.getLeft().test(state)) {
                BakedModel subModel = pair.getRight();

                // If the sub-model is a CopyBlockModel, it will handle texture remapping
                // Otherwise, wrap it on the fly
                if (subModel instanceof CopyBlockModel) {
                    quads.addAll(subModel.getQuads(state, side, rand, extraData, renderType));
                } else {
                    // Wrap and get quads
                    CopyBlockModel wrappedModel = new CopyBlockModel(subModel);
                    quads.addAll(wrappedModel.getQuads(state, side, rand, extraData, renderType));
                }
            }
        }

        return quads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return baseModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return baseModel.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        BlockState copiedState = data.get(CopyBlockModel.COPIED_STATE);
        if (copiedState != null && !copiedState.isAir()) {
            var copiedModel = net.minecraft.client.Minecraft.getInstance()
                    .getBlockRenderer()
                    .getBlockModel(copiedState);
            return copiedModel.getParticleIcon(ModelData.EMPTY);
        }
        return baseModel.getParticleIcon(data);
    }

    @Override
    public ItemTransforms getTransforms() {
        return baseModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return baseModel.getOverrides();
    }

    @NotNull
    @Override
    public ModelData getModelData(@NotNull net.minecraft.world.level.BlockAndTintGetter level,
                                  @NotNull net.minecraft.core.BlockPos pos,
                                  @NotNull BlockState state,
                                  @NotNull ModelData modelData) {
        // Use CopyBlockModel's logic for getting model data
        return new CopyBlockModel(baseModel).getModelData(level, pos, state, modelData);
    }

    @NotNull
    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        BlockState copiedState = data.get(CopyBlockModel.COPIED_STATE);
        if (copiedState != null && !copiedState.isAir()) {
            BakedModel copiedModel = net.minecraft.client.Minecraft.getInstance()
                    .getBlockRenderer()
                    .getBlockModel(copiedState);
            return copiedModel.getRenderTypes(copiedState, rand, ModelData.EMPTY);
        }
        return baseModel.getRenderTypes(state, rand, data);
    }
}