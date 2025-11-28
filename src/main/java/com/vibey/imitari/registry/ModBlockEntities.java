package com.vibey.imitari.registry;

import com.vibey.imitari.Imitari;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Imitari.MODID);

    // CRITICAL FIX: Each block needs to be listed in EVERY registration, OR use separate registrations
    // Going with single registration that supports ALL blocks (this is valid)
    public static final RegistryObject<BlockEntityType<CopyBlockEntity>> COPY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("copy_block_entity", () ->
                    BlockEntityType.Builder.of(CopyBlockEntity::new,
                            ModBlocks.COPY_BLOCK.get(),
                            ModBlocks.COPY_BLOCK_GHOST.get(),
                            ModBlocks.COPY_BLOCK_SLAB.get(),
                            ModBlocks.COPY_BLOCK_STAIRS.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        System.out.println("Registered CopyCraft block entities!");
    }
}