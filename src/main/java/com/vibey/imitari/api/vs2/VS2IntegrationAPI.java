package com.vibey.imitari.api.vs2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * API for Valkyrien Skies 2 integration.
 *
 * <p>This API is safe to call even when VS2 is not installed.
 * All methods will no-op gracefully if VS2 is not present.</p>
 *
 * <p><b>For Addon Developers:</b></p>
 * <p>If you create custom CopyBlock variants that need to notify VS2 of mass changes,
 * use these methods instead of calling VS2 directly.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * // When your block's properties change dynamically:
 * VS2IntegrationAPI.notifyMassChange(level, pos, oldMass, newMass);
 * }</pre>
 */
public class VS2IntegrationAPI {

    /**
     * Check if Valkyrien Skies 2 is loaded and available.
     *
     * @return true if VS2 is present and functional
     */
    public static boolean isVS2Available() {
        return com.vibey.imitari.vs2.VS2CopyBlockIntegration.isAvailable();
    }

    /**
     * Notify VS2 that a CopyBlock's mass has changed.
     * This is called automatically when using ICopyBlock's standard methods,
     * but can be called manually for custom implementations.
     *
     * <p>Safe to call even if VS2 is not installed (will no-op).</p>
     *
     * @param level The level/world
     * @param pos The block position
     * @param copyBlockState The CopyBlock's state
     * @param oldCopiedBlock The previous copied block (for calculating old mass)
     */
    public static void notifyMassChange(Level level, BlockPos pos,
                                        BlockState copyBlockState,
                                        BlockState oldCopiedBlock) {
        if (!isVS2Available()) return;

        com.vibey.imitari.vs2.VS2CopyBlockIntegration.updateCopyBlockMass(
                level, pos, copyBlockState, oldCopiedBlock
        );
    }

    /**
     * Notify VS2 that a BlockEntity has loaded with copied block data.
     * This is critical for ship assembly - VS2 needs to know the correct mass
     * after NBT data is loaded.
     *
     * <p>This is called automatically by CopyBlockEntity, but custom implementations
     * should call this in their load() method.</p>
     *
     * @param level The level/world
     * @param pos The block position
     * @param state The block state
     * @param copiedBlock The copied block from NBT
     */
    public static void notifyBlockEntityLoaded(Level level, BlockPos pos,
                                               BlockState state,
                                               BlockState copiedBlock) {
        if (!isVS2Available()) return;

        com.vibey.imitari.vs2.VS2CopyBlockIntegration.onBlockEntityDataLoaded(
                level, pos, state, copiedBlock
        );
    }

    /**
     * Register a custom CopyBlock with VS2's mass calculation system.
     * This is called automatically during mod initialization for all registered blocks.
     *
     * <p>Only needed if you're dynamically registering blocks at runtime.</p>
     */
    public static void registerWithVS2() {
        if (!isVS2Available()) return;

        com.vibey.imitari.vs2.VS2CopyBlockIntegration.register();
    }

    /**
     * Get the VS2 version string, if available.
     *
     * @return VS2 version string, or null if not installed
     */
    public static String getVS2Version() {
        if (!isVS2Available()) return null;

        try {
            // Query VS2's version
            return net.minecraftforge.fml.ModList.get()
                    .getModContainerById("valkyrienskies")
                    .map(mc -> mc.getModInfo().getVersion().toString())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}