package com.vibey.imitari.block.extras.clockwork;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Wing;
import org.valkyrienskies.mod.common.block.WingBlock;

/**
 * A CopyBlock variant that also functions as a VS2 wing.
 * Simple single-model design - just stores facing direction.
 */
public class CopyBlockFlapCW extends Block implements EntityBlock, ICopyBlock, WingBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    // Wing properties - adjust these for your needs
    private static final double WING_POWER = 150.0;
    private static final double WING_DRAG = 150.0;
    private static final double WING_BREAKING_FORCE = 10.0;

    public CopyBlockFlapCW(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public float getMassMultiplier() {
        return 0.3f;
    }

    // ==================== WING BLOCK IMPLEMENTATION ====================

    @Override
    public Wing getWing(Level level, BlockPos pos, BlockState blockState) {
        Direction facing = blockState.getValue(FACING);

        // Create wing normal vector based on facing direction
        Vector3d normal;
        switch (facing) {
            case EAST, WEST -> normal = new Vector3d(1.0, 0.0, 0.0);
            case UP, DOWN -> normal = new Vector3d(0.0, 1.0, 0.0);
            case NORTH, SOUTH -> normal = new Vector3d(0.0, 0.0, 1.0);
            default -> normal = new Vector3d(0.0, 1.0, 0.0);
        }

        return new Wing(normal, WING_POWER, WING_DRAG, WING_BREAKING_FORCE, 0.0);
    }

    // ==================== PLACEMENT ====================

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Face the direction the player is looking
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // ==================== ICOPYBLOCK IMPLEMENTATION ====================

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
        return copyblock$getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return copyblock$getDestroyProgress(state, player, level, pos);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return copyblock$getSoundType(state, level, pos, entity);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        return copyblock$use(state, level, pos, player, hand, hit);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack result = copyblock$getCloneItemStack(level, pos, state);
        return result.isEmpty() ? super.getCloneItemStack(level, pos, state) : result;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        copyblock$onRemove(state, level, pos, newState, isMoving);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        copyblock$playerWillDestroy(level, pos, state, player);
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        copyblock$setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return copyblock$getLightEmission(state, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case UP, DOWN -> Block.box(0.0, 6.0, 0.0, 16.0, 10.0, 16.0);
            case NORTH, SOUTH -> Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
            case EAST, WEST -> Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
