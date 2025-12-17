package com.vibey.imitari.api;

import com.vibey.imitari.api.blockentity.ICopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Main API entry point for Imitari.
 *
 * <p><b>For Addon Developers - Quick Registration:</b></p>
 * <pre>{@code
 * // In your mod's common setup or FMLCommonSetupEvent:
 * public void commonSetup(FMLCommonSetupEvent event) {
 *     event.enqueueWork(() -> {
 *         // Register a single block
 *         CopyBlockAPI.registerCopyBlock(new ResourceLocation("mymod", "my_copy_block"));
 *
 *         // Or register all blocks from your mod automatically
 *         CopyBlockAPI.autoRegisterModBlocks("mymod");
 *
 *         // Or register from a Block instance
 *         CopyBlockAPI.registerCopyBlock(MY_BLOCK.get());
 *     });
 * }
 * }</pre>
 *
 * <p><b>What Registration Does:</b></p>
 * <ul>
 *   <li>Enables dynamic model/texture system for your block</li>
 *   <li>Enables dynamic tag inheritance</li>
 *   <li>Enables dynamic physics calculations</li>
 *   <li>Registers with Valkyrien Skies 2 (if present)</li>
 * </ul>
 *
 * <p><b>Query API:</b></p>
 * Use the static methods to check copied block data from anywhere in your code:
 * <pre>{@code
 * BlockState copied = CopyBlockAPI.getCopiedBlock(level, pos);
 * if (copied != null && !copied.isAir()) {
 *     // Do something with the copied block
 * }
 * }</pre>
 */
public class CopyBlockAPI {

    private static final Set<ResourceLocation> REGISTERED_BLOCKS = new HashSet<>();

    // ==================== REGISTRATION API ====================

    /**
     * Register a CopyBlock by ResourceLocation.
     * Call this during mod initialization (FMLCommonSetupEvent).
     *
     * <p>Example:</p>
     * <pre>{@code
     * CopyBlockAPI.registerCopyBlock(new ResourceLocation("mymod", "custom_copy_block"));
     * }</pre>
     *
     * @param blockId The ResourceLocation of the block to register
     */
    public static void registerCopyBlock(ResourceLocation blockId) {
        REGISTERED_BLOCKS.add(blockId);
    }

    /**
     * Register a CopyBlock from a Block instance.
     *
     * <p>Example:</p>
     * <pre>{@code
     * CopyBlockAPI.registerCopyBlock(ModBlocks.MY_COPY_BLOCK.get());
     * }</pre>
     *
     * @param block The block instance to register
     */
    public static void registerCopyBlock(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id != null) {
            registerCopyBlock(id);
        }
    }

    /**
     * Automatically register all ICopyBlock implementations from a mod.
     * Only registers blocks that implement ICopyBlock and have useDynamicModel() = true.
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Registers all ICopyBlock blocks from "mymod"
     * CopyBlockAPI.autoRegisterModBlocks("mymod");
     * }</pre>
     *
     * @param modId The mod ID to scan for CopyBlocks
     * @return The number of blocks registered
     */
    public static int autoRegisterModBlocks(String modId) {
        int count = 0;
        for (var entry : ForgeRegistries.BLOCKS.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            Block block = entry.getValue();

            if (id.getNamespace().equals(modId) &&
                    block instanceof ICopyBlock copyBlock &&
                    copyBlock.useDynamicModel()) {
                registerCopyBlock(id);
                count++;
            }
        }
        return count;
    }

    /**
     * Unregister a CopyBlock.
     * Useful for runtime configuration changes.
     *
     * @param blockId The block to unregister
     * @return true if the block was registered and is now removed
     */
    public static boolean unregisterCopyBlock(ResourceLocation blockId) {
        return REGISTERED_BLOCKS.remove(blockId);
    }

    /**
     * Check if a block is registered as a CopyBlock.
     *
     * @param blockId The block ID to check
     * @return true if registered
     */
    public static boolean isRegistered(ResourceLocation blockId) {
        return REGISTERED_BLOCKS.contains(blockId);
    }

    /**
     * Check if a block is registered as a CopyBlock.
     *
     * @param block The block to check
     * @return true if registered
     */
    public static boolean isRegistered(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null && isRegistered(id);
    }

    /**
     * Get all registered CopyBlock IDs.
     * Returns a copy to prevent external modification.
     *
     * @return Set of all registered block IDs
     */
    public static Set<ResourceLocation> getRegisteredBlocks() {
        return new HashSet<>(REGISTERED_BLOCKS);
    }

    /**
     * Clear all registrations.
     * Primarily for testing and development.
     */
    public static void clearRegistrations() {
        REGISTERED_BLOCKS.clear();
    }

    // ==================== QUERY API ====================

    /**
     * Get the copied block from a CopyBlock at a position.
     *
     * <p>Example:</p>
     * <pre>{@code
     * BlockState copied = CopyBlockAPI.getCopiedBlock(level, pos);
     * if (copied != null && copied.is(BlockTags.LOGS)) {
     *     // The CopyBlock is copying a log!
     * }
     * }</pre>
     *
     * @param level The level/world
     * @param pos The position to check
     * @return The copied block state, or null if not a CopyBlock or empty
     */
    @Nullable
    public static BlockState getCopiedBlock(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ICopyBlockEntity copyBE) {
            BlockState copied = copyBE.getCopiedBlock();
            return copied.isAir() ? null : copied;
        }
        return null;
    }

    /**
     * Check if a position contains a CopyBlock (not just implements the interface).
     *
     * @param level The level/world
     * @param pos The position to check
     * @return true if this is a registered CopyBlock
     */
    public static boolean isCopyBlock(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof ICopyBlock;
    }

    /**
     * Check if a CopyBlock has copied content.
     *
     * @param level The level/world
     * @param pos The position to check
     * @return true if the block has copied content
     */
    public static boolean hasCopiedBlock(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ICopyBlockEntity copyBE) {
            return copyBE.hasCopiedBlock();
        }
        return false;
    }

    /**
     * Get the mass multiplier for a CopyBlock.
     *
     * @param block The block to check
     * @return The mass multiplier, or 1.0 if not a CopyBlock
     */
    public static float getMassMultiplier(Block block) {
        if (block instanceof ICopyBlock copyBlock) {
            return copyBlock.getMassMultiplier();
        }
        return 1.0f;
    }

    /**
     * Get the effective mass for a CopyBlock at a position.
     * This is: (copied block base mass) * (copy block mass multiplier)
     *
     * @param level The level/world
     * @param pos The position to check
     * @return The effective mass, or 0.0 if not applicable
     */
    public static double getEffectiveMass(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ICopyBlock copyBlock)) {
            return 0.0;
        }

        BlockState copied = getCopiedBlock(level, pos);
        if (copied == null) {
            return 0.0;
        }

        // Note: Base mass would come from VS2 or other physics system
        // This is a placeholder that addons can extend
        return 50.0 * copyBlock.getMassMultiplier();
    }

    /**
     * Utility to check if a block implements ICopyBlock.
     *
     * @param block The block to check
     * @return true if the block implements ICopyBlock
     */
    public static boolean implementsICopyBlock(Block block) {
        return block instanceof ICopyBlock;
    }

    /**
     * Utility to check if a BlockState is for a CopyBlock.
     *
     * @param state The BlockState to check
     * @return true if the state's block implements ICopyBlock
     */
    public static boolean implementsICopyBlock(BlockState state) {
        return state.getBlock() instanceof ICopyBlock;
    }
}