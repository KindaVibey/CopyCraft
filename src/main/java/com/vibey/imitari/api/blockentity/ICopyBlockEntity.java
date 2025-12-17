package com.vibey.imitari.api.blockentity;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for block entities that store copied block data.
 *
 * <p>Implementations should handle:</p>
 * <ul>
 *   <li>Storing the copied BlockState</li>
 *   <li>Syncing to clients via packets</li>
 *   <li>Saving/loading from NBT</li>
 *   <li>Providing model data for rendering</li>
 * </ul>
 *
 * <p><b>For Addon Developers:</b></p>
 * <p>You typically don't need to implement this yourself. Extend Imitari's
 * {@code CopyBlockEntity} class or use the provided base implementation.</p>
 *
 * @see com.vibey.imitari.blockentity.CopyBlockEntity
 */
public interface ICopyBlockEntity {

    /**
     * Get the currently copied block state.
     *
     * @return The copied block state, or AIR if empty
     */
    BlockState getCopiedBlock();

    /**
     * Set the copied block state.
     * This should trigger:
     * <ul>
     *   <li>Marking the block entity as changed</li>
     *   <li>Syncing to clients</li>
     *   <li>Updating VS2 mass (if applicable)</li>
     *   <li>Refreshing the model/texture</li>
     * </ul>
     *
     * @param newBlock The new block state to copy
     */
    void setCopiedBlock(BlockState newBlock);

    /**
     * Get the virtual rotation state (for log-like blocks).
     * Used to rotate textures without changing the copied BlockState.
     *
     * @return Rotation index (0-2)
     */
    int getVirtualRotation();

    /**
     * Check if this block entity has a copied block.
     *
     * @return true if a block is copied, false if empty
     */
    boolean hasCopiedBlock();

    /**
     * Check if this block was removed by a creative player.
     * Used to prevent dropping items when broken in creative.
     *
     * @return true if removed by creative player
     */
    boolean wasRemovedByCreative();

    /**
     * Mark this block as removed by a creative player.
     *
     * @param value true if being removed by creative player
     */
    void setRemovedByCreative(boolean value);

    /**
     * Force a model/texture refresh.
     * Call this when the block shape changes (e.g., slab -> double slab).
     */
    void forceModelRefresh();
}