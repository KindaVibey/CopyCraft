package com.vibey.copycraft.vs2;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

public class VS2Integration {
    private static Boolean vs2Loaded = null;

    public static boolean isVS2Loaded() {
        if (vs2Loaded == null) {
            vs2Loaded = ModList.get().isLoaded("valkyrienskies");
        }
        return vs2Loaded;
    }

    public static void init() {
        if (isVS2Loaded()) {
            System.out.println("[CopyCraft] VS2 detected! Mass integration active.");
        }
    }

    /**
     * Query VS2's ACTUAL mass from datapacks
     */
    public static double queryBlockMass(BlockState state) {
        if (!isVS2Loaded()) return -1.0;

        try {
            // Use reflection to query VS2's datapack registry
            Class<?> vsGameUtils = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
            java.lang.reflect.Method getBlockMass = vsGameUtils.getMethod("getBlockMass", BlockState.class);

            Object massData = getBlockMass.invoke(null, state);
            if (massData != null) {
                java.lang.reflect.Method getMass = massData.getClass().getMethod("getMass");
                Double mass = (Double) getMass.invoke(massData);

                if (mass != null && mass > 0) {
                    return mass;
                }
            }
        } catch (Exception e) {
            // VS2 API not available
        }

        return -1.0; // Not found
    }
}
