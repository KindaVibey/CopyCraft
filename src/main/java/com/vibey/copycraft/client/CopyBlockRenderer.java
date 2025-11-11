package com.vibey.copycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

public class CopyBlockRenderer implements BlockEntityRenderer<CopyBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public CopyBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CopyBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockState copiedState = blockEntity.getCopiedBlock();
        BlockState copyBlockState = blockEntity.getBlockState();
        BlockPos pos = blockEntity.getBlockPos();

        poseStack.pushPose();

        try {
            BakedModel copyBlockModel = blockRenderer.getBlockModel(copyBlockState);

            TextureAtlasSprite textureSprite;
            if (blockEntity.hasCopiedBlock()) {
                BakedModel copiedModel = blockRenderer.getBlockModel(copiedState);
                textureSprite = copiedModel.getParticleIcon(ModelData.EMPTY);
            } else {
                BakedModel defaultModel = blockRenderer.getBlockModel(Blocks.OAK_PLANKS.defaultBlockState());
                textureSprite = defaultModel.getParticleIcon(ModelData.EMPTY);
            }

            renderModelWithTexture(copyBlockModel, textureSprite, poseStack, bufferSource,
                    packedOverlay, copyBlockState, pos, blockEntity.getLevel());

        } catch (Exception e) {
            e.printStackTrace();
        }

        poseStack.popPose();
    }

    private void renderModelWithTexture(BakedModel model, TextureAtlasSprite texture,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedOverlay, BlockState state, BlockPos pos,
                                        BlockAndTintGetter level) {

        RandomSource random = RandomSource.create(42L);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
        boolean useAO = Minecraft.useAmbientOcclusion() && model.useAmbientOcclusion();

        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(state, direction, random, ModelData.EMPTY, RenderType.cutout());
            if (!quads.isEmpty()) {
                renderQuadsWithTexture(quads, texture, poseStack, consumer, packedOverlay, pos, level, direction, useAO);
            }
        }

        List<BakedQuad> quads = model.getQuads(state, null, random, ModelData.EMPTY, RenderType.cutout());
        if (!quads.isEmpty()) {
            renderQuadsWithTexture(quads, texture, poseStack, consumer, packedOverlay, pos, level, null, useAO);
        }
    }

    private void renderQuadsWithTexture(List<BakedQuad> quads, TextureAtlasSprite sprite,
                                        PoseStack poseStack, VertexConsumer consumer,
                                        int packedOverlay, BlockPos pos, BlockAndTintGetter level,
                                        Direction cullFace, boolean useAO) {

        PoseStack.Pose pose = poseStack.last();

        for (BakedQuad quad : quads) {
            int[] vertexData = quad.getVertices();
            Direction face = quad.getDirection();

            // Calculate light using Minecraft's method
            int combinedLight = LightTexture.pack(
                    level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos.relative(face)),
                    level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos.relative(face))
            );

            // Apply shade if the quad has shading enabled
            float shadeR = 1.0F, shadeG = 1.0F, shadeB = 1.0F;

            // Check if this quad should be shaded (directional lighting)
            if (quad.isShade()) {
                // Apply vanilla directional shading based on face direction
                float shadeFactor = level.getShade(face, true);
                shadeR = shadeG = shadeB = shadeFactor;
            }

            // Process 4 vertices
            for (int i = 0; i < 4; i++) {
                int idx = i * 8;

                float x = Float.intBitsToFloat(vertexData[idx + 0]);
                float y = Float.intBitsToFloat(vertexData[idx + 1]);
                float z = Float.intBitsToFloat(vertexData[idx + 2]);

                // Get color and apply shading
                int color = vertexData[idx + 3];
                int r = (int) (((color >> 16) & 0xFF) * shadeR);
                int g = (int) (((color >> 8) & 0xFF) * shadeG);
                int b = (int) ((color & 0xFF) * shadeB);
                int a = (color >> 24) & 0xFF;
                int finalColor = (a << 24) | (r << 16) | (g << 8) | b;

                // Remap UV coordinates to new sprite
                float oldU = Float.intBitsToFloat(vertexData[idx + 4]);
                float oldV = Float.intBitsToFloat(vertexData[idx + 5]);

                TextureAtlasSprite oldSprite = quad.getSprite();
                float relativeU = (oldU - oldSprite.getU0()) / (oldSprite.getU1() - oldSprite.getU0());
                float relativeV = (oldV - oldSprite.getV0()) / (oldSprite.getV1() - oldSprite.getV0());

                float newU = sprite.getU0() + relativeU * (sprite.getU1() - sprite.getU0());
                float newV = sprite.getV0() + relativeV * (sprite.getV1() - sprite.getV0());

                // Extract normal
                int packedNormal = vertexData[idx + 7];
                byte nx = (byte) (packedNormal & 0xFF);
                byte ny = (byte) ((packedNormal >> 8) & 0xFF);
                byte nz = (byte) ((packedNormal >> 16) & 0xFF);

                consumer.vertex(pose.pose(), x, y, z)
                        .color(finalColor)
                        .uv(newU, newV)
                        .overlayCoords(packedOverlay)
                        .uv2(combinedLight)
                        .normal(pose.normal(), nx / 127.0f, ny / 127.0f, nz / 127.0f)
                        .endVertex();
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(CopyBlockEntity blockEntity) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}