package com.vibey.copycraft;

import com.vibey.copycraft.registry.ModBlockEntities;
import com.vibey.copycraft.registry.ModBlocks;
import com.vibey.copycraft.registry.ModItems;
import com.vibey.copycraft.vs2.VS2Integration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CopyCraft.MODID)
public class CopyCraft {
    public static final String MODID = "copycraft";

    public CopyCraft() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);

        ModItems.ITEMS.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        VS2Integration.init();
    }
}
