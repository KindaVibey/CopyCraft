package com.vibey.copycraft.client;

import com.vibey.copycraft.CopyCraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;

public class ModelRegistrationHandler {

    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        System.out.println("======== MODEL BAKING EVENT ========");

        ResourceLocation[] blockIds = {
                new ResourceLocation(CopyCraft.MODID, "copy_block"),
                new ResourceLocation(CopyCraft.MODID, "copy_block_full"),
                new ResourceLocation(CopyCraft.MODID, "copy_block_slab"),
                new ResourceLocation(CopyCraft.MODID, "copy_block_stairs")
        };

        for (ResourceLocation blockId : blockIds) {
            ModelResourceLocation[] locations = {
                    new ModelResourceLocation(blockId, ""),
                    new ModelResourceLocation(blockId, "inventory")
            };

            boolean foundAny = false;
            for (ModelResourceLocation loc : locations) {
                BakedModel existingModel = event.getModels().get(loc);
                if (existingModel != null) {
                    event.getModels().put(loc, new CopyBlockModel(existingModel));
                    System.out.println("Replaced with CopyBlockModel: " + loc);
                    foundAny = true;
                } else {
                    System.out.println("No model at: " + loc);
                }
            }

            if (!foundAny) {
                System.out.println("WARNING: Could not find model for: " + blockId);
            }
        }
    }
}