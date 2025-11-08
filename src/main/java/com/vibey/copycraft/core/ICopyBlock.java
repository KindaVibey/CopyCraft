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

public interface ICopyBlock {

    /**
     * @return What fraction of a full block this shape represents
     * Full block = 1.0, Slab = 0.5, Stairs = 0.75, Wall = variable, etc.
     */
    double getVolumeFactor(BlockState state);

    /**
     * Apply material to this copycat block
     */
    default boolean applyMaterial(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        BlockState material = blockItem.getBlock().defaultBlockState();

        // Don't copy other copycat blocks
        if (material.getBlock() instanceof ICopyBlock) {
            return false;
        }

        if (level.getBlockEntity(pos) instanceof CopyBlockEntity be) {
            // Shift-click removes material
            if (player.isShiftKeyDown() && be.hasMaterial()) {
                if (!level.isClientSide) {
                    if (!player.isCreative()) {
                        Block.popResource(level, pos, be.getConsumedItem());
                    }
                    be.clearMaterial();
                }
                return true;
            }

            // Already has material
            if (be.hasMaterial()) {
                return false;
            }

            // Apply material
            if (!level.isClientSide) {
                BlockState state = level.getBlockState(pos);
                be.setMaterial(material, stack, getVolumeFactor(state));

                level.playSound(null, pos, material.getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1.0f, 0.8f);

                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Get dynamic hardness
     */
    default float getCopyBlockDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CopyBlockEntity be && be.hasMaterial()) {
            float hardness = be.getMaterialData().getHardness();
            if (hardness < 0) return 0.0f;

            float destroySpeed = player.getDestroySpeed(state);
            return destroySpeed / hardness / 30.0f;
        }
        return -1.0f; // Use default
    }

    /**
     * Get dynamic explosion resistance
     */
    default float getCopyBlockExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        if (level.getBlockEntity(pos) instanceof CopyBlockEntity be && be.hasMaterial()) {
            return be.getMaterialData().getExplosionResistance();
        }
        return -1.0f; // Use default
    }
}
