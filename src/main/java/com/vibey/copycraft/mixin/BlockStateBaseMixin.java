package com.vibey.copycraft.mixin;

import com.vibey.copycraft.block.CopyBlock;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Unique
    private static final ThreadLocal<BlockGetter> copycraft$currentLevel = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<BlockPos> copycraft$currentPos = new ThreadLocal<>();

    @Shadow
    public abstract Block getBlock();

    @Shadow
    protected abstract BlockState asState();

    public static void copycraft$setContext(BlockGetter level, BlockPos pos) {
        copycraft$currentLevel.set(level);
        copycraft$currentPos.set(pos);
    }

    public static void copycraft$clearContext() {
        copycraft$currentLevel.remove();
        copycraft$currentPos.remove();
    }

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void copyBlockTags(TagKey<Block> tag, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = this.asState();

        // Only handle our CopyBlock
        if (state.getBlock() instanceof CopyBlock) {
            BlockGetter level = copycraft$currentLevel.get();
            BlockPos pos = copycraft$currentPos.get();

            if (level != null && pos != null) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CopyBlockEntity copyBE) {
                    BlockState copiedState = copyBE.getCopiedBlock();
                    if (!copiedState.isAir()) {
                        // Check if the copied block has this tag
                        cir.setReturnValue(copiedState.is(tag));
                    }
                }
            }
        }
    }
}