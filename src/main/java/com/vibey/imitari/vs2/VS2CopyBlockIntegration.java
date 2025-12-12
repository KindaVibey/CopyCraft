package com.vibey.imitari.vs2;

import com.vibey.imitari.Imitari;
import com.vibey.imitari.block.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import kotlin.Pair;
import kotlin.Triple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.core.apigame.world.ShipWorldCore;
import org.valkyrienskies.core.apigame.world.chunks.BlockType;
import org.valkyrienskies.mod.common.BlockStateInfo;
import org.valkyrienskies.mod.common.BlockStateInfoProvider;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.physics_api.voxel.Lod1LiquidBlockState;
import org.valkyrienskies.physics_api.voxel.Lod1SolidBlockState;

import java.util.Collections;
import java.util.List;

/**
 * VS2 Integration using onSetBlock for dynamic mass updates.
 *
 * This is the clean, proper way to handle dynamic mass in VS2:
 * 1. We implement BlockStateInfoProvider (optional, for future use)
 * 2. We use ShipWorldCore.onSetBlock() to notify VS2 of mass changes
 * 3. No BlockState properties needed - mass is tracked by VS2 internally
 */
public class VS2CopyBlockIntegration implements BlockStateInfoProvider {
    public static final VS2CopyBlockIntegration INSTANCE = new VS2CopyBlockIntegration();

    private VS2CopyBlockIntegration() {}

    @Override
    public int getPriority() {
        return 200; // Higher than VS default (100)
    }

    /**
     * We don't need to provide mass here since we use onSetBlock.
     * Return null to let VS2 use its default behavior.
     */
    @Nullable
    @Override
    public Double getBlockStateMass(BlockState blockState) {
        return null;
    }

    @Nullable
    @Override
    public BlockType getBlockStateType(BlockState blockState) {
        return null;
    }

    @Override
    public List<Lod1SolidBlockState> getSolidBlockStates() {
        return Collections.emptyList();
    }

    @Override
    public List<Lod1LiquidBlockState> getLiquidBlockStates() {
        return Collections.emptyList();
    }

    @Override
    public List<Triple<Integer, Integer, Integer>> getBlockStateData() {
        return Collections.emptyList();
    }

    /**
     * Call this when a CopyBlock's copied block changes.
     * Uses ShipWorldCore.onSetBlock() to notify VS2 of the mass change.
     *
     * This is the KEY method - it tells VS2: "block at X,Y,Z changed mass from A to B"
     */
    public static void updateCopyBlockMass(Level level, BlockPos pos, BlockState copyBlockState) {
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CopyBlockEntity copyBE)) return;
        if (!(copyBlockState.getBlock() instanceof ICopyBlock copyBlock)) return;

        BlockState copiedState = copyBE.getCopiedBlock();

        // Calculate old mass (assume empty before)
        double oldMass = 10.0;

        // Calculate new mass based on copied block
        double newMass;
        if (copiedState.isAir()) {
            newMass = 10.0; // Empty frame
        } else {
            try {
                Pair<Double, BlockType> blockInfo = BlockStateInfo.INSTANCE.get(copiedState);
                if (blockInfo != null && blockInfo.getFirst() != null) {
                    double copiedMass = blockInfo.getFirst();
                    float multiplier = copyBlock.getMassMultiplier();
                    newMass = copiedMass * multiplier;

                    System.out.println("[Imitari VS2] Copying " +
                            copiedState.getBlock().getName().getString() +
                            ": " + copiedMass + "kg × " + multiplier + " = " + newMass + "kg");
                } else {
                    newMass = 50.0 * copyBlock.getMassMultiplier();
                    System.out.println("[Imitari VS2] No VS2 data for " +
                            copiedState.getBlock().getName().getString() +
                            ", using fallback: " + newMass + "kg");
                }
            } catch (Exception e) {
                newMass = 50.0 * copyBlock.getMassMultiplier();
                System.err.println("[Imitari VS2] Error getting mass, using fallback: " + newMass + "kg");
                e.printStackTrace();
            }
        }

        // Get BlockType from VS2
        BlockType blockType;
        try {
            Pair<Double, BlockType> copyBlockInfo = BlockStateInfo.INSTANCE.get(copyBlockState);
            if (copyBlockInfo != null && copyBlockInfo.getSecond() != null) {
                blockType = copyBlockInfo.getSecond();
            } else {
                System.err.println("[Imitari VS2] Could not get BlockType for CopyBlock, skipping update");
                return;
            }
        } catch (Exception e) {
            System.err.println("[Imitari VS2] Error getting BlockType: " + e.getMessage());
            return;
        }

        // Get dimension ID
        String dimensionId = serverLevel.dimension().location().toString();

        // Notify VS2 about the mass change using onSetBlock
        try {
            ShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(serverLevel);

            if (shipWorld != null) {
                // This is the magic: tell VS2 the block's mass changed
                shipWorld.onSetBlock(
                        pos.getX(), pos.getY(), pos.getZ(),
                        dimensionId,
                        blockType,      // oldBlockType (same block)
                        blockType,      // newBlockType (same block)
                        oldMass,        // oldBlockMass
                        newMass         // newBlockMass
                );

                System.out.println("[Imitari VS2] ✓ Called onSetBlock at " + pos +
                        ": " + oldMass + "kg → " + newMass + "kg");
            } else {
                System.out.println("[Imitari VS2] No ShipWorld found (block not on a ship yet)");
            }
        } catch (Exception e) {
            System.err.println("[Imitari VS2] Error calling onSetBlock:");
            e.printStackTrace();
        }
    }

    public static void register() {
        try {
            Registry.register(
                    BlockStateInfo.INSTANCE.getREGISTRY(),
                    new ResourceLocation(Imitari.MODID, "copyblock_dynamic_mass"),
                    INSTANCE
            );
            System.out.println("[Imitari VS2] ✓ Registered BlockStateInfoProvider with priority 200!");
        } catch (Exception e) {
            System.err.println("[Imitari VS2] Failed to register:");
            e.printStackTrace();
        }
    }
}