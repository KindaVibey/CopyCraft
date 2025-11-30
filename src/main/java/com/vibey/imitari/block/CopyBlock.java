package com.vibey.imitari.block;

/**
 * Legacy CopyBlock class for backwards compatibility.
 * Just extends the base implementation with default settings.
 */
public class CopyBlock extends CopyBlockBase {
    public CopyBlock(Properties properties) {
        super(properties, 1.0f);
    }
}