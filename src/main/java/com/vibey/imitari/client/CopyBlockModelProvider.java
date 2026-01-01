package com.vibey.imitari.client;

import com.google.common.collect.Lists;
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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Centralized model registration system for ICopyBlock implementations.
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
        int wrappedCount = 0;

        for (Map.Entry<ResourceLocation, BakedModel> entry : modelRegistry.entrySet()) {
            ResourceLocation modelId = entry.getKey();
            BakedModel existingModel = entry.getValue();

            // Skip if already wrapped
            if (existingModel instanceof CopyBlockModel || existingModel instanceof CopyBlockMultiPartModel) {
                continue;
            }

            String path = modelId.getPath();
            boolean shouldWrap = false;

            // Check if this is an Imitari copy_block model
            if (modelId.getNamespace().equals("imitari") && path.contains("copy_block")) {
                shouldWrap = true;
            }

            // Check registered blocks for addon compatibility
            if (!shouldWrap) {
                for (ResourceLocation blockId : REGISTERED_BLOCKS) {
                    if (!modelId.getNamespace().equals(blockId.getNamespace())) {
                        continue;
                    }

                    String blockName = blockId.getPath();

                    if (path.equals("block/" + blockName) ||
                            path.equals(blockName) ||
                            path.startsWith("block/" + blockName + "_")) {
                        shouldWrap = true;
                        break;
                    }
                }
            }

            if (shouldWrap) {
                BakedModel wrappedModel;

                // Special handling for MultiPartBakedModel
                if (existingModel instanceof MultiPartBakedModel multiPartModel) {
                    List<Pair<Predicate<BlockState>, BakedModel>> selectors = extractSelectors(multiPartModel);

                    // Wrap each sub-model in the multipart
                    List<Pair<Predicate<BlockState>, BakedModel>> wrappedSelectors = Lists.newArrayList();
                    for (Pair<Predicate<BlockState>, BakedModel> pair : selectors) {
                        BakedModel wrappedSubModel = new CopyBlockModel(pair.getRight());
                        wrappedSelectors.add(Pair.of(pair.getLeft(), wrappedSubModel));
                    }

                    wrappedModel = new CopyBlockMultiPartModel(multiPartModel, wrappedSelectors);
                } else {
                    // Regular single model wrapping
                    wrappedModel = new CopyBlockModel(existingModel);
                }

                modelRegistry.put(modelId, wrappedModel);
                wrappedCount++;
            }
        }

        System.out.println("[Imitari] Wrapped " + wrappedCount + " CopyBlock models");
    }

    /**
     * Extract the selectors from a MultiPartBakedModel using reflection.
     */
    @SuppressWarnings("unchecked")
    private static List<Pair<Predicate<BlockState>, BakedModel>> extractSelectors(MultiPartBakedModel model) {
        try {
            // Access private field "selectors" in MultiPartBakedModel
            Field selectorsField = MultiPartBakedModel.class.getDeclaredField("selectors");
            selectorsField.setAccessible(true);
            return (List<Pair<Predicate<BlockState>, BakedModel>>) selectorsField.get(model);
        } catch (Exception e) {
            e.printStackTrace();
            return Lists.newArrayList();
        }
    }

    public static Set<ResourceLocation> getRegisteredBlocks() {
        return new HashSet<>(REGISTERED_BLOCKS);
    }

    public static void clearRegistrations() {
        REGISTERED_BLOCKS.clear();
    }
}