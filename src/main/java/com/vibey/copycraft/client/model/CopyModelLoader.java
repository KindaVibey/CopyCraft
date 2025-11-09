package com.vibey.copycraft.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.vibey.copycraft.CopyCraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

/**
 * Custom model loader for copy blocks
 * Registers with Forge's model loading system
 */
public class CopyModelLoader implements IGeometryLoader<CopyModelLoader.CopyGeometry> {

    // Just the path, not the full resource location - Forge adds the mod ID automatically
    public static final String ID = "copy_block";

    @Override
    public CopyGeometry read(JsonObject jsonObject, JsonDeserializationContext context) {
        // Parse the JSON if needed - for now we don't need any special configuration
        return new CopyGeometry();
    }

    /**
     * The unbaked geometry - holds the model before it's baked
     */
    public static class CopyGeometry implements IUnbakedGeometry<CopyGeometry> {

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                               Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, ItemOverrides overrides,
                               ResourceLocation modelLocation) {

            // Get the base model (the stone texture) as a fallback
            ResourceLocation baseModelLocation = new ResourceLocation("minecraft", "block/stone");
            UnbakedModel baseUnbaked = baker.getModel(baseModelLocation);
            BakedModel baseModel = baseUnbaked.bake(baker, spriteGetter, modelState, baseModelLocation);

            // Return our custom baked model
            return new CopyBakedModel(baseModel);
        }
    }
}