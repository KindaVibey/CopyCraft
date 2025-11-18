package com.vibey.copycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Slab-sized CopyBlock variant (0.5x multiplier)
 */
public class CopyBlockSlab extends CopyBlockVariant {
    protected static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    protected static final VoxelShape TOP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);

    public CopyBlockSlab(Properties properties) {
        super(properties, 0.5f);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.SLAB_TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);

        if (state.is(this)) {
            // Clicking on existing slab = make it double
            return state.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
        }

        // Determine if top or bottom slab based on click position
        Direction facing = context.getClickedFace();
        if (facing == Direction.DOWN || (facing != Direction.UP && context.getClickLocation().y - pos.getY() > 0.5)) {
            return this.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
        }

        return this.defaultBlockState();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);
        if (type != SlabType.DOUBLE && context.getItemInHand().getItem() == this.asItem()) {
            if (context.replacingClickedOnBlock()) {
                boolean isTop = context.getClickLocation().y - context.getClickedPos().getY() > 0.5;
                Direction facing = context.getClickedFace();

                if (type == SlabType.BOTTOM) {
                    return facing == Direction.UP || (facing != Direction.DOWN && isTop);
                } else {
                    return facing == Direction.DOWN || (facing != Direction.UP && !isTop);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);
        return switch (type) {
            case DOUBLE -> Shapes.block();
            case TOP -> TOP_SHAPE;
            default -> BOTTOM_SHAPE;
        };
    }

    // FIX: Collision shape mirrors visual shape (important for VS physics)
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}