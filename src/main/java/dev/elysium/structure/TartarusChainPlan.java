package dev.elysium.structure;

import java.util.Optional;
import java.util.function.IntBinaryOperator;

/** One immutable route from a cliff exposure to a vaulted underground anchor. */
public record TartarusChainPlan(int x, int z, int top, int floor, boolean firstEastWest) {
    public static final int CHAMBER_RADIUS = 12;
    public static final int LINK_PITCH = 7;
    public int bottomCenter() { return floor + 11; }

    public static Optional<TartarusChainPlan> find(IntBinaryOperator heights, int x, int z, int floor, boolean axis) {
        int center = heights.applyAsInt(x, z);
        int low = center, high = center;
        for (int dx : new int[]{-3, 0, 3}) for (int dz : new int[]{-3, 0, 3}) {
            int h = heights.applyAsInt(x + dx, z + dz);
            low = Math.min(low, h); high = Math.max(high, h);
        }
        int top = center - 6;
        // The top remains inside the mountain; one outer face can show through
        // a steep slope. Bound the exposed length rather than hanging in a valley.
        if (center < 116 || high - center > 6 || top - low < 8 || top - low > 48) return Optional.empty();
        var plan = new TartarusChainPlan(x, z, top, floor, axis);
        int exposed = 0;
        // Check the actual ring footprint, not diagonal samples where there
        // may be a cliff but no chain. Most of the route remains unexcavated.
        for (int dx=-3;dx<=3;dx++) for (int dz=-3;dz<=3;dz++) {
            if (dx != 0 && dz != 0) continue;
            int surface = heights.applyAsInt(x+dx,z+dz);
            for (int y=Math.max(surface,top-48);y<=top;y++) if (plan.chainAt(dx,y,dz)) exposed++;
        }
        return exposed >= 8 ? Optional.of(plan) : Optional.empty();
    }

    /** Rounded rectangular rings, alternating vertical planes and overlapping in height. */
    public boolean chainAt(int dx, int y, int dz) {
        int first = Math.max(0, Math.floorDiv(y - bottomCenter() - 5, LINK_PITCH));
        for (int index = first; index <= first + 2; index++) {
            int center = bottomCenter() + index * LINK_PITCH;
            if (center + 5 > top) break;
            int dy = Math.abs(y - center);
            if (dy > 5) continue;
            boolean eastWest = firstEastWest ^ (index % 2 != 0);
            int along = Math.abs(eastWest ? dx : dz), across = eastWest ? dz : dx;
            if (across != 0) continue;
            if ((dy <= 3 && along == 3) || (dy == 4 && along >= 2 && along <= 3) || (dy == 5 && along <= 2)) return true;
        }
        return false;
    }

    public int roof(int dx, int dz) {
        double distance = (double) (dx * dx + dz * dz) / (CHAMBER_RADIUS * CHAMBER_RADIUS);
        return floor + 12 + (int) Math.round(10 * Math.sqrt(Math.max(0, 1 - distance)));
    }
}
