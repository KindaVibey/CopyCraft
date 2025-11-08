package com.vibey.copycraft.client;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.registry.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CopyCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.COPY_BLOCK.get(), CopyCraftRenderer::new);
        //event.registerBlockEntityRenderer(ModBlockEntities.COPY_SLAB.get(), CopyCraftRenderer::new);
        // Add more as you create them!
    }
}
