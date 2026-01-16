package com.vibey.imitari.registry;

import com.vibey.imitari.Imitari;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import com.vibey.imitari.integration.ModIntegrations;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Imitari.MODID);

    public static final RegistryObject<BlockEntityType<CopyBlockEntity>> COPY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("copy_block_entity", () -> {
                // Build list of blocks that use this BlockEntity
                List<Block> blocks = new ArrayList<>();

                // Add core blocks
                blocks.add(ModBlocks.COPY_BLOCK.get());
                blocks.add(ModBlocks.COPY_BLOCK_GHOST.get());
                blocks.add(ModBlocks.COPY_BLOCK_SLAB.get());
                blocks.add(ModBlocks.COPY_BLOCK_STAIRS.get());
                blocks.add(ModBlocks.COPY_BLOCK_LAYER.get());
                blocks.add(ModBlocks.COPY_BLOCK_FENCE.get());
                blocks.add(ModBlocks.COPY_BLOCK_FENCE_GATE.get());
                blocks.add(ModBlocks.COPY_BLOCK_WALL.get());
                blocks.add(ModBlocks.COPY_BLOCK_DOOR.get());
                blocks.add(ModBlocks.COPY_BLOCK_IRON_DOOR.get());
                blocks.add(ModBlocks.COPY_BLOCK_BUTTON.get());
                blocks.add(ModBlocks.COPY_BLOCK_LEVER.get());
                blocks.add(ModBlocks.COPY_BLOCK_TRAPDOOR.get());
                blocks.add(ModBlocks.COPY_BLOCK_IRON_TRAPDOOR.get());
                blocks.add(ModBlocks.COPY_BLOCK_PANE.get());
                blocks.add(ModBlocks.COPY_BLOCK_PRESSURE_PLATE.get());
                blocks.add(ModBlocks.COPY_BLOCK_LADDER.get());

                // Add Clockwork integration blocks if they exist
                if (ModIntegrations.CLOCKWORK_LOADED && ModIntegrations.VS2_LOADED) {
                    if (ModIntegrations.COPY_BLOCK_WING_CW != null) {
                        blocks.add(ModIntegrations.COPY_BLOCK_WING_CW.get());
                    }
                    if (ModIntegrations.COPY_BLOCK_FLAP_CW != null) {
                        blocks.add(ModIntegrations.COPY_BLOCK_FLAP_CW.get());
                    }
                }

                return BlockEntityType.Builder.of(
                        CopyBlockEntity::new,
                        blocks.toArray(new Block[0])
                ).build(null);
            });

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}