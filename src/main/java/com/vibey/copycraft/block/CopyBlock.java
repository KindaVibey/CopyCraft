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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CopyBlock extends Block implements EntityBlock {
    // Two properties give us 16 × 16 = 256 possible mass values
    public static final IntegerProperty MASS_HIGH = IntegerProperty.create("mass_high", 0, 15);
    public static final IntegerProperty MASS_LOW = IntegerProperty.create("mass_low", 0, 15);

    public CopyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(MASS_HIGH, 0)
                .setValue(MASS_LOW, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MASS_HIGH, MASS_LOW);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        CopyBlockEntity entity = new CopyBlockEntity(pos, state);
        return entity;
    }

    /**
     * Get the mass multiplier for this variant
     * Base CopyBlock has 1.0x multiplier
     */
    public float getMassMultiplier() {
        return 1.0f;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseResistance = copiedState.getBlock().getExplosionResistance();
                float scaledResistance = baseResistance * getMassMultiplier();
                return scaledResistance;
            }
        }
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseProgress = copiedState.getDestroyProgress(player, level, pos);
                float scaledProgress = baseProgress / getMassMultiplier();
                return scaledProgress;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    // ==================== COLLISION METHODS FOR VS2 ====================

    // Full block collision for stability
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    // CRITICAL: VS2 uses this for ship collision
    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    // VS2 uses this to determine if block occludes light and has solid collision
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    // Ensure proper light propagation
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    // Ensure VS2 recognizes this as a solid block
    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
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
                Block copiedBlock = currentCopied.getBlock();
                ItemStack droppedItem = new ItemStack(copiedBlock, 1);

                // Remove blank NBT tags that prevent stacking
                if (droppedItem.hasTag() && droppedItem.getTag().isEmpty()) {
                    droppedItem.setTag(null);
                }

                ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, droppedItem);
                itemEntity.setNoPickUpDelay();
                level.addFreshEntity(itemEntity);

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
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CopyBlockEntity copyBlockEntity) {
                BlockState copiedBlock = copyBlockEntity.getCopiedBlock();
                if (!copiedBlock.isAir()) {
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
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CopyBlockEntity copyBlockEntity) {
            if (player.isCreative()) {
                copyBlockEntity.setRemovedByCreative(true);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    // ==================== MASS ENCODING METHODS ====================

    public static int encodeMass(double mass) {
        mass = Math.max(0, Math.min(4400, mass));

        if (mass < 50) {
            return (int)mass;
        } else if (mass < 150) {
            return 50 + (int)((mass - 50) / 2);
        } else if (mass < 400) {
            return 100 + (int)((mass - 150) / 5);
        } else if (mass < 900) {
            return 150 + (int)((mass - 400) / 10);
        } else {
            int value = 200 + (int)((mass - 900) / 50);
            return Math.min(255, value);
        }
    }

    public static double decodeMass(int encoded) {
        if (encoded < 50) {
            return encoded;
        } else if (encoded < 100) {
            return 50 + (encoded - 50) * 2.0;
        } else if (encoded < 150) {
            return 150 + (encoded - 100) * 5.0;
        } else if (encoded < 200) {
            return 400 + (encoded - 150) * 10.0;
        } else {
            return 900 + (encoded - 200) * 50.0;
        }
    }

    public static double decodeMass(BlockState state) {
        int high = state.getValue(MASS_HIGH);
        int low = state.getValue(MASS_LOW);
        int encoded = high * 16 + low;
        return decodeMass(encoded);
    }

    public static BlockState setMass(BlockState state, double mass) {
        int encoded = encodeMass(mass);
        int high = encoded / 16;
        int low = encoded % 16;

        return state.setValue(MASS_HIGH, high).setValue(MASS_LOW, low);
    }
}