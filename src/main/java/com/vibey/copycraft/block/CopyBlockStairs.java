package com.vibey.copycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stairs-sized CopyBlock variant (0.75x multiplier)
 */
public class CopyBlockStairs extends CopyBlockVariant {
    // Simplified shapes
    protected static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    protected static final VoxelShape TOP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);

    protected static final VoxelShape NORTH_BOTTOM = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(0, 8, 0, 16, 16, 8)
    );

    public CopyBlockStairs(Properties properties) {
        super(properties, 0.75f);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.STRAIGHT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.STAIRS_SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos pos = context.getClickedPos();

        Half half = (context.getClickedFace() == Direction.DOWN ||
                (context.getClickedFace() != Direction.UP &&
                        context.getClickLocation().y - pos.getY() > 0.5))
                ? Half.TOP : Half.BOTTOM;

        BlockState state = this.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.HALF, half);

        return state.setValue(BlockStateProperties.STAIRS_SHAPE,
                getStairsShape(state, context.getLevel(), pos));
    }

    private StairsShape getStairsShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Half half = state.getValue(BlockStateProperties.HALF);

        BlockState leftState = level.getBlockState(pos.relative(facing.getCounterClockWise()));
        BlockState rightState = level.getBlockState(pos.relative(facing.getClockWise()));

        if (leftState.getBlock() instanceof CopyBlockStairs && leftState.getValue(BlockStateProperties.HALF) == half) {
            Direction leftFacing = leftState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (leftFacing.getAxis() != facing.getAxis() && canConnectTo(state, leftState, facing.getCounterClockWise())) {
                return leftFacing == facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.INNER_LEFT;
            }
        }

        if (rightState.getBlock() instanceof CopyBlockStairs && rightState.getValue(BlockStateProperties.HALF) == half) {
            Direction rightFacing = rightState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (rightFacing.getAxis() != facing.getAxis() && canConnectTo(state, rightState, facing.getClockWise())) {
                return rightFacing == facing.getClockWise() ? StairsShape.OUTER_RIGHT : StairsShape.INNER_RIGHT;
            }
        }

        return StairsShape.STRAIGHT;
    }

    private boolean canConnectTo(BlockState state, BlockState neighbor, Direction direction) {
        return neighbor.getBlock() instanceof CopyBlockStairs;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        StairsShape shape = state.getValue(BlockStateProperties.STAIRS_SHAPE);

        switch (mirror) {
            case LEFT_RIGHT -> {
                if (facing.getAxis() == Direction.Axis.Z) {
                    switch (shape) {
                        case INNER_LEFT -> {
                            return state.rotate(Rotation.CLOCKWISE_180)
                                    .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.INNER_RIGHT);
                        }
                        case INNER_RIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180)
                                    .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.INNER_LEFT);
                        }
                        case OUTER_LEFT -> {
                            return state.rotate(Rotation.CLOCKWISE_180)
                                    .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.OUTER_RIGHT);
                        }
                        case OUTER_RIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180)
                                    .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.OUTER_LEFT);
                        }
                        default -> {
                            return state.rotate(Rotation.CLOCKWISE_180);
                        }
                    }
                }
                return state;
            }
            case FRONT_BACK -> {
                if (facing.getAxis() == Direction.Axis.X) {
                    switch (shape) {
                        case INNER_LEFT, INNER_RIGHT, OUTER_LEFT, OUTER_RIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180)
                                    .setValue(BlockStateProperties.STAIRS_SHAPE, shape);
                        }
                        default -> {
                            return state.rotate(Rotation.CLOCKWISE_180);
                        }
                    }
                }
                return state;
            }
            default -> {
                return state;
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(BlockStateProperties.HALF) == Half.BOTTOM ? BOTTOM_SHAPE : TOP_SHAPE;
    }

    // FIX: Collision shape mirrors visual shape (important for VS physics)
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}