package com.vibey.imitari.block;

/**
 * Base class for CopyBlock variants with different mass multipliers.
 * Now simply extends CopyBlockBase with a custom multiplier.
 *
 * @deprecated Use CopyBlockBase directly instead
 */
@Deprecated
public abstract class CopyBlockVariant extends CopyBlockBase {
    public CopyBlockVariant(Properties properties, float massMultiplier) {
        super(properties, massMultiplier);
    }
}