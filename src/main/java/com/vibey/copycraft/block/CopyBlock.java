package com.vibey.copycraft.block;

import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class CopyBlock extends Block implements EntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();

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
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        LOGGER.info("CopyBlock.use called - Client: {}", level.isClientSide);

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopyBlockEntity copyBlockEntity)) {
            LOGGER.warn("BlockEntity at {} is not CopyBlockEntity!", pos);
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);

        // Clear with shift + empty hand
        if (player.isShiftKeyDown() && heldItem.isEmpty()) {
            copyBlockEntity.setCopiedBlock(Blocks.AIR.defaultBlockState());
            player.displayClientMessage(Component.literal("Cleared copied texture"), true);
            return InteractionResult.SUCCESS;
        }

        // Copy or rotate with block in hand
        if (heldItem.getItem() instanceof BlockItem blockItem) {
            Block targetBlock = blockItem.getBlock();

            if (targetBlock instanceof CopyBlock) {
                player.displayClientMessage(Component.literal("Cannot copy a Copy Block!"), true);
                return InteractionResult.FAIL;
            }

            BlockState targetState = targetBlock.defaultBlockState();

            if (!targetState.isCollisionShapeFullBlock(level, pos)) {
                player.displayClientMessage(Component.literal("Can only copy full block textures!"), true);
                return InteractionResult.FAIL;
            }

            // Check if clicking with same block - if so, rotate
            BlockState currentCopied = copyBlockEntity.getCopiedBlock();
            if (!currentCopied.isAir() && currentCopied.getBlock() == targetBlock) {
                LOGGER.info("Same block clicked - rotating");
                copyBlockEntity.setCopiedBlock(targetState);

                String rotationName = switch (copyBlockEntity.getVirtualRotation()) {
                    case 0 -> "Y-axis (vertical)";
                    case 1 -> "Z-axis (north-south)";
                    case 2 -> "X-axis (east-west)";
                    default -> "unknown";
                };

                player.displayClientMessage(Component.literal("Rotated to " + rotationName), true);
            } else {
                // New block
                LOGGER.info("New block being copied: {}", targetBlock.getName().getString());
                copyBlockEntity.setCopiedBlock(targetState);
                player.displayClientMessage(
                        Component.literal("Copied texture from: " + targetBlock.getName().getString()),
                        true
                );
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}