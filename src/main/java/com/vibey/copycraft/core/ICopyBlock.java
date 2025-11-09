package com.vibey.copycraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for all copycat-style blocks
 * Provides default material application behavior
 */
public interface ICopyBlock {

    /**
     * @return What fraction of a full block this shape represents
     * Full block = 1.0, Slab = 0.5, Stairs = 0.75, etc.
     */
    double getVolumeFactor(BlockState state);

    /**
     * Apply material to this copycat block
     * Handles right-click interaction with blocks
     */
    default boolean applyMaterial(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        // Must be a block item
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        BlockState material = blockItem.getBlock().defaultBlockState();

        // Don't copy other copycat blocks (would be weird!)
        if (material.getBlock() instanceof ICopyBlock) {
            return false;
        }

        // Get our block entity
        if (!(level.getBlockEntity(pos) instanceof CopyBlockEntity be)) {
            return false;
        }

        // Shift-click to remove material
        if (player.isShiftKeyDown() && be.hasMaterial()) {
            if (!level.isClientSide) {
                // Drop the consumed item (unless creative)
                if (!player.isCreative()) {
                    Block.popResource(level, pos, be.getConsumedItem());
                }
                be.clearMaterial();
            }
            return true;
        }

        // Already has material - do nothing
        if (be.hasMaterial()) {
            return false;
        }

        // Apply the material!
        if (!level.isClientSide) {
            BlockState state = level.getBlockState(pos);
            be.setMaterial(material, stack, getVolumeFactor(state));

            // Play placement sound
            level.playSound(null, pos, material.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0f, 0.8f);

            // Consume item (unless creative)
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }

        return true;
    }

    /**
     * Get dynamic hardness for destroy progress
     * Helper method for blocks to use in getDestroyProgress()
     */
    default float getCopyBlockDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CopyBlockEntity be && be.hasMaterial()) {
            BlockState material = be.getMaterial();
            float hardness = material.getDestroySpeed(level, pos);

            if (hardness < 0) {
                return 0.0f; // Unbreakable
            }

            if (hardness == 0) {
                return 1.0f; // Instant break
            }

            // Vanilla calculation
            float speed = player.getDestroySpeed(material);
            return speed / hardness / 30.0f;
        }
        return -1.0f; // Use default
    }

    /**
     * Get dynamic explosion resistance
     * Helper method for blocks to use in getExplosionResistance()
     */
    default float getCopyBlockExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        if (level.getBlockEntity(pos) instanceof CopyBlockEntity be && be.hasMaterial()) {
            return be.getMaterial().getBlock().getExplosionResistance();
        }
        return -1.0f; // Use default
    }
}