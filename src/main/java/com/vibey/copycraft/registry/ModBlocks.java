package com.vibey.copycraft.registry;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.block.CopyBlock;
import com.vibey.copycraft.block.CopyBlockFull;
import com.vibey.copycraft.block.CopyBlockSlab;
import com.vibey.copycraft.block.CopyBlockStairs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CopyCraft.MODID);

    // Original full-size copy block
    public static final RegistryObject<Block> COPY_BLOCK = BLOCKS.register("copy_block",
            () -> new CopyBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
                    //.dynamicShape()));

    // New variants with mass multipliers
    public static final RegistryObject<Block> COPY_BLOCK_FULL = BLOCKS.register("copy_block_full",
            () -> new CopyBlockFull(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
                    //.dynamicShape()));

    public static final RegistryObject<Block> COPY_BLOCK_SLAB = BLOCKS.register("copy_block_slab",
            () -> new CopyBlockSlab(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
                    //.dynamicShape()));

    public static final RegistryObject<Block> COPY_BLOCK_STAIRS = BLOCKS.register("copy_block_stairs",
            () -> new CopyBlockStairs(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
                    //.dynamicShape()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}