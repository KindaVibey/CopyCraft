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
import org.valkyrienskies.mod.common.BlockStateInfo;
import kotlin.Pair;
import org.valkyrienskies.core.apigame.world.chunks.BlockType;

/**
 * Alternative approach: Mixin directly into BlockStateInfo to intercept ALL mass queries
 */
@Pseudo
@Mixin(value = BlockStateInfo.class, remap = false)
public class AlternativeVSMassContextMixin {

    /**
     * Intercept the get() method that VS uses to query block masses
     * This is more reliable than trying to hook into getBlockMass
     */
    @Inject(
            method = "get(Lnet/minecraft/world/level/block/state/BlockState;)Lkotlin/Pair;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void interceptBlockStateInfoGet(BlockState blockState, CallbackInfoReturnable<Pair<Double, BlockType>> cir) {
        System.out.println("[CopyCraft VS ALT] BlockStateInfo.get() called for: " + blockState);

        if (!(blockState.getBlock() instanceof CopyBlockVariant copyBlockVariant)) {
            return; // Not our block, let VS handle it normally
        }

        Level level = CopyCraftWeights.currentLevel.get();
        BlockPos pos = CopyCraftWeights.currentPos.get();

        if (level == null || pos == null) {
            System.out.println("[CopyCraft VS ALT] No context available, trying direct lookup...");
            // Try to find the block entity without context (less reliable but better than nothing)
            return;
        }

        System.out.println("[CopyCraft VS ALT] Context available: " + pos);
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof CopyBlockEntity copyBE)) {
            System.out.println("[CopyCraft VS ALT] Wrong BE type: " + be);
            return;
        }

        BlockState copiedState = copyBE.getCopiedBlock();
        if (copiedState.isAir()) {
            System.out.println("[CopyCraft VS ALT] Copied state is AIR");
            return;
        }

        // Get the copied block's info from VS
        Pair<Double, BlockType> copiedInfo = BlockStateInfo.INSTANCE.get(copiedState);
        if (copiedInfo == null || copiedInfo.getFirst() == null) {
            System.out.println("[CopyCraft VS ALT] No info for copied block: " + copiedState);
            return;
        }

        // Apply multiplier
        Double copiedMass = copiedInfo.getFirst();
        float multiplier = copyBlockVariant.getMassMultiplier();
        double finalMass = copiedMass * multiplier;
        BlockType blockType = copiedInfo.getSecond();

        System.out.println("[CopyCraft VS ALT] SUCCESS: " + copiedState +
                " mass=" + copiedMass + " x" + multiplier + " = " + finalMass + " kg");

        // Return modified info and cancel original call
        cir.setReturnValue(new Pair<>(finalMass, blockType));
    }
}

