package com.vibey.copycraft.vs2;

import com.vibey.copycraft.block.CopyBlockVariant;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;

@Pseudo
@Mixin(targets = "org.valkyrienskies.mod.common.assembly.ShipAssemblyKt", remap = false)
public class VS2MassMixin {

    @Inject(method = "getBlockMass", at = @At("RETURN"), cancellable = true, remap = false)
    private static void modifyMass(Level level, BlockPos pos, BlockState blockState,
                                   ShipObjectServerWorld ship, CallbackInfoReturnable<Double> cir) {
        // Check if this is a CopyBlock variant
        if (blockState.getBlock() instanceof CopyBlockVariant copyBlockVariant) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CopyBlockEntity copyBE) {
                BlockState copiedState = copyBE.getCopiedBlock();
                if (!copiedState.isAir()) {
                    // Get the original mass that was returned
                    double originalMass = cir.getReturnValue();

                    // Apply the variant's mass multiplier
                    // The original mass is likely the copied block's mass already
                    // so we just scale it
                    double scaledMass = originalMass * copyBlockVariant.getMassMultiplier();

                    cir.setReturnValue(scaledMass);
                }
            }
        }
    }
}