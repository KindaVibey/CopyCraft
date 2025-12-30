package com.vibey.imitari.mixin;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
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
    public abstract net.minecraft.world.level.block.Block m_60734_();

    @Inject(method = "m_60800_", at = @At("HEAD"), cancellable = true)
    private void imitari$getDynamicDestroySpeed(BlockGetter p_60801_, BlockPos p_60802_, CallbackInfoReturnable<Float> cir) {
        if (!(this.m_60734_() instanceof ICopyBlock)) {
            return;
        }

        try {
            BlockEntity be = p_60801_.getBlockEntity(p_60802_);
            if (!(be instanceof CopyBlockEntity copyBE)) {
                return;
            }

            BlockState copiedState = copyBE.getCopiedBlock();

            if (copiedState == null || copiedState.isAir()) {
                cir.setReturnValue(0.5f);
                return;
            }

            float baseSpeed = copiedState.getDestroySpeed(p_60801_, p_60802_);

            if (baseSpeed < 0.0f) {
                cir.setReturnValue(baseSpeed);
                return;
            }

            net.minecraft.world.level.block.state.BlockState currentState = (net.minecraft.world.level.block.state.BlockState)(Object)this;
            ICopyBlock copyBlock = (ICopyBlock) this.m_60734_();

            // Try to get effective mass multiplier via reflection (for addon blocks)
            float effectiveMultiplier = getEffectiveMassMultiplier(currentState, copyBlock);
            float multipliedSpeed = baseSpeed * effectiveMultiplier;

            cir.setReturnValue(multipliedSpeed);

        } catch (Exception e) {
            System.err.println("[Imitari] ERROR in getDestroySpeed mixin:");
            e.printStackTrace();
        }
    }

    /**
     * Get the effective mass multiplier for any ICopyBlock implementation.
     * First tries to call getEffectiveMassMultiplier(BlockState) via reflection (for addon-friendly support),
     * then falls back to getMassMultiplier().
     */
    private float getEffectiveMassMultiplier(net.minecraft.world.level.block.state.BlockState state, ICopyBlock copyBlock) {
        try {
            // Try to call getEffectiveMassMultiplier(BlockState) if it exists
            java.lang.reflect.Method method = copyBlock.getClass().getMethod("getEffectiveMassMultiplier", net.minecraft.world.level.block.state.BlockState.class);
            Object result = method.invoke(copyBlock, state);
            if (result instanceof Float) {
                return (Float) result;
            }
        } catch (NoSuchMethodException e) {
            // Method doesn't exist - this is normal for blocks without effective multiplier
        } catch (Exception e) {
            // Other reflection errors - log but continue
            System.err.println("[Imitari] Failed to call getEffectiveMassMultiplier via reflection: " + e.getMessage());
        }

        // Fallback: use base multiplier
        return copyBlock.getMassMultiplier();
    }
}