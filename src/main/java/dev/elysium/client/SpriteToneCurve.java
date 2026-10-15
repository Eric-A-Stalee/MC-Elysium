package dev.elysium.client;

import java.util.function.IntUnaryOperator;

/** A reload-time tonal lift. NativeImage stores pixels as ABGR, including alpha. */
public final class SpriteToneCurve implements IntUnaryOperator {
    private final int[] channels = new int[256];

    public SpriteToneCurve(double gamma) {
        if (!Double.isFinite(gamma) || gamma < 0.2 || gamma > 2.0) {
            throw new IllegalArgumentException("Sprite gamma must be between 0.2 and 2.0");
        }
        for (int value = 0; value < channels.length; value++) {
            channels[value] = (int) Math.round(255 * Math.pow(value / 255.0, gamma));
        }
    }

    @Override
    public int applyAsInt(int abgr) {
        // Preserve invisible texels verbatim, including their RGB padding.
        if ((abgr >>> 24) == 0) return abgr;
        return (abgr & 0xFF000000)
                | channels[(abgr >>> 16) & 255] << 16
                | channels[(abgr >>> 8) & 255] << 8
                | channels[abgr & 255];
    }
}
