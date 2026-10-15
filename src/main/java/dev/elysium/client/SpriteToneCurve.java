package dev.elysium.client;

import java.util.function.IntUnaryOperator;

/** Reload-time value/contrast adjustment; NativeImage stores pixels as ABGR. */
public final class SpriteToneCurve implements IntUnaryOperator {
    private final int[] channels = new int[256];

    public SpriteToneCurve(double gamma) {
        this(gamma, 1.0, 0);
    }

    public SpriteToneCurve(double gamma, double contrast, int pivot) {
        if (!Double.isFinite(gamma) || gamma < 0.2 || gamma > 2.0) {
            throw new IllegalArgumentException("Sprite gamma must be between 0.2 and 2.0");
        }
        if (!Double.isFinite(contrast) || contrast < 0 || contrast > 4 || pivot < 0 || pivot > 255) {
            throw new IllegalArgumentException("Sprite contrast must be 0..4 and pivot 0..255");
        }
        for (int value = 0; value < channels.length; value++) {
            // Keep the two rounding steps used in the approved actual-sprite preview.
            long lifted = Math.round(255 * Math.pow(value / 255.0, gamma));
            channels[value] = (int) Math.clamp(Math.round(pivot + contrast * (lifted - pivot)), 0, 255);
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
