package dev.elysium.forge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Personal discoveries live with the world: reconnecting, death and shared visits do not reset them. */
public final class ForgeDiscoveries extends SavedData {
    public static final int SURVEY=1, LEDGER=2, BINDINGS=4, HEARTH=8, ANCHOR=16;
    private final Map<UUID,Integer> readers=new HashMap<>();
    public static ForgeDiscoveries get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ForgeDiscoveries::new,ForgeDiscoveries::load),"elysium_forge_discoveries");
    }
    public boolean has(UUID player,int flag) { return (readers.getOrDefault(player,0)&flag)!=0; }
    public boolean discover(UUID player,int flag) {
        if(has(player,flag)) return false;
        readers.merge(player,flag,(a,b)->a|b);setDirty();return true;
    }
    public static ForgeDiscoveries load(CompoundTag tag,HolderLookup.Provider provider) {
        var data=new ForgeDiscoveries();
        for(Tag entry:tag.getList("Readers",Tag.TAG_COMPOUND)) {
            var row=(CompoundTag)entry;
            if(row.hasUUID("Player"))data.readers.put(row.getUUID("Player"),row.getInt("Discoveries")&63);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider) {
        var entries=new ListTag();
        readers.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            var row=new CompoundTag();row.putUUID("Player",entry.getKey());row.putInt("Discoveries",entry.getValue());entries.add(row);
        });
        tag.putInt("Version",1);tag.put("Readers",entries);return tag;
    }
}
