package com.vibey.imitari.client;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.vibey.imitari.api.ICopyBlock;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Predicate;

/**
 * Centralized model registration system for ICopyBlock implementations.
 */
public class CopyBlockModelProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
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
        LOGGER.info("[Imitari] Starting model wrapping...");

        Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();
        int wrappedCount = 0;

        // Create a list of models to wrap (to avoid ConcurrentModificationException)
        List<Map.Entry<ResourceLocation, BakedModel>> toWrap = new ArrayList<>();

        for (Map.Entry<ResourceLocation, BakedModel> entry : modelRegistry.entrySet()) {
            ResourceLocation modelId = entry.getKey();
            BakedModel existingModel = entry.getValue();

            // Skip if already wrapped
            if (existingModel instanceof CopyBlockModel || existingModel instanceof CopyBlockMultiPartModel) {
                continue;
            }

            String path = modelId.getPath();
            String namespace = modelId.getNamespace();
            boolean shouldWrap = false;

            // IMITARI MODELS: Check if this is an Imitari copy_block model
            // Match: imitari:block/copy_block, imitari:copy_block_slab, etc.
            if (namespace.equals("imitari")) {
                // Match both "block/copy_block" and just "copy_block"
                String blockName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;

                if (blockName.startsWith("copy_block")) {
                    // Exclude item models
                    if (!path.contains("item/") && !path.endsWith("_inventory")) {
                        shouldWrap = true;
                        LOGGER.debug("[Imitari] Found Imitari model: {}", modelId);
                    }
                }
            }

            // ADDON MODELS: Check registered blocks for addon compatibility
            if (!shouldWrap && !REGISTERED_BLOCKS.isEmpty()) {
                for (ResourceLocation blockId : REGISTERED_BLOCKS) {
                    if (!namespace.equals(blockId.getNamespace())) {
                        continue;
                    }

                    String blockName = blockId.getPath();

                    // Extract just the model name (after last /)
                    String modelName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;

                    // Match: modname:block/custom_copy_block or modname:custom_copy_block_variant
                    if (modelName.equals(blockName) || modelName.startsWith(blockName + "_")) {
                        // Exclude item models
                        if (!path.contains("item/") && !path.endsWith("_inventory")) {
                            shouldWrap = true;
                            LOGGER.debug("[Imitari] Found registered block model: {}", modelId);
                            break;
                        }
                    }
                }
            }

            if (shouldWrap) {
                toWrap.add(entry);
            }
        }

        LOGGER.info("[Imitari] Found {} models to wrap", toWrap.size());

        // Now wrap the models
        for (Map.Entry<ResourceLocation, BakedModel> entry : toWrap) {
            ResourceLocation modelId = entry.getKey();
            BakedModel existingModel = entry.getValue();

            try {
                BakedModel wrappedModel;

                // Special handling for MultiPartBakedModel
                if (existingModel instanceof MultiPartBakedModel multiPartModel) {
                    List<Pair<Predicate<BlockState>, BakedModel>> selectors = extractSelectors(multiPartModel);

                    if (selectors != null && !selectors.isEmpty()) {
                        // Wrap each sub-model in the multipart
                        List<Pair<Predicate<BlockState>, BakedModel>> wrappedSelectors = Lists.newArrayList();
                        for (Pair<Predicate<BlockState>, BakedModel> pair : selectors) {
                            BakedModel wrappedSubModel = new CopyBlockModel(pair.getRight());
                            wrappedSelectors.add(Pair.of(pair.getLeft(), wrappedSubModel));
                        }

                        wrappedModel = new CopyBlockMultiPartModel(multiPartModel, wrappedSelectors);
                        LOGGER.debug("[Imitari] Wrapped multipart model: {}", modelId);
                    } else {
                        // Couldn't extract selectors, wrap the whole thing
                        wrappedModel = new CopyBlockModel(existingModel);
                        LOGGER.debug("[Imitari] Wrapped as single model (multipart extraction failed): {}", modelId);
                    }
                } else {
                    // Regular single model wrapping
                    wrappedModel = new CopyBlockModel(existingModel);
                    LOGGER.debug("[Imitari] Wrapped regular model: {}", modelId);
                }

                modelRegistry.put(modelId, wrappedModel);
                wrappedCount++;

            } catch (Exception e) {
                LOGGER.error("[Imitari] Failed to wrap model: " + modelId, e);
                // Don't wrap if there's an error - leave the original model
            }
        }

        LOGGER.info("[Imitari] Successfully wrapped {} CopyBlock models", wrappedCount);
    }

    /**
     * Extract the selectors from a MultiPartBakedModel using reflection.
     * Returns null if extraction fails (instead of crashing).
     */
    @SuppressWarnings("unchecked")
    private static List<Pair<Predicate<BlockState>, BakedModel>> extractSelectors(MultiPartBakedModel model) {
        try {
            // Try multiple possible field names (obfuscated vs unobfuscated)
            Field selectorsField = null;

            // Try unobfuscated name first
            try {
                selectorsField = MultiPartBakedModel.class.getDeclaredField("selectors");
            } catch (NoSuchFieldException e) {
                // Try to find any field that looks like selectors
                for (Field field : MultiPartBakedModel.class.getDeclaredFields()) {
                    // Check if it's a List type
                    if (List.class.isAssignableFrom(field.getType())) {
                        selectorsField = field;
                        break;
                    }
                }
            }

            if (selectorsField == null) {
                LOGGER.warn("[Imitari] Could not find selectors field in MultiPartBakedModel");
                return null;
            }

            selectorsField.setAccessible(true);
            Object result = selectorsField.get(model);

            if (result instanceof List) {
                return (List<Pair<Predicate<BlockState>, BakedModel>>) result;
            }

            return null;

        } catch (Exception e) {
            LOGGER.warn("[Imitari] Failed to extract selectors from MultiPartBakedModel: " + e.getMessage());
            return null;
        }
    }

    public static Set<ResourceLocation> getRegisteredBlocks() {
        return new HashSet<>(REGISTERED_BLOCKS);
    }

    public static void clearRegistrations() {
        REGISTERED_BLOCKS.clear();
    }
}