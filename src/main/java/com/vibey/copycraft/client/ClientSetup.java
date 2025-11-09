package com.vibey.copycraft.client;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.client.model.CopyModelLoader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CopyCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    /**
     * Register our custom model loader
     * This tells Forge how to load our copy block models
     */
    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(CopyModelLoader.ID.toString(), new CopyModelLoader());
        System.out.println("[CopyCraft] Registered custom model loader");
    }
}