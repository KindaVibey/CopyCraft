package com.vibey.imitari.block.base;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Convenient base class for creating CopyBlock variants.
 *
 * <p><b>Quick Start:</b></p>
 * <pre>{@code
 * public class MyQuarterBlock extends CopyBlockBase {
 *     public MyQuarterBlock(Properties properties) {
 *         super(properties, 0.25f); // 1/4 block mass
 *     }
 * }
 * }</pre>
 *
 * <p>This class provides:</p>
 * <ul>
 *   <li>Automatic EntityBlock implementation</li>
 *   <li>Full ICopyBlock delegation</li>
 *   <li>Proper rendering setup</li>
 *   <li>Light occlusion handling</li>
 *   <li>All interaction methods wired up</li>
 * </ul>
 *
 * <p><b>For Custom Shapes:</b></p>
 * <p>If you need custom shapes (stairs, slabs, etc), override the shape methods:</p>
 * <pre>{@code
 * @Override
 * public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
 *     return MY_CUSTOM_SHAPE;
 * }
 * }</pre>
 */
public class CopyBlockBase extends Block implements EntityBlock, ICopyBlock {
    private final float massMultiplier;

    /**
     * Create a CopyBlock with default full mass (1.0).
     *
     * @param properties Block properties
     */
    public CopyBlockBase(Properties properties) {
        this(properties, 1.0f);
    }

    /**
     * Create a CopyBlock with a custom mass multiplier.
     *
     * @param properties Block properties
     * @param massMultiplier The mass multiplier (0.0 to 1.0+)
     */
    public CopyBlockBase(Properties properties, float massMultiplier) {
        super(properties);
        this.massMultiplier = massMultiplier;
    }

    // ==================== ICOPYBLOCK IMPLEMENTATION ====================

    @Override
    public float getMassMultiplier() {
        return massMultiplier;
    }

    // ==================== RENDERING ====================

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    // ==================== BLOCK ENTITY ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopyBlockEntity(pos, state);
    }

    // ==================== PHYSICS DELEGATION ====================

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

    // ==================== INTERACTION ====================

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

    // ==================== LIFECYCLE ====================

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
}