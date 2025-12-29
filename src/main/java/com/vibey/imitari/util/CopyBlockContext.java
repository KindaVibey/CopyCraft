package com.vibey.imitari.util;

import com.vibey.imitari.api.blockentity.ICopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Thread-safe context manager for CopyBlock tag checks.
 * Simple design: context is only valid during the getBlockState() call.
 */
public class CopyBlockContext {

    private static final ThreadLocal<Deque<Context>> CONTEXT_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private record Context(BlockGetter level, BlockPos pos) {}

    /**
     * Push a new context onto the stack
     */
    public static void push(BlockGetter level, BlockPos pos) {
        CONTEXT_STACK.get().push(new Context(level, pos.immutable()));
    }

    /**
     * Pop the current context from the stack
     */
    public static void pop() {
        Deque<Context> stack = CONTEXT_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }

        // Clean up ThreadLocal if stack is empty
        if (stack.isEmpty()) {
            CONTEXT_STACK.remove();
        }
    }

    /**
     * Check if the copied block has the given tag.
     * Returns null if no context or not a CopyBlock with copied content.
     *
     * IMPORTANT: Pops context after use to prevent leaks.
     */
    @Nullable
    public static Boolean checkCopiedBlockTag(TagKey<Block> tag) {
        Deque<Context> stack = CONTEXT_STACK.get();
        if (stack.isEmpty()) {
            return null;
        }

        Context ctx = stack.peek();
        if (ctx == null) {
            pop(); // Clean up invalid state
            return null;
        }

        BlockEntity be = ctx.level.getBlockEntity(ctx.pos);
        if (!(be instanceof ICopyBlockEntity copyBE)) {
            pop(); // Not our block, clean up
            return null;
        }

        BlockState copiedState = copyBE.getCopiedBlock();
        if (copiedState.isAir()) {
            pop(); // Empty, clean up
            return null;
        }

        // Check the COPIED block's tags
        boolean result = copiedState.is(tag);

        // ALWAYS pop after tag check to prevent memory leaks
        pop();

        return result;
    }

    /**
     * Emergency cleanup for thread safety
     */
    public static void clearAll() {
        CONTEXT_STACK.remove();
    }
}
