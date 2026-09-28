package dev.elysium.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.Direction;

/** Bounded, deterministic terrain decisions, independent of chunks and block writes. */
public final class LandscapePlanner {
    public static final int MAX_BANK_DISTANCE = 48;
    private static final Direction[] CARDINALS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public interface Terrain {
        /** First air/water block above the solid noise terrain, before structures or vegetation. */
        int height(int x, int z);
        boolean water(int x, int z);
    }

    private LandscapePlanner() {}

    public static OptionalInt pad(Terrain terrain, int x, int z, int radius, int seaLevel, int maxRelief) {
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        // Cheap rejection before the full check. Even a narrow ridge or a single
        // submerged corner must fail the final, every-column footprint check.
        for (int pass = 0; pass < 2; pass++) {
            int step = pass == 0 ? radius : 1;
            for (int dx = -radius; dx <= radius; dx += step) {
                for (int dz = -radius; dz <= radius; dz += step) {
                    int height = terrain.height(x + dx, z + dz);
                    if (height < seaLevel) return OptionalInt.empty();
                    low = Math.min(low, height);
                    high = Math.max(high, height);
                    if (high - low > maxRelief) return OptionalInt.empty();
                }
            }
        }
        return OptionalInt.of(high - 1); // Solid surface of the authored court.
    }

    public static Optional<Direction> outlook(Terrain terrain, int x, int z, int ground, int requiredDrop) {
        Direction best = null;
        int bestDrop = requiredDrop - 1;
        for (Direction direction : CARDINALS) {
            // Both the middle and distant view must fall away: a tiny hole is not a vista.
            int drop = Math.min(ground + 1 - terrain.height(x + direction.getStepX() * 24, z + direction.getStepZ() * 24),
                    ground + 1 - terrain.height(x + direction.getStepX() * 40, z + direction.getStepZ() * 40));
            if (drop > bestDrop) { best = direction; bestDrop = drop; }
        }
        return Optional.ofNullable(best);
    }

    public static Optional<BankPath> bankPath(Terrain terrain, int x, int z, int radius, int ground, int seaLevel) {
        BankPath best = null;
        for (Direction direction : CARDINALS) {
            int dx = direction.getStepX(), dz = direction.getStepZ();
            List<Integer> rows = new ArrayList<>();
            int previous = ground;
            for (int distance = radius + 1; distance <= MAX_BANK_DISTANCE; distance++) {
                int cx = x + dx * distance, cz = z + dz * distance;
                if (terrain.height(cx, cz) < seaLevel) {
                    if (rows.size() >= 4 && previous <= seaLevel + 1 && terrain.water(cx, cz)
                            && terrain.water(cx + dx, cz + dz)) {
                        var candidate = new BankPath(x + dx * (radius + 1), z + dz * (radius + 1), direction,
                                rows.stream().mapToInt(Integer::intValue).toArray());
                        if (best == null || candidate.length() < best.length()) best = candidate;
                    }
                    break;
                }
                int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
                for (int across = -1; across <= 1; across++) {
                    int height = terrain.height(cx - dz * across, cz + dx * across);
                    low = Math.min(low, height);
                    high = Math.max(high, height);
                }
                int surface = high - 1;
                if (low < seaLevel || high - low > 1 || Math.abs(surface - previous) > 1) break;
                rows.add(surface);
                previous = surface;
            }
        }
        return Optional.ofNullable(best);
    }

    public record BankPath(int startX, int startZ, Direction direction, int[] surfaces) {
        public BankPath {
            if (direction.getAxis().isVertical() || surfaces.length < 4 || surfaces.length > MAX_BANK_DISTANCE)
                throw new IllegalArgumentException("Invalid bank path extent");
            surfaces = surfaces.clone();
            for (int i = 1; i < surfaces.length; i++) {
                if (Math.abs(surfaces[i] - surfaces[i - 1]) > 1) throw new IllegalArgumentException("Discontinuous bank path");
            }
        }
        @Override public int[] surfaces() { return surfaces.clone(); }
        public int length() { return surfaces.length; }
        public int surface(int along) { return surfaces[along]; }
        public int x(int along, int across) { return startX + direction.getStepX() * along - direction.getStepZ() * across; }
        public int z(int along, int across) { return startZ + direction.getStepZ() * along + direction.getStepX() * across; }
    }
}
