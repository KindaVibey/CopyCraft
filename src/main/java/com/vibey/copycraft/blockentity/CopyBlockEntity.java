package com.vibey.copycraft.blockentity;

import com.vibey.copycraft.client.ClientEventsHandler;
import com.vibey.copycraft.client.CopyBlockModel;
import com.vibey.copycraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class CopyBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private BlockState copiedBlock = Blocks.AIR.defaultBlockState();
    private int virtualRotation = 0; // 0 = Y-axis (normal), 1 = Z-axis, 2 = X-axis

    public CopyBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COPY_BLOCK_ENTITY.get(), pos, blockState);
    }

    public BlockState getCopiedBlock() {
        return copiedBlock;
    }

    public int getVirtualRotation() {
        return virtualRotation;
    }

    public void setCopiedBlock(BlockState newBlock) {
        LOGGER.info("setCopiedBlock called - Current: {}, New: {}",
                copiedBlock.getBlock().getName().getString(),
                newBlock.getBlock().getName().getString());

        // If it's the same block, rotate it
        if (!copiedBlock.isAir() && copiedBlock.getBlock() == newBlock.getBlock()) {
            rotateBlock();
            LOGGER.info("Rotating! New rotation: {}", virtualRotation);
        } else {
            // New block, reset rotation
            this.copiedBlock = newBlock;
            this.virtualRotation = 0;
            LOGGER.info("New block copied!");
        }

        setChanged();

        if (level != null && !level.isClientSide) {
            LOGGER.info("Server sending update packet and forcing chunk update");
            // Mark block for update
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_ALL);
            // Also notify neighbors to ensure render update
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    private void rotateBlock() {
        // Simple 3-axis rotation like logs: Y -> Z -> X -> Y
        virtualRotation = (virtualRotation + 1) % 3;
        LOGGER.info("Virtual rotation changed to: {} ({})",
                virtualRotation,
                virtualRotation == 0 ? "Y-axis" : virtualRotation == 1 ? "Z-axis" : "X-axis");
    }

    public boolean hasCopiedBlock() {
        return !copiedBlock.isAir();
    }

    @NotNull
    @Override
    public ModelData getModelData() {
        ModelData data = ModelData.builder()
                .with(CopyBlockModel.COPIED_STATE, copiedBlock)
                .with(CopyBlockModel.VIRTUAL_ROTATION, virtualRotation)
                .build();

        LOGGER.debug("getModelData returning - Block: {}, Rotation: {}",
                copiedBlock.getBlock().getName().getString(), virtualRotation);

        return data;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("CopiedBlockId")) {
            try {
                String blockId = tag.getString("CopiedBlockId");
                ResourceLocation loc = new ResourceLocation(blockId);
                this.copiedBlock = BuiltInRegistries.BLOCK.get(loc).defaultBlockState();

                // Load block state properties
                if (tag.contains("CopiedBlock")) {
                    try {
                        this.copiedBlock = NbtUtils.readBlockState(
                                level != null ? level.holderLookup(net.minecraft.core.registries.Registries.BLOCK) : null,
                                tag.getCompound("CopiedBlock")
                        );
                    } catch (Exception e) {
                        LOGGER.warn("Failed to read block state properties", e);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load copied block", e);
                this.copiedBlock = Blocks.AIR.defaultBlockState();
            }
        } else if (tag.contains("CopiedBlock")) {
            try {
                this.copiedBlock = NbtUtils.readBlockState(
                        level != null ? level.holderLookup(net.minecraft.core.registries.Registries.BLOCK) : null,
                        tag.getCompound("CopiedBlock")
                );
            } catch (Exception e) {
                LOGGER.error("Failed to load copied block", e);
                this.copiedBlock = Blocks.AIR.defaultBlockState();
            }
        }

        this.virtualRotation = tag.getInt("VirtualRotation");

        LOGGER.info("Loaded block entity - Block: {}, Rotation: {}",
                copiedBlock.getBlock().getName().getString(), virtualRotation);

        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        LOGGER.info("handleUpdateTag called");
        BlockState oldState = this.copiedBlock;
        int oldRotation = this.virtualRotation;

        load(tag);

        // If data changed and we're on client, request render update
        if (level != null && level.isClientSide &&
                (!oldState.equals(this.copiedBlock) || oldRotation != this.virtualRotation)) {
            LOGGER.info("Data changed, requesting model update");
            requestModelDataUpdate();

            // Queue the block for render update
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientEventsHandler.queueBlockUpdate(worldPosition)
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (!copiedBlock.isAir()) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(copiedBlock.getBlock());
            tag.putString("CopiedBlockId", blockId.toString());
            tag.put("CopiedBlock", NbtUtils.writeBlockState(this.copiedBlock));
        }

        tag.putInt("VirtualRotation", virtualRotation);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        LOGGER.info("getUpdateTag - Block: {}, Rotation: {}",
                copiedBlock.getBlock().getName().getString(), virtualRotation);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        LOGGER.info("getUpdatePacket called");
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        LOGGER.info("onDataPacket received on client");
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            BlockState oldState = this.copiedBlock;
            int oldRotation = this.virtualRotation;

            handleUpdateTag(tag);

            // Force model data update and chunk re-render on client
            if (level != null && level.isClientSide) {
                LOGGER.info("Client forcing model data refresh and chunk re-render");
                requestModelDataUpdate();

                // Queue the block for render update
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientEventsHandler.queueBlockUpdate(worldPosition)
                );

                // Mark the block for re-render
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                        Block.UPDATE_ALL_IMMEDIATE);
            }
        }
    }
}