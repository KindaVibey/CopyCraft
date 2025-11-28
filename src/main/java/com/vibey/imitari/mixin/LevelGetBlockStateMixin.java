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
 * This allows the tag check mixin to know which BlockEntity to check.
 */
@Mixin(Level.class)
public abstract class LevelGetBlockStateMixin {

    @Inject(method = "m_8055_", at = @At("HEAD")) // getBlockState obfuscated
    private void imitari$captureContextBefore(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        CopyBlockContext.push((Level)(Object)this, pos);
    }

    @Inject(method = "m_8055_", at = @At("RETURN")) // getBlockState obfuscated
    private void imitari$captureContextAfter(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();

        // If it's NOT a CopyBlock, pop immediately (no tag checks will need context)
        if (!(state.getBlock() instanceof com.vibey.imitari.block.CopyBlock)) {
            CopyBlockContext.pop();
        }
        // If it IS a CopyBlock, leave context for tag checks to consume
    }
}