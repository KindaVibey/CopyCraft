package com.vibey.copycraft.vs2;

import com.vibey.copycraft.core.CopyBlockEntity;
import com.vibey.copycraft.core.ICopyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.valkyrienskies.mod.common.VSGameUtilsKt", remap = false)
public class VS2MassMixin {

    @Inject(
            method = "getBlockMass",
            at = @At("HEAD"),
            cancellable = true,
            require = 0  // Don't crash if VS2 not loaded
    )
    private static void onGetBlockMass(Level level, BlockPos pos, BlockState state,
                                       CallbackInfoReturnable<Double> cir) {

        if (state.getBlock() instanceof ICopyBlock) {
            if (level.getBlockEntity(pos) instanceof CopyBlockEntity be && be.hasMaterial()) {
                cir.setReturnValue(be.getMaterialData().getMass());
            }
        }
    }
}
