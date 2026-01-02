package com.vibey.imitari.api.tags;

import com.vibey.imitari.api.CopyBlockAPI;
import com.vibey.imitari.util.CopyBlockContext;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * API for working with CopyBlock dynamic tags.
 *
 * <p>CopyBlocks automatically inherit all tags from their copied blocks.
 * This means if you copy an oak log, the CopyBlock will return true for
 * all log tags, wood tags, etc.</p>
 *
 * <p><b>For Addon Developers:</b></p>
 * <p>You typically don't need to interact with this API directly - tag inheritance
 * happens automatically through Imitari's mixins. However, these utilities are
 * provided for advanced use cases.</p>
 *
 * <p><b>How It Works:</b></p>
 * <ol>
 *   <li>When {@code BlockState.is(TagKey)} is called on a CopyBlock</li>
 *   <li>Imitari's mixin intercepts the call</li>
 *   <li>Checks if the block has {@code useDynamicTags() == true}</li>
 *   <li>Queries the copied block's tags using cached context</li>
 * </ol>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * // Check if a CopyBlock is copying something with a specific tag
 * if (CopyBlockTagAPI.copiedBlockHasTag(level, pos, BlockTags.LOGS)) {
 *     // The copied block is a log!
 * }
 * }</pre>
 */
public class CopyBlockTagAPI {

    /**
     * Check if a CopyBlock's copied content has a specific tag.
     *
     * <p>This is more explicit than just calling {@code state.is(tag)} and can be
     * used when you specifically want to check the copied block's tags.</p>
     *
     * @param level The level/world
     * @param pos The block position
     * @param tag The tag to check
     * @return true if the copied block has this tag, false otherwise
     */
    public static boolean copiedBlockHasTag(BlockGetter level, BlockPos pos, TagKey<Block> tag) {
        BlockState copied = CopyBlockAPI.getCopiedBlock(level, pos);
        return copied != null && copied.is(tag);
    }

    /**
     * Check if a CopyBlock's copied content has a specific tag.
     * This version works with a BlockState directly.
     *
     * @param level The level/world
     * @param pos The block position
     * @param state The CopyBlock's state (for optimization)
     * @param tag The tag to check
     * @return true if the copied block has this tag
     */
    public static boolean copiedBlockHasTag(BlockGetter level, BlockPos pos,
                                            BlockState state, TagKey<Block> tag) {
        BlockState copied = CopyBlockAPI.getCopiedBlock(level, pos);
        return copied != null && copied.is(tag);
    }

    /**
     * Manually check a tag with context.
     * This is what Imitari's mixin calls internally.
     *
     * <p>Only needed if you're implementing custom tag checking logic.</p>
     *
     * @param tag The tag to check
     * @return true/false if tag matches, null if no context available
     */
    @Nullable
    public static Boolean checkWithContext(TagKey<Block> tag) {
        return CopyBlockContext.checkCopiedBlockTag(tag);
    }

    /**
     * Emergency cleanup of all contexts.
     * Should not be needed in normal operation with the new zero-overhead system.
     */
    public static void clearAllContexts() {
        CopyBlockContext.clearAll();
    }

    /**
     * Check if dynamic tags are enabled for a block.
     *
     * @param block The block to check
     * @return true if the block uses dynamic tags
     */
    public static boolean usesDynamicTags(Block block) {
        if (block instanceof com.vibey.imitari.api.ICopyBlock copyBlock) {
            return copyBlock.useDynamicTags();
        }
        return false;
    }

    /**
     * Check if dynamic tags are enabled for a block state.
     *
     * @param state The state to check
     * @return true if the block uses dynamic tags
     */
    public static boolean usesDynamicTags(BlockState state) {
        return usesDynamicTags(state.getBlock());
    }
}