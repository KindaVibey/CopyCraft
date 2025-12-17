package com.vibey.imitari.api.registration;

import com.vibey.imitari.api.CopyBlockAPI;
import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.client.CopyBlockModelProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Helper class for registering CopyBlocks.
 *
 * <p><b>For Addon Developers:</b></p>
 * <p>Use this in your mod's setup events to register your CopyBlocks.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * @Mod.EventBusSubscriber(modid = "mymod", bus = Mod.EventBusSubscriber.Bus.MOD)
 * public class MyModSetup {
 *
 *     @SubscribeEvent
 *     public static void commonSetup(FMLCommonSetupEvent event) {
 *         event.enqueueWork(() -> {
 *             // Register your blocks
 *             CopyBlockRegistration.registerForMod("mymod");
 *         });
 *     }
 * }
 * }</pre>
 */
public class CopyBlockRegistration {

    /**
     * Register all ICopyBlock implementations from a mod.
     * This should be called in FMLCommonSetupEvent with enqueueWork().
     *
     * <p>This will:</p>
     * <ul>
     *   <li>Register blocks with the CopyBlock API</li>
     *   <li>Register blocks with the model system</li>
     *   <li>Enable all CopyBlock features for your blocks</li>
     * </ul>
     *
     * @param modId Your mod's ID
     * @return The number of blocks registered
     */
    public static int registerForMod(String modId) {
        // Register with API
        int count = CopyBlockAPI.autoRegisterModBlocks(modId);

        // Register with model system
        CopyBlockModelProvider.autoRegisterForMod(modId);

        return count;
    }

    /**
     * Register a single CopyBlock.
     * Use this if you only want to register specific blocks.
     *
     * @param blockId The block's ResourceLocation
     */
    public static void registerBlock(ResourceLocation blockId) {
        CopyBlockAPI.registerCopyBlock(blockId);
        CopyBlockModelProvider.registerBlock(blockId);
    }

    /**
     * Register a single CopyBlock from a Block instance.
     *
     * @param block The block to register
     */
    public static void registerBlock(Block block) {
        CopyBlockAPI.registerCopyBlock(block);
        CopyBlockModelProvider.registerBlock(block);
    }

    /**
     * Register multiple blocks at once.
     *
     * @param blocks Array of blocks to register
     * @return The number of blocks registered
     */
    public static int registerBlocks(Block... blocks) {
        int count = 0;
        for (Block block : blocks) {
            if (block instanceof ICopyBlock) {
                registerBlock(block);
                count++;
            }
        }
        return count;
    }

    /**
     * Register multiple blocks by ResourceLocation.
     *
     * @param blockIds Array of block IDs to register
     * @return The number of blocks registered
     */
    public static int registerBlocks(ResourceLocation... blockIds) {
        for (ResourceLocation id : blockIds) {
            registerBlock(id);
        }
        return blockIds.length;
    }

    /**
     * Internal use - called by Imitari during mod initialization.
     * Addons should not call this directly.
     */
    @ApiStatus.Internal
    public static void initializeImitari() {
        registerForMod("imitari");
    }
}