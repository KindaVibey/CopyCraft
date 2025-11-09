package com.vibey.copycraft.blocks;

import com.vibey.copycraft.core.CopyBlockEntity;
import com.vibey.copycraft.core.ICopyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopyBlock extends Block implements EntityBlock, ICopyBlock {

    // ModelData property to pass material to the renderer
    public static final ModelProperty<BlockState> MATERIAL_PROPERTY = new ModelProperty<>();

    public CopyBlock(Properties properties) {
        super(properties);
    }

    // ========== ICopyBlock Implementation ==========

    @Override
    public double getVolumeFactor(BlockState state) {
        return 1.0; // Full block
    }

    // ========== Block Entity ==========

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopyBlockEntity(pos, state);
    }

    // Note: Model data is provided by the BlockEntity, not the Block
    // The BlockEntity overrides getModelData() instead

    // ========== Rendering ==========

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Use MODEL rendering (the default) - this tells Minecraft to use the baked model system
        return RenderShape.MODEL;
    }

    // ========== Interactions ==========

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack stack = player.getItemInHand(hand);

        if (applyMaterial(level, pos, player, hand, stack)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ========== Dynamic Properties ==========

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float progress = getCopyBlockDestroyProgress(state, player, level, pos);
        return progress >= 0 ? progress : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        float resistance = getCopyBlockExplosionResistance(state, level, pos, explosion);
        return resistance >= 0 ? resistance : super.getExplosionResistance(state, level, pos, explosion);
    }

    // ========== Light Emission ==========

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CopyBlockEntity be && be.hasMaterial()) {
            return be.getMaterial().getLightEmission();
        }
        return super.getLightEmission(state, level, pos);
    }

    // ========== Collision & Shape ==========

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Full block shape - override in subclasses for slabs, stairs, etc.
        return Shapes.block();
    }

    // ========== Cleanup ==========

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof CopyBlockEntity be && be.hasMaterial()) {
                Block.popResource(level, pos, be.getConsumedItem());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isCreative() && level.getBlockEntity(pos) instanceof CopyBlockEntity be) {
            be.clearMaterial();
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}