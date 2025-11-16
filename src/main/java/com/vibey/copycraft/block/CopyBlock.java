package com.vibey.copycraft.block;

import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CopyBlock extends Block implements EntityBlock {

    public CopyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopyBlockEntity(pos, state);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                // Return the copied block's explosion resistance
                return copiedState.getBlock().getExplosionResistance();
            }
        }
        // Default to our own explosion resistance
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                // Return the copied block's destroy progress (affects mining speed)
                return copiedState.getDestroyProgress(player, level, pos);
            }
        }
        // Default to our own destroy progress
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopyBlockEntity copyBlockEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        BlockState currentCopied = copyBlockEntity.getCopiedBlock();

        // Shift + empty hand = remove texture and drop the copied block
        if (player.isShiftKeyDown() && heldItem.isEmpty()) {
            if (!currentCopied.isAir()) {
                // Get the block and create a completely clean ItemStack
                Block copiedBlock = currentCopied.getBlock();
                ItemStack droppedItem = new ItemStack(copiedBlock, 1);

                // Fix NBT bug: Remove blank NBT tags that prevent stacking
                if (droppedItem.hasTag() && droppedItem.getTag().isEmpty()) {
                    droppedItem.setTag(null);
                }

                // Drop the item in the world with no pickup delay
                ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, droppedItem);
                itemEntity.setNoPickUpDelay();
                level.addFreshEntity(itemEntity);

                // Clear the texture
                copyBlockEntity.setCopiedBlock(Blocks.AIR.defaultBlockState());

                // Force neighbor updates to trigger chunk rebuild
                state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
                level.updateNeighborsAt(pos, state.getBlock());
            }

            return InteractionResult.SUCCESS;
        }

        // Block in hand
        if (heldItem.getItem() instanceof BlockItem blockItem) {
            Block targetBlock = blockItem.getBlock();

            if (targetBlock instanceof CopyBlock) {
                return InteractionResult.FAIL;
            }

            BlockState targetState = targetBlock.defaultBlockState();

            if (!targetState.isCollisionShapeFullBlock(level, pos)) {
                return InteractionResult.FAIL;
            }

            // If already has a texture, check if it's the same block for rotation
            if (!currentCopied.isAir()) {
                // Same block = rotate
                if (currentCopied.getBlock() == targetBlock) {
                    copyBlockEntity.setCopiedBlock(targetState);
                    return InteractionResult.SUCCESS;
                } else {
                    // Different block = can't switch, must clear first
                    return InteractionResult.FAIL;
                }
            } else {
                // Empty block = copy new texture and consume one item
                if (!player.isCreative()) {
                    heldItem.shrink(1);
                }
                copyBlockEntity.setCopiedBlock(targetState);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Only drop the copied block if broken in survival mode
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CopyBlockEntity copyBlockEntity) {
                BlockState copiedBlock = copyBlockEntity.getCopiedBlock();
                if (!copiedBlock.isAir()) {
                    // Check if a player broke it and if they're in creative
                    // We'll drop the item unless explicitly prevented
                    // Since we can't easily check player mode here, we'll use a flag
                    Boolean droppedByCreative = level.getBlockEntity(pos) instanceof CopyBlockEntity be ?
                            be.wasRemovedByCreative() : false;

                    if (!droppedByCreative) {
                        ItemStack droppedItem = new ItemStack(copiedBlock.getBlock().asItem(), 1);
                        ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, droppedItem);
                        level.addFreshEntity(itemEntity);
                    }
                }
            }

            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Mark if broken by creative player
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CopyBlockEntity copyBlockEntity) {
            if (player.isCreative()) {
                copyBlockEntity.setRemovedByCreative(true);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}