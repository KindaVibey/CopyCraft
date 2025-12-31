package com.vibey.imitari.client;

import com.vibey.imitari.api.ICopyBlock;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Centralized model registration system for ICopyBlock implementations.
 * Addon mods can register their own blocks by calling registerBlock().
 */
public class CopyBlockModelProvider {

    private static final Set<ResourceLocation> REGISTERED_BLOCKS = new HashSet<>();

    public static void registerBlock(ResourceLocation blockId) {
        REGISTERED_BLOCKS.add(blockId);
    }

    public static void registerBlock(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id != null) {
            registerBlock(id);
        }
    }

    public static void autoRegisterForMod(String modId) {
        for (Map.Entry<net.minecraft.resources.ResourceKey<Block>, Block> entry : ForgeRegistries.BLOCKS.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            Block block = entry.getValue();

            if (id.getNamespace().equals(modId) && block instanceof ICopyBlock copyBlock && copyBlock.useDynamicModel()) {
                registerBlock(id);
            }
        }
    }

    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();

        for (Map.Entry<ResourceLocation, BakedModel> entry : modelRegistry.entrySet()) {
            ResourceLocation modelId = entry.getKey();
            BakedModel existingModel = entry.getValue();

            // Skip if already wrapped
            if (existingModel instanceof CopyBlockModel) {
                continue;
            }

            // Check if this model should be wrapped
            boolean shouldWrap = false;
            String path = modelId.getPath();

            // Simple pattern matching for Imitari CopyBlocks
            if (modelId.getNamespace().equals("imitari") &&
                (path.startsWith("block/copy_block_") ||
                 path.equals("item/copy_block"))) {
                shouldWrap = true;
            }

            // Also check registered blocks for addon compatibility
            if (!shouldWrap) {
                for (ResourceLocation blockId : REGISTERED_BLOCKS) {
                    if (!modelId.getNamespace().equals(blockId.getNamespace())) {
                        continue;
                    }

                    String blockName = blockId.getPath();

                    // Match block models and their variants/sub-models
                    if (path.equals("block/" + blockName) ||
                            path.equals(blockName) ||
                            path.startsWith("block/" + blockName + "_")) {
                        shouldWrap = true;
                        break;
                    }
                }
            }

            if (shouldWrap) {
                CopyBlockModel wrappedModel = new CopyBlockModel(existingModel);
                modelRegistry.put(modelId, wrappedModel);
            }
        }
    }

    public static Set<ResourceLocation> getRegisteredBlocks() {
        return new HashSet<>(REGISTERED_BLOCKS);
    }

    public static void clearRegistrations() {
        REGISTERED_BLOCKS.clear();
    }
}
