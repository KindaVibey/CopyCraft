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
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = CopyCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventsHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<BlockPos> blocksToUpdate = new HashSet<>();

    public static void queueBlockUpdate(BlockPos pos) {
        LOGGER.info("Queueing block update for: {}", pos);
        blocksToUpdate.add(pos.immutable());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !blocksToUpdate.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;

            if (level != null) {
                for (BlockPos pos : blocksToUpdate) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof CopyBlockEntity copyBE) {
                        LOGGER.info("Forcing render update for CopyBlock at: {}", pos);
                        copyBE.requestModelDataUpdate();

                        // Force chunk section re-render
                        mc.levelRenderer.setSectionDirty(
                                pos.getX() >> 4,
                                pos.getY() >> 4,
                                pos.getZ() >> 4
                        );
                    }
                }
                blocksToUpdate.clear();
            }
        }
    }
}