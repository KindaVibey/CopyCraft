package com.vibey.imitari.block;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.block.base.CopyBlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fence variant that properly supports texture copying.
 * Extends CopyBlockBase instead of FenceBlock to avoid rendering conflicts.
 */
public class CopyBlockFence extends CopyBlockBase {
    // Fence connection properties
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    // Fence shapes
    private static final VoxelShape POST_SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(7.0, 12.0, 0.0, 9.0, 15.0, 9.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(7.0, 12.0, 7.0, 9.0, 15.0, 16.0);
    private static final VoxelShape EAST_SHAPE = Block.box(7.0, 12.0, 7.0, 16.0, 15.0, 9.0);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0, 12.0, 7.0, 9.0, 15.0, 9.0);
    private static final VoxelShape NORTH_BOTTOM_SHAPE = Block.box(7.0, 6.0, 0.0, 9.0, 9.0, 9.0);
    private static final VoxelShape SOUTH_BOTTOM_SHAPE = Block.box(7.0, 6.0, 7.0, 9.0, 9.0, 16.0);
    private static final VoxelShape EAST_BOTTOM_SHAPE = Block.box(7.0, 6.0, 7.0, 16.0, 9.0, 9.0);
    private static final VoxelShape WEST_BOTTOM_SHAPE = Block.box(0.0, 6.0, 7.0, 9.0, 9.0, 9.0);

    public CopyBlockFence(Properties properties) {
        super(properties, 0.4f); // Fence is ~40% of a full block

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = POST_SHAPE;

        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, NORTH_SHAPE, NORTH_BOTTOM_SHAPE);
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, EAST_SHAPE, EAST_BOTTOM_SHAPE);
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, SOUTH_SHAPE, SOUTH_BOTTOM_SHAPE);
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, WEST_SHAPE, WEST_BOTTOM_SHAPE);
        }

        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    public boolean canConnectToFence(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Connect to other fences and fence gates
        return state.getBlock() instanceof CopyBlockFence ||
               state.getBlock() instanceof CopyBlockFenceGate;
    }

    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  Level level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty property = getConnectionProperty(direction);
        if (property != null) {
            boolean shouldConnect = canConnectToFence(neighborState, level, neighborPos, direction.getOpposite());
            return state.setValue(property, shouldConnect);
        }
        return state;
    }

    private BooleanProperty getConnectionProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> null;
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        // Update connections after placement
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            BooleanProperty property = getConnectionProperty(direction);

            if (property != null) {
                boolean shouldConnect = canConnectToFence(neighborState, level, neighborPos, direction.getOpposite());
                state = state.setValue(property, shouldConnect);

                // Also update the neighbor to connect back to us
                if (neighborState.getBlock() instanceof CopyBlockFence) {
                    BooleanProperty neighborProperty = getConnectionProperty(direction.getOpposite());
                    if (neighborProperty != null) {
                        level.setBlock(neighborPos, neighborState.setValue(neighborProperty, true), 3);
                    }
                }
            }
        }

        level.setBlock(pos, state, 3);

        // Call parent setPlacedBy for CopyBlock logic
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Update neighboring fences when this fence is removed
        if (!isMoving && newState.isAir()) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);

                if (neighborState.getBlock() instanceof CopyBlockFence) {
                    BooleanProperty neighborProperty = getConnectionProperty(direction.getOpposite());
                    if (neighborProperty != null && neighborState.getValue(neighborProperty)) {
                        level.setBlock(neighborPos, neighborState.setValue(neighborProperty, false), 3);
                    }
                }
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
