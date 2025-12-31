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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wall variant that properly supports texture copying.
 * Extends CopyBlockBase instead of WallBlock to avoid rendering conflicts.
 */
public class CopyBlockWall extends CopyBlockBase {
    // Wall connection properties
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final EnumProperty<WallSide> NORTH_WALL = EnumProperty.create("north", WallSide.class);
    public static final EnumProperty<WallSide> EAST_WALL = EnumProperty.create("east", WallSide.class);
    public static final EnumProperty<WallSide> SOUTH_WALL = EnumProperty.create("south", WallSide.class);
    public static final EnumProperty<WallSide> WEST_WALL = EnumProperty.create("west", WallSide.class);

    // Wall shapes
    private static final VoxelShape POST_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(5.0, 0.0, 0.0, 11.0, 14.0, 8.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5.0, 0.0, 8.0, 11.0, 14.0, 16.0);
    private static final VoxelShape EAST_SHAPE = Block.box(8.0, 0.0, 5.0, 16.0, 14.0, 11.0);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0, 0.0, 5.0, 8.0, 14.0, 11.0);
    private static final VoxelShape NORTH_TALL_SHAPE = Block.box(5.0, 0.0, 0.0, 11.0, 16.0, 8.0);
    private static final VoxelShape SOUTH_TALL_SHAPE = Block.box(5.0, 0.0, 8.0, 11.0, 16.0, 16.0);
    private static final VoxelShape EAST_TALL_SHAPE = Block.box(8.0, 0.0, 5.0, 16.0, 16.0, 11.0);
    private static final VoxelShape WEST_TALL_SHAPE = Block.box(0.0, 0.0, 5.0, 8.0, 16.0, 11.0);

    public CopyBlockWall(Properties properties) {
        super(properties, 0.5f); // Wall is ~50% of a full block

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(UP, true)
                .setValue(NORTH_WALL, WallSide.NONE)
                .setValue(EAST_WALL, WallSide.NONE)
                .setValue(SOUTH_WALL, WallSide.NONE)
                .setValue(WEST_WALL, WallSide.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH_WALL, EAST_WALL, SOUTH_WALL, WEST_WALL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = POST_SHAPE;

        WallSide north = state.getValue(NORTH_WALL);
        WallSide east = state.getValue(EAST_WALL);
        WallSide south = state.getValue(SOUTH_WALL);
        WallSide west = state.getValue(WEST_WALL);

        if (north != WallSide.NONE) {
            shape = Shapes.or(shape, north == WallSide.TALL ? NORTH_TALL_SHAPE : NORTH_SHAPE);
        }
        if (east != WallSide.NONE) {
            shape = Shapes.or(shape, east == WallSide.TALL ? EAST_TALL_SHAPE : EAST_SHAPE);
        }
        if (south != WallSide.NONE) {
            shape = Shapes.or(shape, south == WallSide.TALL ? SOUTH_TALL_SHAPE : SOUTH_SHAPE);
        }
        if (west != WallSide.NONE) {
            shape = Shapes.or(shape, west == WallSide.TALL ? WEST_TALL_SHAPE : WEST_SHAPE);
        }

        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    public boolean connectsToWall(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getBlock() instanceof CopyBlockWall;
    }

    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  Level level, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis axis = direction.getAxis();

        if (axis == Direction.Axis.Y) {
            return state.setValue(UP, canConnectToWall(neighborState, level, neighborPos, Direction.DOWN));
        }

        EnumProperty<WallSide> property = getWallProperty(direction);
        if (property != null) {
            WallSide wallSide = getWallSide(level, pos, direction, neighborState);
            return state.setValue(property, wallSide);
        }

        return state;
    }

    private EnumProperty<WallSide> getWallProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH_WALL;
            case EAST -> EAST_WALL;
            case SOUTH -> SOUTH_WALL;
            case WEST -> WEST_WALL;
            default -> null;
        };
    }

    private WallSide getWallSide(BlockGetter level, BlockPos pos, Direction direction, BlockState neighborState) {
        if (!canConnectToWall(neighborState, level, pos.relative(direction), direction.getOpposite())) {
            return WallSide.NONE;
        }

        // Check if wall should be tall (connects to another wall above)
        BlockState aboveNeighbor = level.getBlockState(pos.relative(direction).above());
        if (aboveNeighbor.getBlock() instanceof CopyBlockWall) {
            return WallSide.TALL;
        }

        return WallSide.LOW;
    }

    private boolean canConnectToWall(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getBlock() instanceof CopyBlockWall;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        // Update connections after placement
        state = updateWallConnections(level, pos, state);
        level.setBlock(pos, state, 3);

        // Call parent setPlacedBy for CopyBlock logic
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    private BlockState updateWallConnections(Level level, BlockPos pos, BlockState state) {
        // Update horizontal connections
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            EnumProperty<WallSide> property = getWallProperty(direction);

            if (property != null) {
                WallSide wallSide = getWallSide(level, pos, direction, neighborState);
                state = state.setValue(property, wallSide);
            }
        }

        // Update UP connection
        BlockState aboveState = level.getBlockState(pos.above());
        state = state.setValue(UP, canConnectToWall(aboveState, level, pos.above(), Direction.DOWN));

        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Update neighboring walls when this wall is removed
        if (!isMoving && newState.isAir()) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);

                if (neighborState.getBlock() instanceof CopyBlockWall) {
                    BlockState updatedState = updateWallConnections(level, neighborPos, neighborState);
                    level.setBlock(neighborPos, updatedState, 3);
                }
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
