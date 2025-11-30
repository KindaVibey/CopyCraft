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
        int count = 0;
        for (Map.Entry<net.minecraft.resources.ResourceKey<Block>, Block> entry : ForgeRegistries.BLOCKS.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            Block block = entry.getValue();

            if (id.getNamespace().equals(modId) && block instanceof ICopyBlock copyBlock) {
                if (copyBlock.useDynamicModel()) {
                    registerBlock(id);
                    count++;
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

        for (ResourceLocation blockId : REGISTERED_BLOCKS) {
            // Try to find the model - might be just the block ID or with variants
            BakedModel existingModel = modelRegistry.get(blockId);

            if (existingModel != null) {
                CopyBlockModel wrappedModel = new CopyBlockModel(existingModel);
                modelRegistry.put(blockId, wrappedModel);
                System.out.println("✓ Wrapped model: " + blockId);
                replacedCount++;
            } else {
                System.out.println("⚠ Model not found for registered block: " + blockId);
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