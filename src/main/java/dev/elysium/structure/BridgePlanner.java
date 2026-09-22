package dev.elysium.structure;

import java.util.Arrays;
import java.util.Optional;

/** Pure, bounded geometry. Heights are the first air block above solid terrain, excluding water. */
public final class BridgePlanner {
    public static final int HALF_WIDTH = 2;
    public static final int WIDTH = HALF_WIDTH * 2 + 1;
    public static final int APPROACH_LENGTH = 5;
    public static final int MAX_WATER_SPAN = 48;
    public static final int MAX_LENGTH = MAX_WATER_SPAN + 2 + APPROACH_LENGTH * 2;
    private static final int MAX_SEARCH_DISTANCE = 32;
    private static final int MIN_WATER_SPAN = 4;
    private static final int MAX_WATER_DEPTH = 12;
    private static final int MAX_BANK_HEIGHT = 4;

    private BridgePlanner() {}

    @FunctionalInterface
    public interface Terrain {
        int floorHeight(int x, int z);
    }

    public static Optional<Span> find(Terrain terrain, int centerX, int centerZ, boolean eastWest, int seaLevel) {
        if (terrain.floorHeight(centerX, centerZ) >= seaLevel) return Optional.empty();
        int dx = eastWest ? 1 : 0;
        int dz = eastWest ? 0 : 1;
        int near = findBank(terrain, centerX, centerZ, -dx, -dz, seaLevel);
        int far = findBank(terrain, centerX, centerZ, dx, dz, seaLevel);
        if (near < 0 || far < 0) return Optional.empty();
        int waterSpan = near + far - 1;
        if (waterSpan < MIN_WATER_SPAN || waterSpan > MAX_WATER_SPAN) return Optional.empty();

        int startX = centerX - dx * (near + APPROACH_LENGTH);
        int startZ = centerZ - dz * (near + APPROACH_LENGTH);
        int length = near + far + APPROACH_LENGTH * 2 + 1;
        int[] floors = new int[length * WIDTH];
        for (int along = 0; along < length; along++) {
            for (int across = -HALF_WIDTH; across <= HALF_WIDTH; across++) {
                int floor = terrain.floorHeight(startX + dx * along + dz * across,
                        startZ + dz * along + dx * across);
                // Reject deep ravines and cliffs before committing to a piece.
                if (floor < seaLevel - MAX_WATER_DEPTH || floor > seaLevel + MAX_BANK_HEIGHT) {
                    return Optional.empty();
                }
                floors[along * WIDTH + across + HALF_WIDTH] = floor;
            }
        }

        int startHeight = rowMaximum(floors, 0);
        int endHeight = rowMaximum(floors, length - 1);
        if (Math.abs(startHeight - endHeight) > 3) return Optional.empty();
        int deckHeight = Math.max(seaLevel + 2, Math.max(startHeight, endHeight));
        Span span = new Span(startX, startZ, eastWest, length, startHeight, endHeight, deckHeight, floors);

        for (int along = 0; along < length; along++) {
            for (int across = -HALF_WIDTH; across <= HALF_WIDTH; across++) {
                int floor = span.floorHeight(along, across);
                if (floor > span.walkingHeight(along)) return Optional.empty();
                // Two full-width dry landing rows at both ends, within one block of the deck.
                if (along <= 1 || along >= length - 2) {
                    int bankHeight = along <= 1 ? startHeight : endHeight;
                    if (floor < seaLevel || Math.abs(floor - bankHeight) > 1) return Optional.empty();
                }
            }
        }
        // Check the terrain immediately beyond both exits, not just beneath the bridge itself.
        for (int across = -1; across <= 1; across++) {
            int before = terrain.floorHeight(startX - dx + dz * across, startZ - dz + dx * across);
            int after = terrain.floorHeight(startX + dx * length + dz * across, startZ + dz * length + dx * across);
            if (before < seaLevel || after < seaLevel
                    || Math.abs(before - startHeight) > 1 || Math.abs(after - endHeight) > 1) {
                return Optional.empty();
            }
        }
        return Optional.of(span);
    }

    private static int findBank(Terrain terrain, int x, int z, int dx, int dz, int seaLevel) {
        for (int distance = 1; distance <= MAX_SEARCH_DISTANCE; distance++) {
            int floor = terrain.floorHeight(x + dx * distance, z + dz * distance);
            if (floor >= seaLevel) return floor <= seaLevel + MAX_BANK_HEIGHT ? distance : -1;
            if (floor < seaLevel - MAX_WATER_DEPTH) return -1;
        }
        return -1;
    }

    private static int rowMaximum(int[] floors, int row) {
        int maximum = Integer.MIN_VALUE;
        for (int across = 0; across < WIDTH; across++) maximum = Math.max(maximum, floors[row * WIDTH + across]);
        return maximum;
    }

    /** The exact plan is persisted, so reloading a partially generated structure never resamples terrain. */
    public record Span(int startX, int startZ, boolean eastWest, int length,
                       int startHeight, int endHeight, int deckHeight, int[] floorHeights) {
        public Span {
            if (length < MIN_WATER_SPAN + 2 || length > MAX_LENGTH || floorHeights.length != length * WIDTH) {
                throw new IllegalArgumentException("Invalid Elysian bridge length or terrain profile");
            }
            if (deckHeight < Math.max(startHeight, endHeight)
                    || deckHeight - Math.min(startHeight, endHeight) > APPROACH_LENGTH) {
                throw new IllegalArgumentException("Invalid Elysian bridge approach heights");
            }
            floorHeights = floorHeights.clone();
        }

        @Override
        public int[] floorHeights() { return floorHeights.clone(); }

        public int floorHeight(int along, int across) { return floorHeights[along * WIDTH + across + HALF_WIDTH]; }

        public int walkingHeight(int along) {
            return Math.min(deckHeight, Math.min(startHeight + (along + 1) / 2, endHeight + (length - along) / 2));
        }

        public int x(int along, int across) { return startX + (eastWest ? along : across); }
        public int z(int along, int across) { return startZ + (eastWest ? across : along); }
        public int minimumFloor() { return Arrays.stream(floorHeights).min().orElseThrow(); }
    }
}
