package com.vibey.copycraft.client;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = CopyCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventsHandler {
    private static final Set<BlockPos> blocksToUpdate = new HashSet<>();

    public static void queueBlockUpdate(BlockPos pos) {
        blocksToUpdate.add(pos.immutable());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !blocksToUpdate.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;

            if (level != null && mc.levelRenderer != null) {
                for (BlockPos pos : blocksToUpdate) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof CopyBlockEntity copyBE) {
                        copyBE.requestModelDataUpdate();

                        // Force chunk section re-render - mark ALL nearby sections dirty
                        int chunkX = pos.getX() >> 4;
                        int chunkY = pos.getY() >> 4;
                        int chunkZ = pos.getZ() >> 4;

                        // Mark the section and all adjacent sections as dirty
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                for (int dz = -1; dz <= 1; dz++) {
                                    mc.levelRenderer.setSectionDirty(
                                            chunkX + dx,
                                            chunkY + dy,
                                            chunkZ + dz
                                    );
                                }
                            }
                        }
                    }
                }
                blocksToUpdate.clear();
            }
        }
    }
}