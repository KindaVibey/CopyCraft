package com.vibey.copycraft.client;

import com.vibey.copycraft.CopyCraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;

import java.util.Map;

public class ModelRegistrationHandler {

    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        System.out.println("======== MODEL BAKING EVENT ========");

        // Forge 1.20+: models are stored under ResourceLocation, NOT ModelResourceLocation
        Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();

        int replacedCount = 0;

        for (Map.Entry<ResourceLocation, BakedModel> entry : modelRegistry.entrySet()) {
            ResourceLocation id = entry.getKey();

            // Check if this model belongs to our mod
            if (id.getNamespace().equals(CopyCraft.MODID)) {
                String path = id.getPath();

                if (path.equals("copy_block") ||
                        path.equals("copy_block_full") ||
                        path.equals("copy_block_slab") ||
                        path.equals("copy_block_slab_top") ||
                        path.equals("copy_block_stairs")) {

                    BakedModel existingModel = entry.getValue();
                    CopyBlockModel wrappedModel = new CopyBlockModel(existingModel);

                    // Replace the model
                    modelRegistry.put(id, wrappedModel);

                    System.out.println("✓ Wrapped model: " + id);
                    replacedCount++;
                }
            }
        }

        System.out.println("======== WRAPPED " + replacedCount + " MODELS ========");

        if (replacedCount == 0) {
            System.err.println("ERROR: NO MODELS WERE WRAPPED! Check your blockstate JSONs!");
        }
    }
}
