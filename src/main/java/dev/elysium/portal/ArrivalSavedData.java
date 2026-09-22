package dev.elysium.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** One shared arrival sanctuary per world, surviving reloads and shared by every player. */
final class ArrivalSavedData extends SavedData {
    private BlockPos altar;

    static ArrivalSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(ArrivalSavedData::new, ArrivalSavedData::load), "elysium_arrival");
    }

    private static ArrivalSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ArrivalSavedData data = new ArrivalSavedData();
        if (tag.contains("Altar", Tag.TAG_LONG)) {
            data.altar = BlockPos.of(tag.getLong("Altar"));
        }
        return data;
    }

    BlockPos altar() {
        return altar;
    }

    void setAltar(BlockPos pos) {
        altar = pos.immutable();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (altar != null) {
            tag.putLong("Altar", altar.asLong());
        }
        return tag;
    }
}
