package com.vibey.copycraft.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class MaterialData {
    private BlockState material;
    private float hardness;
    private float explosionResistance;
    private double mass;

    public MaterialData() {
        reset();
    }

    public void setMaterial(BlockState state, double volumeFactor) {
        this.material = state;
        captureProperties(state, volumeFactor);
    }

    private void captureProperties(BlockState state, double volumeFactor) {
        if (state == null || state.isAir()) {
            reset();
            return;
        }

        // Capture hardness
        float destroySpeed = state.getDestroySpeed(null, null);
        this.hardness = destroySpeed < 0 ? -1 : destroySpeed;

        // Capture explosion resistance
        this.explosionResistance = state.getBlock().getExplosionResistance();

        // Capture mass (scaled by volume)
        this.mass = getMassFromVS2(state) * volumeFactor;
    }

    private double getMassFromVS2(BlockState state) {
        // Try to get ACTUAL mass from VS2
        try {
            Class<?> vs2Class = Class.forName("com.vibey.copycraft.vs2.VS2Integration");
            java.lang.reflect.Method method = vs2Class.getMethod("queryBlockMass", BlockState.class);
            double vs2Mass = (double) method.invoke(null, state);
            if (vs2Mass > 0) {
                return vs2Mass;
            }
        } catch (Exception ignored) {
            // VS2 not loaded or API unavailable
        }

        // Fallback estimates
        return estimateMass();
    }

    private double estimateMass() {
        return 100.0;
    }

    private void reset() {
        this.material = Blocks.AIR.defaultBlockState();
        this.hardness = 2.0f;
        this.explosionResistance = 6.0f;
        this.mass = 100.0;
    }

    // Getters
    public BlockState getMaterial() { return material; }
    public float getHardness() { return hardness; }
    public float getExplosionResistance() { return explosionResistance; }
    public double getMass() { return mass; }
    public boolean hasMaterial() { return !material.isAir(); }

    // NBT
    public CompoundTag save(CompoundTag tag) {
        tag.put("Material", NbtUtils.writeBlockState(material));
        tag.putFloat("Hardness", hardness);
        tag.putFloat("Resistance", explosionResistance);
        tag.putDouble("Mass", mass);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.material = NbtUtils.readBlockState(null, tag.getCompound("Material"));
        this.hardness = tag.getFloat("Hardness");
        this.explosionResistance = tag.getFloat("Resistance");
        this.mass = tag.getDouble("Mass");
    }
}
