package com.vibey.imitari.mixin;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import com.vibey.imitari.config.ImitariConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateDestroySpeedMixin {

    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void imitari$getDynamicDestroySpeed(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        Block block = this.getBlock();
        if (!(block instanceof ICopyBlock copyBlock)) {
            return;
        }

        // Get the block entity
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CopyBlockEntity copyBE)) {
            return;
        }

        BlockState copiedState = copyBE.getCopiedBlock();

        // If empty CopyBlock, always return 0.5 hardness
        if (copiedState == null || copiedState.isAir()) {
            cir.setReturnValue(0.5f);
            return;
        }

        // Check config - if disabled, use empty hardness
        if (!ImitariConfig.COPY_HARDNESS.get()) {
            cir.setReturnValue(0.5f);
            return;
        }

        try {
            // Get base destroy speed from copied block
            float baseSpeed = copiedState.getDestroySpeed(level, pos);

            // Handle unbreakable blocks
            if (baseSpeed < 0.0f) {
                cir.setReturnValue(baseSpeed);
                return;
            }

            BlockState currentState = (BlockState)(Object)this;

            // Get effective mass multiplier (handles layers, double slabs, etc.)
            float effectiveMultiplier = getEffectiveMassMultiplier(currentState, copyBlock);

            // Calculate final destroy speed: base * multiplier
            float finalSpeed = baseSpeed * effectiveMultiplier;

            cir.setReturnValue(finalSpeed);

        } catch (Exception e) {
            // On error, return empty CopyBlock hardness
            cir.setReturnValue(0.5f);
        }
    }

    /**
     * Get the effective mass multiplier for any ICopyBlock implementation.
     * First tries to call getEffectiveMassMultiplier(BlockState) via reflection,
     * then falls back to getMassMultiplier().
     */
    private float getEffectiveMassMultiplier(BlockState state, ICopyBlock copyBlock) {
        try {
            // Try to get the effective multiplier method (for layers, double slabs, etc.)
            java.lang.reflect.Method method = copyBlock.getClass().getMethod("getEffectiveMassMultiplier", BlockState.class);
            Object result = method.invoke(copyBlock, state);
            if (result instanceof Float) {
                return (Float) result;
            }
        } catch (NoSuchMethodException e) {
            // Method doesn't exist - this is normal, just use base multiplier
        } catch (Exception e) {
            // Other errors - log and continue to fallback
        }

        // Fallback to base mass multiplier
        return copyBlock.getMassMultiplier();
    }
}