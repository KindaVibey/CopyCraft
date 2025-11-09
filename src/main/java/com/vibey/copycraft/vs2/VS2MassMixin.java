package com.vibey.copycraft.vs2;

import com.vibey.copycraft.core.CopyBlockEntity;
import com.vibey.copycraft.core.ICopyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to VS2's mass calculation to provide dynamic mass for copy blocks
 *
 * VS2's method signature (in Kotlin):
 * fun getBlockMass(level: Level, pos: BlockPos, blockState: BlockState): Double?
 *
 * In Java bytecode this becomes:
 * public static Double getBlockMass(Level, BlockPos, BlockState)
 */
@Pseudo // Don't crash if VS2 isn't loaded
@Mixin(targets = "org.valkyrienskies.mod.common.VSGameUtilsKt", remap = false)
public class VS2MassMixin {

    /**
     * Inject at the HEAD of getBlockMass to override for copy blocks
     * The method returns Double (nullable) not double (primitive)
     */
    @Inject(
            method = "getBlockMass(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Ljava/lang/Double;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0  // Optional - don't fail if VS2 isn't present
    )
    private static void onGetBlockMass(Level level, BlockPos pos, BlockState state,
                                       CallbackInfoReturnable<Double> cir) {

        // Only handle copy blocks
        if (state.getBlock() instanceof ICopyBlock) {
            // Get the block entity
            if (level.getBlockEntity(pos) instanceof CopyBlockEntity be && be.hasMaterial()) {
                // Return the calculated mass
                double mass = be.getMass();
                cir.setReturnValue(mass);

                // Debug logging
                System.out.println("[CopyCraft-VS2] Providing mass " + mass + " for copy block at " + pos);
            }
        }
    }
}