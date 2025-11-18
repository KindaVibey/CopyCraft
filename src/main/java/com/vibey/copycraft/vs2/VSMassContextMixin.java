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
 * This mixin targets the VS2 assembly code to provide context for mass calculations.
 * The @Pseudo annotation means this mixin won't cause crashes if VS isn't installed.
 */
@Pseudo
@Mixin(targets = {
        "org.valkyrienskies.mod.common.assembly.ShipAssemblyKt",
        "org.valkyrienskies.mod.common.assembly.ShipAssemblyKt$getBlockMass"
}, remap = false)
public class VSMassContextMixin {

    /**
     * Inject at HEAD to set context BEFORE VS queries the mass
     */
    @Inject(
            method = "getBlockMass",
            at = @At("HEAD"),
            remap = false,
            require = 0  // Don't require this mixin to succeed
    )
    private static void setMassContext(Level level, BlockPos pos, BlockState blockState,
                                       Object ship, CallbackInfoReturnable<Double> cir) {
        CopyCraftWeights.setContext(level, pos);
        System.out.println("[CopyCraft VS] getBlockMass HEAD: " + blockState + " at " + pos);
    }

    /**
     * Inject at RETURN to clear context AFTER VS is done
     */
    @Inject(
            method = "getBlockMass",
            at = @At("RETURN"),
            remap = false,
            require = 0  // Don't require this mixin to succeed
    )
    private static void clearMassContext(Level level, BlockPos pos, BlockState blockState,
                                         Object ship, CallbackInfoReturnable<Double> cir) {
        Double result = cir.getReturnValue();
        System.out.println("[CopyCraft VS] getBlockMass RETURN: " + result + " kg for " + blockState);
        CopyCraftWeights.clearContext();
    }
}