package com.vibey.copycraft.client.model;

import com.vibey.copycraft.blocks.CopyBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Custom baked model that renders the material block's model
 * This is called during chunk rebuild, not every frame - much more efficient!
 */
public class CopyBakedModel implements BakedModel {

    private final BakedModel baseModel; // Fallback model when no material is applied

    public CopyBakedModel(BakedModel baseModel) {
        this.baseModel = baseModel;
    }

    @Override
    @NotNull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        // Get the material from ModelData
        BlockState material = extraData.get(CopyBlock.MATERIAL_PROPERTY);

        if (material != null && !material.isAir()) {
            // Get the material's baked model
            BlockModelShaper shaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
            BakedModel materialModel = shaper.getBlockModel(material);

            // Return the material's quads for this side and render type
            return materialModel.getQuads(material, side, rand, ModelData.EMPTY, renderType);
        }

        // No material - return base model quads
        return baseModel.getQuads(state, side, rand, extraData, renderType);
    }

    // ========== Delegate to base model for non-rendering methods ==========

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
        return false; // We're rendering through the normal pipeline
    }

    @Override
    @NotNull
    public TextureAtlasSprite getParticleIcon() {
        return baseModel.getParticleIcon();
    }

    @Override
    @NotNull
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        // Try to get particle texture from material
        BlockState material = data.get(CopyBlock.MATERIAL_PROPERTY);
        if (material != null && !material.isAir()) {
            BlockModelShaper shaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
            BakedModel materialModel = shaper.getBlockModel(material);
            return materialModel.getParticleIcon(ModelData.EMPTY);
        }
        return baseModel.getParticleIcon(data);
    }

    @Override
    @NotNull
    public ItemTransforms getTransforms() {
        return baseModel.getTransforms();
    }

    @Override
    @NotNull
    public ItemOverrides getOverrides() {
        return baseModel.getOverrides();
    }

    // ========== Deprecated methods - delegate to new API ==========

    @Override
    @NotNull
    @Deprecated
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }
}