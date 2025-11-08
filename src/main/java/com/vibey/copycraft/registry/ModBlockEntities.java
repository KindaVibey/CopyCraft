package com.vibey.copycraft.registry;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.core.CopyBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CopyCraft.MODID);

    public static final RegistryObject<BlockEntityType<CopyBlockEntity>> COPY_BLOCK =
            BLOCK_ENTITIES.register("copy_block", () ->
                    BlockEntityType.Builder.<CopyBlockEntity>of(
                            CopyBlockEntity::new,
                            ModBlocks.COPY_BLOCK.get()
                    ).build(null)
            );
}