package dev.elysium.forge;

import dev.elysium.block.ForgeServiceBlock;
import dev.elysium.block.ForgeServiceBlockEntity;
import dev.elysium.structure.ForgeFixtures;
import dev.elysium.structure.ForgePlan;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Shared, per-foundry service power. No chunk tickets, continuous scans or main-hearth activation. */
public final class ForgeLighting extends SavedData {
    public static final int CIRCUITS = 3;
    private final Map<Long, Integer> circuits = new HashMap<>();

    public static ForgeLighting get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ForgeLighting::new, ForgeLighting::load), "elysium_forge_lighting");
    }

    public boolean active(ForgePlan plan, int circuit) {
        return valid(circuit) && (circuits.getOrDefault(plan.key(), 0) & (1 << circuit)) != 0;
    }

    public boolean restore(ForgePlan plan, int circuit) {
        if (!valid(circuit) || active(plan, circuit)) return false;
        circuits.merge(plan.key(), 1 << circuit, (a, b) -> a | b);
        setDirty();
        return true;
    }

    public static boolean valid(int circuit) { return circuit >= 0 && circuit < CIRCUITS; }

    /** Loaded lamps wake in sequence. Unloaded lamps reconcile once when their block entity loads. */
    public static boolean restore(ServerLevel level, ForgePlan plan, int circuit) {
        boolean changed = get(level).restore(plan, circuit);
        int delay = 1;
        for (var fixture : ForgeFixtures.ALL) {
            if (fixture.circuit() != circuit) continue;
            var pos = fixture.worldPosition(plan);
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null && chunk.getBlockEntity(pos) instanceof ForgeServiceBlockEntity service
                    && service.belongsTo(plan, circuit) && chunk.getBlockState(pos).getBlock() instanceof ForgeServiceBlock) {
                level.scheduleTick(pos, chunk.getBlockState(pos).getBlock(), delay);
                delay += 3;
            }
        }
        return changed;
    }

    public static ForgeLighting load(CompoundTag tag, HolderLookup.Provider provider) {
        var data = new ForgeLighting();
        for (var value : tag.getList("Forges", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            data.circuits.put(row.getLong("Entrance"), row.getInt("Circuits") & 7);
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        var rows = new ListTag();
        circuits.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var row = new CompoundTag();
            row.putLong("Entrance", entry.getKey());
            row.putInt("Circuits", entry.getValue());
            rows.add(row);
        });
        tag.put("Forges", rows);
        return tag;
    }
}
