package com.vibey.imitari.integration;

import com.mojang.logging.LogUtils;
import com.vibey.imitari.Imitari;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

public class ModIntegrations {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Imitari.MODID);

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Imitari.MODID);


    public static final boolean CLOCKWORK_LOADED = ModList.get().isLoaded("vs_clockwork");
    public static final boolean VS2_LOADED = ModList.get().isLoaded("valkyrienskies");
    public static final boolean WARIUMVS_LOADED = ModList.get().isLoaded("valkyrien_warium");


    public static RegistryObject<Block> COPY_BLOCK_WING_CW = null;
    public static RegistryObject<Block> COPY_BLOCK_FLAP_CW = null;
    public static RegistryObject<Item> COPY_BLOCK_WING_CW_ITEM = null;
    public static RegistryObject<Item> COPY_BLOCK_FLAP_CW_ITEM = null;

    public static RegistryObject<Block> COPY_BLOCK_WING_VW = null;
    public static RegistryObject<Item> COPY_BLOCK_WING_VW_ITEM = null;

    public static void register(IEventBus modEventBus) {
        if (CLOCKWORK_LOADED && VS2_LOADED) {
            LOGGER.info("Clockwork detected! Registering CopyBlock wing variants...");
            registerClockworkBlocks();
        }

        if (WARIUMVS_LOADED && VS2_LOADED) {
            LOGGER.info("Valkyrien Warium detected! Registering CopyBlock wing variants...");
            registerWariumBlocks();
        }

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        LOGGER.info("Mod integrations initialized (Clockwork: {}, VS2: {}, Warium: {})",
                CLOCKWORK_LOADED, VS2_LOADED, WARIUMVS_LOADED);
    }

    private static void registerClockworkBlocks() {
        try {
            Class<?> wingClass = Class.forName("com.vibey.imitari.block.extras.clockwork.CopyBlockWingCW");
            Class<?> flapClass = Class.forName("com.vibey.imitari.block.extras.clockwork.CopyBlockFlapCW");

            COPY_BLOCK_WING_CW = BLOCKS.register("copy_block_wing_cw", () -> {
                try {
                    return (Block) wingClass
                            .getConstructor(BlockBehaviour.Properties.class)
                            .newInstance(BlockBehaviour.Properties.of()
                                    .strength(0.5F)
                                    .sound(SoundType.WOOD)
                                    .noOcclusion());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create CopyBlockWingCW", e);
                }
            });

            COPY_BLOCK_FLAP_CW = BLOCKS.register("copy_block_flap_cw", () -> {
                try {
                    return (Block) flapClass
                            .getConstructor(BlockBehaviour.Properties.class)
                            .newInstance(BlockBehaviour.Properties.of()
                                    .strength(0.5F)
                                    .sound(SoundType.WOOD)
                                    .noOcclusion());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create CopyBlockFlapCW", e);
                }
            });

            COPY_BLOCK_WING_CW_ITEM = ITEMS.register("copy_block_wing_cw",
                    () -> new net.minecraft.world.item.BlockItem(COPY_BLOCK_WING_CW.get(), new Item.Properties()));

            COPY_BLOCK_FLAP_CW_ITEM = ITEMS.register("copy_block_flap_cw",
                    () -> new net.minecraft.world.item.BlockItem(COPY_BLOCK_FLAP_CW.get(), new Item.Properties()));

            LOGGER.info("Successfully registered {} Clockwork integration blocks", 2);

        } catch (ClassNotFoundException e) {
            LOGGER.error("Clockwork is loaded but CopyBlock wing classes not found!", e);
        } catch (Exception e) {
            LOGGER.error("Failed to register Clockwork integration blocks", e);
        }
    }

    private static void registerWariumBlocks() {
        try {
            Class<?> wingClass = Class.forName("com.vibey.imitari.block.extras.warium.CopyBlockWingVW");

            COPY_BLOCK_WING_VW = BLOCKS.register("copy_block_wing_vw", () -> {
                try {
                    return (Block) wingClass
                            .getConstructor(BlockBehaviour.Properties.class)
                            .newInstance(BlockBehaviour.Properties.of()
                                    .strength(4.0F, 4.0F)
                                    .sound(SoundType.WOOD)
                                    .noOcclusion());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create CopyBlockWingVW", e);
                }
            });

            COPY_BLOCK_WING_VW_ITEM = ITEMS.register("copy_block_wing_vw",
                    () -> new net.minecraft.world.item.BlockItem(COPY_BLOCK_WING_VW.get(), new Item.Properties()));

            LOGGER.info("Successfully registered {} Valkyrien Warium integration blocks", 1);

        } catch (ClassNotFoundException e) {
            LOGGER.error("Valkyrien Warium is loaded but CopyBlock wing classes not found!", e);
        } catch (Exception e) {
            LOGGER.error("Failed to register Valkyrien Warium integration blocks", e);
        }
    }

    public static void addToClockworkTab(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
        if (!CLOCKWORK_LOADED) return;

        try {
            net.minecraft.resources.ResourceKey<CreativeModeTab> clockworkTab =
                    net.minecraft.resources.ResourceKey.create(
                            Registries.CREATIVE_MODE_TAB,
                            new net.minecraft.resources.ResourceLocation("vs_clockwork", "clockwork_physicalities")
                    );

            if (event.getTabKey().equals(clockworkTab)) {
                if (COPY_BLOCK_WING_CW_ITEM != null) {
                    event.accept(COPY_BLOCK_WING_CW_ITEM.get());
                }
                if (COPY_BLOCK_FLAP_CW_ITEM != null) {
                    event.accept(COPY_BLOCK_FLAP_CW_ITEM.get());
                }
                LOGGER.info("Added Imitari wing blocks to Clockwork creative tab");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to add blocks to Clockwork creative tab", e);
        }
    }

    public static void addToWariumTab(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
        if (!WARIUMVS_LOADED) return;

        try {
            net.minecraft.resources.ResourceKey<CreativeModeTab> wariumTab =
                    net.minecraft.resources.ResourceKey.create(
                            Registries.CREATIVE_MODE_TAB,
                            new net.minecraft.resources.ResourceLocation("valkyrien_warium", "warium_aerodynamics")
                    );

            if (event.getTabKey().equals(wariumTab)) {
                if (COPY_BLOCK_WING_VW_ITEM != null) {
                    event.accept(COPY_BLOCK_WING_VW_ITEM.get());
                }
                LOGGER.info("Added Imitari wing block to Warium aerodynamics creative tab");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to add blocks to Warium creative tab", e);
        }
    }

    public static void registerWithImitariSystems() {
        if (CLOCKWORK_LOADED && VS2_LOADED) {
            try {
                if (COPY_BLOCK_WING_CW != null) {
                    com.vibey.imitari.api.CopyBlockAPI.registerCopyBlock(COPY_BLOCK_WING_CW.get());
                }
                if (COPY_BLOCK_FLAP_CW != null) {
                    com.vibey.imitari.api.CopyBlockAPI.registerCopyBlock(COPY_BLOCK_FLAP_CW.get());
                }

                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> {
                            if (COPY_BLOCK_WING_CW != null) {
                                com.vibey.imitari.client.CopyBlockModelProvider.registerBlock(COPY_BLOCK_WING_CW.get());
                            }
                            if (COPY_BLOCK_FLAP_CW != null) {
                                com.vibey.imitari.client.CopyBlockModelProvider.registerBlock(COPY_BLOCK_FLAP_CW.get());
                            }
                        }
                );

                LOGGER.info("Registered Clockwork blocks with Imitari systems");
            } catch (Exception e) {
                LOGGER.error("Failed to register Clockwork blocks with Imitari", e);
            }
        }

        if (WARIUMVS_LOADED && VS2_LOADED) {
            try {
                if (COPY_BLOCK_WING_VW != null) {
                    com.vibey.imitari.api.CopyBlockAPI.registerCopyBlock(COPY_BLOCK_WING_VW.get());
                }

                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> {
                            if (COPY_BLOCK_WING_VW != null) {
                                com.vibey.imitari.client.CopyBlockModelProvider.registerBlock(COPY_BLOCK_WING_VW.get());
                            }
                        }
                );

                LOGGER.info("Registered Valkyrien Warium blocks with Imitari systems");
            } catch (Exception e) {
                LOGGER.error("Failed to register Valkyrien Warium blocks with Imitari", e);
            }
        }
    }
}
