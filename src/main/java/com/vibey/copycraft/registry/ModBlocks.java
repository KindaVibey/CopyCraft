package com.vibey.copycraft.registry;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.blocks.CopyBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CopyCraft.MODID);

    public static final RegistryObject<Block> COPY_BLOCK = BLOCKS.register("copy_block",
            () -> new CopyBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .dynamicShape()
            )
    );
}