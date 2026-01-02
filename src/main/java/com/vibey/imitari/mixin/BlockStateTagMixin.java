package com.vibey.imitari.mixin;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.config.ImitariConfig;
import com.vibey.imitari.util.CopyBlockContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * The ONLY mixin needed for dynamic tags.
 * Zero-overhead implementation using cached context.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateTagMixin {

    @Shadow
    public abstract Block getBlock();

    /**
     * Inject at HEAD of is(TagKey) to check CopyBlock's copied block tags.
     */
    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void imitari$checkCopiedTags(TagKey<Block> tag, CallbackInfoReturnable<Boolean> cir) {
        // CRITICAL: Only process if this implements ICopyBlock
        Block block = this.getBlock();
        if (!(block instanceof ICopyBlock copyBlock) || !copyBlock.useDynamicTags()) {
            return;
        }

        // Check if this tag is blacklisted
        if (isTagBlacklisted(tag)) {
            return; // Don't inherit blacklisted tags
        }

        // Try to get the copied block's tags using our cached context
        Boolean result = CopyBlockContext.checkCopiedBlockTag(tag);

        // If we got a result, use it. Otherwise, let vanilla behavior continue.
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    private boolean isTagBlacklisted(TagKey<Block> tag) {
        try {
            List<? extends String> blacklist = ImitariConfig.TAG_BLACKLIST.get();
            if (blacklist.isEmpty()) {
                return false;
            }

            ResourceLocation tagLocation = tag.location();
            String tagString = tagLocation.toString();

            for (String blacklistedTag : blacklist) {
                if (tagString.equals(blacklistedTag)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Config not loaded yet or error - allow all tags
        }

        return false;
    }
}