package com.vibey.imitari.mixin;

import com.vibey.imitari.block.CopyBlock;
import com.vibey.imitari.util.CopyBlockContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The ONLY mixin needed for dynamic tags.
 * Intercepts tag checks on BlockState and delegates to copied block if it's a CopyBlock.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateTagMixin {

    @Shadow
    public abstract Block m_60734_(); // getBlock() - obfuscated name

    /**
     * Inject at HEAD of is(TagKey) to check CopyBlock's copied block tags.
     * This is called EVERY time any mod checks if a BlockState has a tag.
     */
    @Inject(method = "m_204336_", at = @At("HEAD"), cancellable = true) // is(TagKey)
    private void imitari$checkCopiedTags(TagKey<Block> tag, CallbackInfoReturnable<Boolean> cir) {
        // CRITICAL: Only process if this is a CopyBlock
        // Early return means ZERO performance impact on all other blocks
        if (!(this.m_60734_() instanceof CopyBlock)) {
            return;
        }

        // Try to get the copied block's tags using our context system
        Boolean result = CopyBlockContext.checkCopiedBlockTag(tag);

        // If we got a result, use it. Otherwise let vanilla behavior continue.
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}