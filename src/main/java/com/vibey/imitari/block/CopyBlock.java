package com.vibey.imitari.block;

import com.vibey.imitari.block.base.CopyBlockBase;

/**
 * Legacy CopyBlock class for backwards compatibility.
 * Just extends the base implementation with default settings.
 */
public class CopyBlock extends CopyBlockBase {
    public CopyBlock(Properties properties) {
        super(properties, 1.0f);
    }
}