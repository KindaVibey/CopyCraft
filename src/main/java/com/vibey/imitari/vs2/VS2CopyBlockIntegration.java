// This is the SAFE wrapper that has NO VS2 dependencies
// File: src/main/java/com/vibey/imitari/vs2/VS2CopyBlockIntegration.java

package com.vibey.imitari.vs2;

import com.vibey.imitari.Imitari;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

public class VS2CopyBlockIntegration {
    private static boolean VS2_LOADED = false;
    private static boolean CHECKED = false;

    private static boolean isVS2Loaded() {
        if (!CHECKED) {
            VS2_LOADED = ModList.get().isLoaded("valkyrienskies");
            CHECKED = true;
            if (VS2_LOADED) {
                System.out.println("[Imitari] Valkyrienskies detected! Enabling CopyBlock physics integration.");
            } else {
                System.out.println("[Imitari] Valkyrienskies not detected. CopyBlock will work without ship physics.");
            }
        }
        return VS2_LOADED;
    }

    public static void register() {
        if (!isVS2Loaded()) {
            return;
        }

        try {
            // Load the actual implementation class ONLY if VS2 is present
            Class<?> implClass = Class.forName("com.vibey.imitari.vs2.VS2CopyBlockIntegrationImpl");
            var registerMethod = implClass.getMethod("register");
            registerMethod.invoke(null);
            System.out.println("[Imitari] Successfully registered VS2 CopyBlock integration!");
        } catch (Exception e) {
            System.err.println("[Imitari] Failed to register VS2 integration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void updateCopyBlockMass(Level level, BlockPos pos, BlockState copyBlockState, BlockState oldCopiedBlock) {
        if (!isVS2Loaded()) {
            return;
        }

        try {
            Class<?> implClass = Class.forName("com.vibey.imitari.vs2.VS2CopyBlockIntegrationImpl");
            var method = implClass.getMethod("updateCopyBlockMass", Level.class, BlockPos.class, BlockState.class, BlockState.class);
            method.invoke(null, level, pos, copyBlockState, oldCopiedBlock);
        } catch (Exception e) {
            System.err.println("[Imitari] Failed to update VS2 copy block mass: " + e.getMessage());
        }
    }

    public static void onBlockEntityDataLoaded(Level level, BlockPos pos, BlockState state, BlockState copiedBlock) {
        if (!isVS2Loaded()) {
            return;
        }

        try {
            Class<?> implClass = Class.forName("com.vibey.imitari.vs2.VS2CopyBlockIntegrationImpl");
            var method = implClass.getMethod("onBlockEntityDataLoaded", Level.class, BlockPos.class, BlockState.class, BlockState.class);
            method.invoke(null, level, pos, state, copiedBlock);
        } catch (Exception e) {
            System.err.println("[Imitari] Failed to notify VS2 of block entity data load: " + e.getMessage());
        }
    }
}