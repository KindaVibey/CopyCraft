package com.vibey.copycraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CopyBlockEntity extends BlockEntity {

    private final MaterialData materialData = new MaterialData();
    private ItemStack consumedItem = ItemStack.EMPTY;

    // Constructor that matches what BlockEntityType.Builder expects
    public CopyBlockEntity(BlockPos pos, BlockState state) {
        super(com.vibey.copycraft.registry.ModBlockEntities.COPY_BLOCK.get(), pos, state);
    }

    /**
     * Set the material with volume factor
     * @param material The block state to copy
     * @param consumed The item that was consumed
     * @param volumeFactor How much of a full block this is (1.0 = full, 0.5 = slab, etc.)
     */
    public void setMaterial(BlockState material, ItemStack consumed, double volumeFactor) {
        this.materialData.setMaterial(material, volumeFactor);
        this.consumedItem = consumed.copy();
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void clearMaterial() {
        this.materialData.setMaterial(null, 1.0);
        this.consumedItem = ItemStack.EMPTY;
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public MaterialData getMaterialData() { return materialData; }
    public BlockState getMaterial() { return materialData.getMaterial(); }
    public ItemStack getConsumedItem() { return consumedItem; }
    public boolean hasMaterial() { return materialData.hasMaterial(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        materialData.save(tag);
        tag.put("ConsumedItem", consumedItem.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        materialData.load(tag);
        consumedItem = ItemStack.of(tag.getCompound("ConsumedItem"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
