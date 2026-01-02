package com.vibey.imitari.util;

import com.vibey.imitari.api.blockentity.ICopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Zero-overhead context for CopyBlock tag checks.
 * No stacks, no ThreadLocals with deques, no allocations.
 *
 * Uses a single cached lookup per thread that's perfectly balanced:
 * - set() called on getBlockState HEAD
 * - clear() called on getBlockState RETURN
 * - No memory leaks possible
 */
public class CopyBlockContext {

    // Store ONLY the current lookup - single field, not a stack
    private static final ThreadLocal<LookupCache> CACHE =
            ThreadLocal.withInitial(LookupCache::new);

    private static class LookupCache {
        BlockGetter level;
        BlockPos pos;
        long lastAccessTime;
        BlockState cachedResult;

        void set(BlockGetter level, BlockPos pos) {
            this.level = level;
            this.pos = pos.immutable();
            this.lastAccessTime = System.nanoTime();
            this.cachedResult = null;
        }

        void clear() {
            this.level = null;
            this.pos = null;
            this.cachedResult = null;
        }

        boolean isValid() {
            // Cache valid for 1ms (same frame/operation)
            return level != null && (System.nanoTime() - lastAccessTime) < 1_000_000L;
        }
    }

    /**
     * Set lookup context - called ONLY by LevelGetBlockStateMixin at HEAD
     */
    public static void set(BlockGetter level, BlockPos pos) {
        CACHE.get().set(level, pos);
    }

    /**
     * Clear lookup context - called ONLY by LevelGetBlockStateMixin at RETURN
     * This ensures perfect balance - every set() has a matching clear()
     */
    public static void clear() {
        CACHE.get().clear();
    }

    /**
     * Check tag with zero overhead - uses cached context
     * Returns null if no valid context or not a CopyBlock
     */
    @Nullable
    public static Boolean checkCopiedBlockTag(TagKey<Block> tag) {
        LookupCache cache = CACHE.get();

        // No valid context? Return null to let vanilla handle it
        if (!cache.isValid()) {
            return null;
        }

        // Use cached result if available (for multiple tag checks on same block)
        if (cache.cachedResult == null) {
            BlockEntity be = cache.level.getBlockEntity(cache.pos);
            if (!(be instanceof ICopyBlockEntity copyBE)) {
                return null;
            }
            cache.cachedResult = copyBE.getCopiedBlock();
        }

        BlockState copiedState = cache.cachedResult;
        if (copiedState == null || copiedState.isAir()) {
            return null;
        }

        // Check the copied block's tags
        return copiedState.is(tag);
    }

    /**
     * Emergency cleanup - should never be needed with proper mixin balance
     */
    public static void clearAll() {
        CACHE.remove();
    }
}