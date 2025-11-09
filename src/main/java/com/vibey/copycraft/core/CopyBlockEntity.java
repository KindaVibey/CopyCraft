package com.vibey.copycraft.core;

import com.vibey.copycraft.blocks.CopyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopyBlockEntity extends BlockEntity {

    private BlockState material = Blocks.AIR.defaultBlockState();
    private ItemStack consumedItem = ItemStack.EMPTY;
    private double volumeFactor = 1.0;

    public CopyBlockEntity(BlockPos pos, BlockState state) {
        super(com.vibey.copycraft.registry.ModBlockEntities.COPY_BLOCK.get(), pos, state);
    }

    /**
     * Set the material with volume factor
     */
    public void setMaterial(BlockState material, ItemStack consumed, double volumeFactor) {
        this.material = material;
        this.consumedItem = consumed.copy();
        this.volumeFactor = volumeFactor;

        setChanged();

        // CRITICAL: Request model data update for rendering
        if (level != null) {
            if (!level.isClientSide) {
                // Server: Send update to client
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            } else {
                // Client: Request chunk re-render
                level.getModelDataManager().requestRefresh(this);
            }
        }
    }

    public void clearMaterial() {
        this.material = Blocks.AIR.defaultBlockState();
        this.consumedItem = ItemStack.EMPTY;
        this.volumeFactor = 1.0;

        setChanged();

        if (level != null) {
            if (!level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            } else {
                level.getModelDataManager().requestRefresh(this);
            }
        }
    }

    // ========== Model Data for Rendering ==========

    @Override
    @NotNull
    public ModelData getModelData() {
        if (hasMaterial()) {
            return ModelData.builder()
                    .with(CopyBlock.MATERIAL_PROPERTY, material)
                    .build();
        }
        return ModelData.EMPTY;
    }

    // ========== Getters ==========

    public BlockState getMaterial() {
        return material;
    }

    public ItemStack getConsumedItem() {
        return consumedItem;
    }

    public boolean hasMaterial() {
        return material != null && !material.isAir();
    }

    public double getVolumeFactor() {
        return volumeFactor;
    }

    /**
     * Calculate mass for VS2 integration
     */
    public double getMass() {
        if (!hasMaterial()) {
            return 100.0; // Default mass
        }

        // Try to get VS2 mass
        try {
            Class<?> vs2Integration = Class.forName("com.vibey.copycraft.vs2.VS2Integration");
            java.lang.reflect.Method queryMass = vs2Integration.getMethod("queryBlockMass", BlockState.class);
            double vs2Mass = (double) queryMass.invoke(null, material);

            if (vs2Mass > 0) {
                return vs2Mass * volumeFactor;
            }
        } catch (Exception e) {
            // VS2 not loaded or failed
        }

        // Fallback: estimate from hardness
        float hardness = material.getDestroySpeed(null, null);
        double baseMass;

        if (hardness < 0) baseMass = 1000.0;
        else if (hardness == 0) baseMass = 1.0;
        else if (hardness < 0.5) baseMass = 10.0;
        else if (hardness < 2.0) baseMass = 50.0;
        else if (hardness < 5.0) baseMass = 100.0;
        else baseMass = 200.0;

        return baseMass * volumeFactor;
    }

    // ========== NBT ==========

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Material", NbtUtils.writeBlockState(material));
        tag.put("ConsumedItem", consumedItem.save(new CompoundTag()));
        tag.putDouble("VolumeFactor", volumeFactor);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        material = NbtUtils.readBlockState(null, tag.getCompound("Material"));
        consumedItem = ItemStack.of(tag.getCompound("ConsumedItem"));
        volumeFactor = tag.getDouble("VolumeFactor");
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