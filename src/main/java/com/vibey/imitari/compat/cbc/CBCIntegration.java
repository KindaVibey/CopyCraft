package com.vibey.imitari.compat.cbc;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Safe wrapper for CBC integration that has NO direct CBC dependencies.
 * All CBC-specific code is in CBCIntegrationImpl and loaded via reflection.
 *
 * This allows Imitari to work without Create Big Cannons installed.
 */
public class CBCIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean CBC_LOADED = false;
    private static boolean CHECKED = false;
    private static boolean INTEGRATION_FAILED = false;

    /**
     * Check if Create Big Cannons is available.
     * Safe to call from anywhere.
     *
     * @return true if CBC is loaded and integration is working
     */
    public static boolean isAvailable() {
        if (!CHECKED) {
            checkCBC();
        }
        return CBC_LOADED && !INTEGRATION_FAILED;
    }

    private static void checkCBC() {
        if (CHECKED) return;

        CBC_LOADED = ModList.get().isLoaded("createbigcannons");
        CHECKED = true;

        if (CBC_LOADED) {
            LOGGER.info("Create Big Cannons detected - enabling block armor integration");
        }
    }

    /**
     * Register all CopyBlocks with CBC's block armor system.
     * Called during mod initialization.
     */
    public static void register() {
        if (!isAvailable()) {
            return;
        }

        try {
            Class<?> implClass = Class.forName("com.vibey.imitari.compat.cbc.CBCIntegrationImpl");
            var registerMethod = implClass.getMethod("register");
            registerMethod.invoke(null);
            LOGGER.info("CBC block armor integration registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register CBC integration", e);
            INTEGRATION_FAILED = true;
        }
    }
}