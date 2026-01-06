package com.vibey.imitari.compat.cbc;

import com.mojang.logging.LogUtils;
import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.api.blockentity.ICopyBlockEntity;
import com.vibey.imitari.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesHandler;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesProvider;
import rbasamoyai.createbigcannons.block_armor_properties.mimicking_blocks.AbstractMimickingBlockArmorProperties;
import rbasamoyai.createbigcannons.block_armor_properties.mimicking_blocks.MimickingBlockArmorUnit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Implementation of CBC block armor integration for Imitari CopyBlocks.
 *
 * This injects our blocks into CBC's BLOCK_MAP after data reload to avoid
 * needing JSON files for every block. Works for any mod that implements ICopyBlock!
 */
@Mod.EventBusSubscriber(modid = "imitari", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CBCIntegrationImpl {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Register all Imitari CopyBlocks with CBC's block armor system.
     */
    public static void register() {
        // Register custom serializers for all our blocks
        registerCopyBlock(ModBlocks.COPY_BLOCK.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_GHOST.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_SLAB.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_STAIRS.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_LAYER.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_FENCE.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_FENCE_GATE.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_WALL.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_DOOR.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_IRON_DOOR.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_TRAPDOOR.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_IRON_TRAPDOOR.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_PANE.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_BUTTON.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_LEVER.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_PRESSURE_PLATE.get());
        registerCopyBlock(ModBlocks.COPY_BLOCK_LADDER.get());

        LOGGER.info("CBC integration: Custom serializers registered");
    }

    /**
     * Register a single CopyBlock with CBC's armor system.
     */
    private static void registerCopyBlock(Block block) {
        if (!(block instanceof ICopyBlock)) {
            return;
        }

        BlockArmorPropertiesHandler.registerCustomSerializer(
                block,
                AbstractMimickingBlockArmorProperties.createMimicrySerializer(
                        (defaultUnit, unitsByState) -> new ImitariCopyBlockArmorProperties(defaultUnit, unitsByState)
                )
        );
    }

    /**
     * Listen for data reload and inject our blocks into CBC's BLOCK_MAP.
     * This runs AFTER CBC's reload listener, so we can inject our blocks
     * even without JSON files.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDataReload(AddReloadListenerEvent event) {
        event.addListener((synchronizer, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            return synchronizer.wait(null).thenRunAsync(() -> {
                injectCopyBlocksIntoBlockMap();
            }, gameExecutor);
        });
    }

    /**
     * Inject all ICopyBlock instances into CBC's BLOCK_MAP using reflection.
     * This allows our blocks to work without requiring JSON files.
     */
    private static void injectCopyBlocksIntoBlockMap() {
        try {
            // Get CBC's private BLOCK_MAP via reflection
            Field blockMapField = BlockArmorPropertiesHandler.class.getDeclaredField("BLOCK_MAP");
            blockMapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Block, BlockArmorPropertiesProvider> blockMap = (Map<Block, BlockArmorPropertiesProvider>) blockMapField.get(null);

            int injected = 0;

            // Iterate through all registered blocks
            for (var entry : ForgeRegistries.BLOCKS.getEntries()) {
                Block block = entry.getValue();

                // Only process ICopyBlocks
                if (!(block instanceof ICopyBlock)) {
                    continue;
                }

                // Skip if already in the map (from JSON file)
                if (blockMap.containsKey(block)) {
                    continue;
                }

                // Create default armor properties for this block
                MimickingBlockArmorUnit defaultUnit = new MimickingBlockArmorUnit(
                        1.0,   // emptyHardness
                        1.0,   // materialHardnessMultiplier
                        0.5,   // emptyToughness
                        1.0    // materialToughnessMultiplier
                );

                // Create and inject the armor properties
                ImitariCopyBlockArmorProperties properties = new ImitariCopyBlockArmorProperties(
                        defaultUnit,
                        new java.util.HashMap<>()
                );

                blockMap.put(block, properties);
                injected++;
            }

            LOGGER.info("CBC integration: Injected {} CopyBlocks into BLOCK_MAP", injected);

        } catch (Exception e) {
            LOGGER.error("CBC integration: Failed to inject CopyBlocks into BLOCK_MAP", e);
        }
    }

    /**
     * Custom armor properties implementation for Imitari CopyBlocks.
     *
     * Integrates with CBC's block armor system to provide dynamic armor values
     * based on the copied block and the CopyBlock variant's mass multiplier.
     */
    public static class ImitariCopyBlockArmorProperties extends AbstractMimickingBlockArmorProperties {

        public ImitariCopyBlockArmorProperties(MimickingBlockArmorUnit defaultUnit,
                                               Map<BlockState, MimickingBlockArmorUnit> unitsByState) {
            super(defaultUnit, unitsByState);
        }

        @Override
        protected BlockState getCopiedState(Level level, BlockState state, BlockPos pos) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ICopyBlockEntity copyBE) {
                return copyBE.getCopiedBlock();
            }
            return Blocks.AIR.defaultBlockState();
        }

        @Override
        protected boolean isEmptyState(Level level, BlockState copiedState, BlockState state, BlockPos pos) {
            return copiedState == null || copiedState.isAir();
        }

        /**
         * Calculate hardness with effective mass multiplier support.
         *
         * This handles:
         * - CopyBlockLayer with 1-8 layers (0.125 * layers)
         * - CopyBlockSlab with double slabs (0.5 * 2)
         * - All other blocks use their base mass multiplier
         */
        @Override
        public double hardness(Level level, BlockState state, BlockPos pos, boolean recurse) {
            BlockState copiedState = this.getCopiedState(level, state, pos);

            // Empty CopyBlock - use default empty hardness
            if (this.isEmptyState(level, copiedState, state, pos)) {
                return this.getDefaultProperties().emptyHardness();
            }

            // Block is indestructible
            if (copiedState.getDestroySpeed(level, pos) == -1) {
                return 1000.0; // Very high hardness for indestructible blocks
            }

            // Get effective mass multiplier for this specific block state
            float effectiveMultiplier = getEffectiveMassMultiplier(state);

            // Get copied block's armor properties from CBC's system
            // This respects any custom CBC data packs for the copied block
            double copiedHardness = BlockArmorPropertiesHandler.getProperties(copiedState)
                    .hardness(level, copiedState, pos, false);

            // Apply our effective mass multiplier
            return copiedHardness * effectiveMultiplier;
        }

        /**
         * Calculate toughness with effective mass multiplier support.
         */
        @Override
        public double toughness(Level level, BlockState state, BlockPos pos, boolean recurse) {
            BlockState copiedState = this.getCopiedState(level, state, pos);

            // Empty CopyBlock - use default empty toughness
            if (this.isEmptyState(level, copiedState, state, pos)) {
                return this.getDefaultProperties().emptyToughness();
            }

            // Block is indestructible
            if (copiedState.getDestroySpeed(level, pos) == -1) {
                return copiedState.getBlock().getExplosionResistance();
            }

            // Get effective mass multiplier for this specific block state
            float effectiveMultiplier = getEffectiveMassMultiplier(state);

            // Get copied block's armor properties from CBC's system
            double copiedToughness = BlockArmorPropertiesHandler.getProperties(copiedState)
                    .toughness(level, copiedState, pos, false);

            // Apply our effective mass multiplier
            return copiedToughness * effectiveMultiplier;
        }

        /**
         * Get the effective mass multiplier for a block state.
         *
         * This handles special cases like:
         * - CopyBlockLayer: base multiplier (0.125) * layer count (1-8)
         * - CopyBlockSlab: base multiplier (0.5) * 2 for double slabs
         * - All others: just the base mass multiplier
         */
        private float getEffectiveMassMultiplier(BlockState state) {
            Block block = state.getBlock();
            if (!(block instanceof ICopyBlock copyBlock)) {
                return 1.0f;
            }

            // Try to get getEffectiveMassMultiplier method (for layers, double slabs, etc.)
            try {
                Method method = block.getClass().getMethod("getEffectiveMassMultiplier", BlockState.class);
                Object result = method.invoke(block, state);
                if (result instanceof Float) {
                    return (Float) result;
                }
            } catch (NoSuchMethodException e) {
                // Expected - most blocks don't have this method
            } catch (Exception e) {
                // Silently continue to fallback
            }

            // Fallback to base mass multiplier
            return copyBlock.getMassMultiplier();
        }
    }
}