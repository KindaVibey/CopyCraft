package com.vibey.copycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vibey.copycraft.core.CopyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class CopyCraftRenderer implements BlockEntityRenderer<CopyBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public CopyCraftRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CopyBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int combinedLight, int combinedOverlay) {

        if (!blockEntity.hasMaterial()) {
            return;
        }

        BlockState material = blockEntity.getMaterial();
        BakedModel model = blockRenderer.getBlockModel(material);

        poseStack.pushPose();

        // Render the material
        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.cutout()),
                material,
                model,
                1.0f, 1.0f, 1.0f,
                combinedLight,
                combinedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );

        poseStack.popPose();
    }
}
