package com.vibey.copycraft.vs2;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.block.CopyBlockVariant;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
import kotlin.Pair;
import kotlin.Triple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.core.apigame.world.chunks.BlockType;
import org.valkyrienskies.mod.common.BlockStateInfo;
import org.valkyrienskies.mod.common.BlockStateInfoProvider;
import org.valkyrienskies.physics_api.voxel.Lod1LiquidBlockState;
import org.valkyrienskies.physics_api.voxel.Lod1SolidBlockState;

import java.util.Collections;
import java.util.List;

public class CopyCraftWeights implements BlockStateInfoProvider {
    public static final CopyCraftWeights INSTANCE = new CopyCraftWeights();

    // Made package-private so AlternativeVSMassContextMixin can access them
    static final ThreadLocal<Level> currentLevel = new ThreadLocal<>();
    static final ThreadLocal<BlockPos> currentPos = new ThreadLocal<>();

    public static void setContext(Level level, BlockPos pos) {
        currentLevel.set(level);
        currentPos.set(pos);
    }

    public static void clearContext() {
        currentLevel.remove();
        currentPos.remove();
    }

    @Override
    public int getPriority() {
        // CRITICAL: Must be higher than VS's default priority (100)
        return 1000;
    }

    @Nullable
    @Override
    public Double getBlockStateMass(BlockState blockState) {
        System.out.println("[CopyCraft VS] getBlockStateMass called for: " + blockState);

        if (!(blockState.getBlock() instanceof CopyBlockVariant copyBlockVariant)) {
            System.out.println("[CopyCraft VS] Not a CopyBlockVariant");
            return null;
        }

        Level level = currentLevel.get();
        BlockPos pos = currentPos.get();

        if (level == null || pos == null) {
            System.out.println("[CopyCraft VS] ERROR: No context! level=" + level + " pos=" + pos);
            return null;
        }

        System.out.println("[CopyCraft VS] Looking up block entity at " + pos);
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof CopyBlockEntity copyBE)) {
            System.out.println("[CopyCraft VS] ERROR: Wrong block entity type: " + be);
            return null;
        }

        BlockState copiedState = copyBE.getCopiedBlock();
        System.out.println("[CopyCraft VS] Copied state: " + copiedState);

        if (copiedState.isAir()) {
            System.out.println("[CopyCraft VS] Copied state is AIR, using default mass");
            return null; // Let VS use default mass
        }

        // Get the copied block's mass from VS
        Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(copiedState);
        if (info == null || info.getFirst() == null) {
            System.out.println("[CopyCraft VS] No VS info for copied block, using default");
            return null;
        }

        Double copiedMass = info.getFirst();
        float multiplier = copyBlockVariant.getMassMultiplier();
        double finalMass = copiedMass * multiplier;

        System.out.println("[CopyCraft VS] SUCCESS: " + copiedState + " mass=" + copiedMass +
                " x " + multiplier + " = " + finalMass + " kg");

        return finalMass;
    }

    @Nullable
    @Override
    public BlockType getBlockStateType(BlockState blockState) {
        if (!(blockState.getBlock() instanceof CopyBlockVariant)) {
            return null;
        }

        Level level = currentLevel.get();
        BlockPos pos = currentPos.get();

        if (level == null || pos == null) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CopyBlockEntity copyBE)) {
            return null;
        }

        BlockState copiedState = copyBE.getCopiedBlock();
        if (copiedState.isAir()) {
            return null;
        }

        Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(copiedState);
        if (info != null) {
            BlockType type = info.getSecond();
            System.out.println("[CopyCraft VS] Block type for " + copiedState + ": " + type);
            return type;
        }

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

    // Dummy methods for compatibility - the simplified version doesn't use caching
    public static void invalidateCache(BlockPos pos) {
        // No-op in simplified version
    }

    public static void cleanCache() {
        // No-op in simplified version
    }

    public static void register() {
        try {
            Registry.register(
                    BlockStateInfo.INSTANCE.getREGISTRY(),
                    new ResourceLocation(CopyCraft.MODID, "copycraft_weights"),
                    INSTANCE
            );
            System.out.println("[CopyCraft VS] Successfully registered VS2 weights provider with priority 1000!");
        } catch (Exception e) {
            System.err.println("[CopyCraft VS] Failed to register weights provider:");
            e.printStackTrace();
        }
    }
}