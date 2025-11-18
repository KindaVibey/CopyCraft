package com.vibey.copycraft.vs2;

import com.vibey.copycraft.CopyCraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Periodically cleans the VS weight cache to prevent memory leaks
 */
@Mod.EventBusSubscriber(modid = CopyCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VSCacheCleaner {
    private static int tickCounter = 0;
    private static final int CLEAN_INTERVAL = 200; // Clean every 10 seconds (20 ticks/sec)

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter >= CLEAN_INTERVAL) {
                tickCounter = 0;
                try {
                    CopyCraftWeights.cleanCache();
                } catch (NoClassDefFoundError e) {
                    // VS not installed, ignore
                }
            }
        }
    }
}