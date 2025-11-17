package com.vibey.copycraft.registry;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CopyCraft.MODID);

    public static final RegistryObject<BlockEntityType<CopyBlockEntity>> COPY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("copy_block_entity", () ->
                    BlockEntityType.Builder.of(CopyBlockEntity::new,
                            ModBlocks.COPY_BLOCK.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<CopyBlockEntity>> COPY_BLOCK_FULL_ENTITY =
            BLOCK_ENTITIES.register("copy_block_full_entity", () ->
                    BlockEntityType.Builder.of(CopyBlockEntity::new,
                            ModBlocks.COPY_BLOCK_FULL.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<CopyBlockEntity>> COPY_BLOCK_SLAB_ENTITY =
            BLOCK_ENTITIES.register("copy_block_slab_entity", () ->
                    BlockEntityType.Builder.of(CopyBlockEntity::new,
                            ModBlocks.COPY_BLOCK_SLAB.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<CopyBlockEntity>> COPY_BLOCK_STAIRS_ENTITY =
            BLOCK_ENTITIES.register("copy_block_stairs_entity", () ->
                    BlockEntityType.Builder.of(CopyBlockEntity::new,
                            ModBlocks.COPY_BLOCK_STAIRS.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}