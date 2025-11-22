package com.vibey.copycraft.client;

import com.vibey.copycraft.block.CopyBlock;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CopyBlockModel implements BakedModel {

    public static final ModelProperty<BlockState> COPIED_STATE = new ModelProperty<>();
    public static final ModelProperty<Integer> VIRTUAL_ROTATION = new ModelProperty<>();
    public static final ModelProperty<boolean[]> CULL_FACES = new ModelProperty<>();

    private final BakedModel baseModel;

    public CopyBlockModel(BakedModel baseModel) {
        this.baseModel = baseModel;
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        return baseModel.getQuads(state, side, rand);
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    @NotNull RandomSource rand, @NotNull ModelData data,
                                    @Nullable RenderType renderType) {

        BlockState copiedState = data.get(COPIED_STATE);
        Integer virtualRotation = data.get(VIRTUAL_ROTATION);
        boolean[] cullFaces = data.get(CULL_FACES);

        List<BakedQuad> baseQuads = baseModel.getQuads(state, side, rand, ModelData.EMPTY, renderType);

        if (copiedState == null || copiedState.isAir()) {
            return baseQuads;
        }

        if (virtualRotation == null) {
            virtualRotation = 0;
        }

        // Check if this face should be culled (for glass-like behavior)
        if (cullFaces != null && side != null && cullFaces[side.ordinal()]) {
            return Collections.emptyList();
        }

        BakedModel copiedModel = Minecraft.getInstance()
                .getBlockRenderer()
                .getBlockModel(copiedState);

        Direction textureFace = applyLogRotation(side, virtualRotation);

        List<BakedQuad> copiedQuads = copiedModel.getQuads(copiedState, textureFace, rand, ModelData.EMPTY, renderType);
        if (copiedQuads.isEmpty() && textureFace != null) {
            copiedQuads = copiedModel.getQuads(copiedState, null, rand, ModelData.EMPTY, renderType);
        }

        TextureAtlasSprite sprite;
        BakedQuad sourceQuad = null;
        if (!copiedQuads.isEmpty()) {
            sourceQuad = copiedQuads.get(0);
            sprite = sourceQuad.getSprite();
        } else {
            sprite = copiedModel.getParticleIcon(ModelData.EMPTY);
        }

        List<BakedQuad> remappedQuads = new ArrayList<>(baseQuads.size());
        for (BakedQuad quad : baseQuads) {
            remappedQuads.add(remapQuadTexture(quad, sprite, sourceQuad));
        }

        return remappedQuads;
    }

    private Direction applyLogRotation(@Nullable Direction face, int rotation) {
        if (face == null || rotation == 0) {
            return face;
        }
        return switch (rotation) {
            case 1 -> switch (face) {
                case UP -> Direction.SOUTH;
                case DOWN -> Direction.NORTH;
                case NORTH -> Direction.DOWN;
                case SOUTH -> Direction.UP;
                default -> face;
            };
            case 2 -> switch (face) {
                case UP -> Direction.EAST;
                case DOWN -> Direction.WEST;
                case EAST -> Direction.DOWN;
                case WEST -> Direction.UP;
                default -> face;
            };
            default -> face;
        };
    }

    private BakedQuad remapQuadTexture(BakedQuad originalQuad, TextureAtlasSprite newSprite, @Nullable BakedQuad sourceQuad) {
        int[] vertexData = originalQuad.getVertices().clone();
        TextureAtlasSprite oldSprite = originalQuad.getSprite();

        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            float u = Float.intBitsToFloat(vertexData[offset + 4]);
            float v = Float.intBitsToFloat(vertexData[offset + 5]);

            float relativeU = (u - oldSprite.getU0()) / (oldSprite.getU1() - oldSprite.getU0());
            float relativeV = (v - oldSprite.getV0()) / (oldSprite.getV1() - oldSprite.getV0());

            float newU = newSprite.getU0() + relativeU * (newSprite.getU1() - newSprite.getU0());
            float newV = newSprite.getV0() + relativeV * (newSprite.getV1() - newSprite.getV0());

            vertexData[offset + 4] = Float.floatToRawIntBits(newU);
            vertexData[offset + 5] = Float.floatToRawIntBits(newV);
        }

        int tintIndex = sourceQuad != null ? sourceQuad.getTintIndex() : originalQuad.getTintIndex();

        return new BakedQuad(vertexData, tintIndex, originalQuad.getDirection(), newSprite, originalQuad.isShade());
    }

    @NotNull
    @Override
    public ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                  @NotNull BlockState state, @NotNull ModelData modelData) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            int rotation = copyBE.getVirtualRotation();

            // Calculate which faces should be culled based on neighbors
            boolean[] cullFaces = new boolean[6];

            if (copiedState != null && !copiedState.isAir()) {
                // Check if copied block is one that should have face culling (like glass)
                if (shouldCullMatchingFaces(copiedState)) {
                    for (Direction dir : Direction.values()) {
                        BlockPos neighborPos = pos.relative(dir);
                        BlockState neighborState = level.getBlockState(neighborPos);
                        BlockEntity neighborBE = level.getBlockEntity(neighborPos);

                        // First check: does the neighbor's face fully cover ours?
                        if (!doesNeighborCoverFace(state, neighborState, level, pos, neighborPos, dir)) {
                            continue;
                        }

                        if (neighborBE instanceof CopyBlockEntity neighborCopyBE) {
                            BlockState neighborCopied = neighborCopyBE.getCopiedBlock();
                            // Cull if neighbor is copying the same block type
                            if (neighborCopied != null &&
                                    neighborCopied.getBlock() == copiedState.getBlock()) {
                                cullFaces[dir.ordinal()] = true;
                            }
                        } else {
                            // Also cull against actual blocks of the same type
                            if (neighborState.getBlock() == copiedState.getBlock()) {
                                cullFaces[dir.ordinal()] = true;
                            }
                        }
                    }
                }
            }

            return ModelData.builder()
                    .with(COPIED_STATE, copiedState != null ? copiedState : Blocks.AIR.defaultBlockState())
                    .with(VIRTUAL_ROTATION, rotation)
                    .with(CULL_FACES, cullFaces)
                    .build();
        }
        return ModelData.builder()
                .with(COPIED_STATE, Blocks.AIR.defaultBlockState())
                .with(VIRTUAL_ROTATION, 0)
                .with(CULL_FACES, new boolean[6])
                .build();
    }

    /**
     * Check if the neighbor block's face fully covers our face in the given direction.
     * Only cull if the neighbor provides full coverage.
     */
    private boolean doesNeighborCoverFace(BlockState ourState, BlockState neighborState,
                                          BlockAndTintGetter level, BlockPos ourPos,
                                          BlockPos neighborPos, Direction dir) {
        // Get our face's shape (the face we might cull)
        VoxelShape ourShape = ourState.getFaceOcclusionShape(level, ourPos, dir);

        // Get neighbor's shape on the opposite face (the face touching ours)
        Direction opposite = dir.getOpposite();
        VoxelShape neighborShape = neighborState.getFaceOcclusionShape(level, neighborPos, opposite);

        // Only cull if neighbor fully covers our face
        // For full blocks touching full blocks, both shapes will be full 16x16 faces
        // For stairs/slabs, the shapes will be partial

        if (ourShape.isEmpty()) {
            return false;
        }

        if (neighborShape.isEmpty()) {
            return false;
        }

        // Check if neighbor's face fully contains our face
        return Shapes.joinIsNotEmpty(ourShape, neighborShape, BooleanOp.ONLY_FIRST) == false;
    }

    /**
     * Check if this block type should have its faces culled when adjacent to same type.
     * This applies to glass, ice, and similar transparent blocks.
     */
    private boolean shouldCullMatchingFaces(BlockState state) {
        // Glass and glass panes
        if (state.is(Blocks.GLASS) ||
                state.is(Blocks.TINTED_GLASS) ||
                state.getBlock().getName().getString().toLowerCase().contains("glass")) {
            return true;
        }

        // Ice blocks
        if (state.is(Blocks.ICE) ||
                state.is(Blocks.PACKED_ICE) ||
                state.is(Blocks.BLUE_ICE)) {
            return true;
        }

        // Slime and honey
        if (state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean useAmbientOcclusion() { return true; }
    @Override
    public boolean isGui3d() { return true; }
    @Override
    public boolean usesBlockLight() { return true; }
    @Override
    public boolean isCustomRenderer() { return false; }

    @Override
    public TextureAtlasSprite getParticleIcon() { return baseModel.getParticleIcon(); }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        BlockState copiedState = data.get(COPIED_STATE);
        if (copiedState != null && !copiedState.isAir()) {
            BakedModel copiedModel = Minecraft.getInstance()
                    .getBlockRenderer()
                    .getBlockModel(copiedState);
            return copiedModel.getParticleIcon(ModelData.EMPTY);
        }
        return baseModel.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() { return baseModel.getTransforms(); }
    @Override
    public ItemOverrides getOverrides() { return baseModel.getOverrides(); }

    @NotNull
    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        BlockState copiedState = data.get(COPIED_STATE);
        if (copiedState != null && !copiedState.isAir()) {
            BakedModel copiedModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(copiedState);
            return copiedModel.getRenderTypes(copiedState, rand, ModelData.EMPTY);
        }
        return baseModel.getRenderTypes(state, rand, data);
    }
}