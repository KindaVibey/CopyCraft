package com.vibey.imitari.client;

import com.vibey.imitari.block.ICopyBlock;
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

    /**
     * Register a block to use the CopyBlock model system.
     * Call this during mod initialization (before model baking).
     *
     * @param blockId The ResourceLocation of the block to register
     */
    public static void registerBlock(ResourceLocation blockId) {
        REGISTERED_BLOCKS.add(blockId);
        System.out.println("[Imitari] Registered CopyBlock model for: " + blockId);
    }

    /**
     * Register a block to use the CopyBlock model system.
     *
     * @param block The block instance to register
     */
    public static void registerBlock(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id != null) {
            registerBlock(id);
        }
    }

    /**
     * Auto-register all blocks implementing ICopyBlock from a specific namespace.
     *
     * @param modId The mod ID to scan for ICopyBlock implementations
     */
    public static void autoRegisterForMod(String modId) {
        System.out.println("[Imitari] Scanning for ICopyBlock implementations in mod: " + modId);
        int count = 0;
        for (Map.Entry<net.minecraft.resources.ResourceKey<Block>, Block> entry : ForgeRegistries.BLOCKS.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            Block block = entry.getValue();

            if (id.getNamespace().equals(modId)) {
                System.out.println("  Found block in " + modId + ": " + id + " (" + block.getClass().getSimpleName() + ")");

                if (block instanceof ICopyBlock copyBlock) {
                    System.out.println("    -> Implements ICopyBlock");
                    if (copyBlock.useDynamicModel()) {
                        registerBlock(id);
                        count++;
                        System.out.println("    -> Registered for dynamic model");
                    } else {
                        System.out.println("    -> useDynamicModel() returned false, skipping");
                    }
                } else {
                    System.out.println("    -> Does NOT implement ICopyBlock");
                }
            }
        }
        System.out.println("[Imitari] Auto-registered " + count + " CopyBlock models for mod: " + modId);
    }

    /**
     * Internal method called during model baking.
     * Wraps all registered blocks with the CopyBlockModel.
     */
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        System.out.println("======== IMITARI MODEL BAKING EVENT ========");

        Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();
        int replacedCount = 0;

        // Iterate through ALL models in the registry
        for (Map.Entry<ResourceLocation, BakedModel> entry : modelRegistry.entrySet()) {
            ResourceLocation id = entry.getKey();

            // Check if this model belongs to one of our registered blocks
            for (ResourceLocation blockId : REGISTERED_BLOCKS) {
                // Only check models from the same namespace
                if (!id.getNamespace().equals(blockId.getNamespace())) {
                    continue;
                }

                String path = id.getPath();
                String blockName = blockId.getPath();

                // Original logic: check if path exactly matches block name OR is a variant
                if (path.equals(blockName) ||
                        path.equals(blockName + "_top") ||
                        path.equals(blockName + "_slab") ||
                        path.equals(blockName + "_stairs")) {

                    BakedModel existingModel = entry.getValue();
                    CopyBlockModel wrappedModel = new CopyBlockModel(existingModel);

                    // Replace the model in the registry
                    modelRegistry.put(id, wrappedModel);

                    System.out.println("✓ Wrapped model: " + id);
                    replacedCount++;
                }
            }
        }

        System.out.println("======== WRAPPED " + replacedCount + " MODELS ========");

        if (replacedCount == 0 && !REGISTERED_BLOCKS.isEmpty()) {
            System.err.println("ERROR: NO MODELS WERE WRAPPED! Check your blockstate JSONs!");
        }
    }

    /**
     * Get all registered block IDs (for debugging).
     */
    public static Set<ResourceLocation> getRegisteredBlocks() {
        return new HashSet<>(REGISTERED_BLOCKS);
    }

    /**
     * Clear all registrations (for testing/reloading).
     */
    public static void clearRegistrations() {
        REGISTERED_BLOCKS.clear();
    }
}