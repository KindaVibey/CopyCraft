package com.vibey.imitari.mixin;

import com.vibey.imitari.util.CopyBlockContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures context when BlockStates are retrieved from the world.
 *
 * CRITICAL: Perfect push/pop balance
 * - set() at HEAD (before operation)
 * - clear() at RETURN (after operation, always)
 *
 * This ensures zero memory leaks and minimal overhead.
 */
@Mixin(Level.class)
public abstract class LevelGetBlockStateMixin {

    @Inject(method = "getBlockState", at = @At("HEAD"))
    private void imitari$setContext(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        CopyBlockContext.set((Level)(Object)this, pos);
    }

    @Inject(method = "getBlockState", at = @At("RETURN"))
    private void imitari$clearContext(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        // ALWAYS clear - no conditions, perfect balance
        CopyBlockContext.clear();
    }
}