package com.vibey.copycraft.vs2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keep the original mixin too - use whichever one works for your VS version
 */
@Pseudo
@Mixin(targets = "org.valkyrienskies.mod.common.assembly.ShipAssemblyKt", remap = false)
class OriginalVSMassContextMixin {

    @Inject(
            method = "getBlockMass",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private static void setMassContextOriginal(Level level, BlockPos pos, BlockState blockState,
                                               Object ship, CallbackInfoReturnable<Double> cir) {
        CopyCraftWeights.setContext(level, pos);
        System.out.println("[CopyCraft VS ORIG] getBlockMass HEAD: " + blockState + " at " + pos);
    }

    @Inject(
            method = "getBlockMass",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private static void clearMassContextOriginal(Level level, BlockPos pos, BlockState blockState,
                                                 Object ship, CallbackInfoReturnable<Double> cir) {
        System.out.println("[CopyCraft VS ORIG] getBlockMass RETURN: " + cir.getReturnValue());
        CopyCraftWeights.clearContext();
    }
}
