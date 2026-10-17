package dev.elysium.story;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** The realm's own clock. Never writes the Overworld's shared DerivedLevelData. */
public final class RealmCycle extends SavedData {
    public enum Phase { PARADISE, DESCENDING, NIGHT, STALLED, LIVING, GOLDEN }
    private Phase phase=Phase.PARADISE;
    private long time=11500;
    private int watches,bindings,forgings,pressureTicks,ruptures;
    private BlockPos forge;
    private boolean firstDawn;
    private final Map<Long,Set<BlockPos>> wards=new HashMap<>();
    public static RealmCycle get(ServerLevel level){return level.getDataStorage().computeIfAbsent(new Factory<>(RealmCycle::new,RealmCycle::load),"elysium_realm_cycle");}
    public Phase phase(){return phase;}
    public long time(){return time;}
    public int watches(){return watches;}
    public int bindings(){return bindings;}
    public int forgings(){return forgings;}public int ruptures(){return ruptures;}public int pressureTicks(){return pressureTicks;}
    public boolean bindingHeld(int kind){return kind>=0&&kind<3&&(bindings&(1<<kind))!=0;}
    public boolean bindDeep(int kind){if(!released()||kind<0||kind>2||bindingHeld(kind))return false;bindings|=1<<kind;setDirty();return true;}
    public boolean recordWork(BlockPos hearth){forgings++;pressureTicks=Math.min(12000,pressureTicks+200);boolean first=awaken(hearth);setDirty();return first;}
    public boolean ruptureDue(){return unsettled()&&bindings!=7&&pressureTicks>=(ruptures==0?600:Math.max(2400,6000-forgings*160));}
    public void ruptureOpened(){ruptures++;pressureTicks=0;setDirty();}
    public BlockPos forge(){return forge;}
    public boolean firstDawn(){return firstDawn;}
    public boolean moving(){return phase==Phase.DESCENDING||phase==Phase.NIGHT||phase==Phase.LIVING;}
    public boolean unsettled(){return phase==Phase.NIGHT||phase==Phase.STALLED;}
    public boolean released(){return phase!=Phase.PARADISE;}
    public List<BlockPos> wards(){return wards.values().stream().flatMap(Set::stream).toList();}
    public List<BlockPos> wardsNear(BlockPos pos) {
        var found=new ArrayList<BlockPos>();int x=pos.getX()>>4,z=pos.getZ()>>4;
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)found.addAll(wards.getOrDefault(net.minecraft.world.level.ChunkPos.asLong(x+dx,z+dz),Set.of()));
        return found;
    }
    public boolean awaken(BlockPos hearth){if(phase!=Phase.PARADISE)return false;forge=hearth.immutable();phase=Phase.DESCENDING;time=11500;setDirty();return true;}
    public void advance(boolean occupied) {
        if(!occupied)return;
        if(unsettled()&&bindings!=7){pressureTicks=Math.min(12000,pressureTicks+1);setDirty();}
        if(!moving())return;
        time++;
        if(phase==Phase.DESCENDING&&time>=14000)phase=Phase.NIGHT;
        if(phase==Phase.NIGHT&&time>=18000){time=18000;phase=Phase.STALLED;}
        if(phase==Phase.LIVING&&Math.floorMod(time,24000)==0)firstDawn=true;
        setDirty();
    }
    public boolean answerWatch(int kind,BlockPos pos) {
        if(kind<0||kind>2||!unsettled())return false;
        watches|=1<<kind;
        addWard(pos);setDirty();return true;
    }
    public void addWard(BlockPos pos){if(wards.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4),key->new HashSet<>()).add(pos.immutable()))setDirty();}
    public void removeWard(BlockPos pos){long key=net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4);var set=wards.get(key);if(set!=null&&set.remove(pos)){if(set.isEmpty())wards.remove(key);setDirty();}}
    public boolean resolve(boolean living) {
        if(watches!=7||bindings!=7||phase==Phase.PARADISE)return false;
        var chosen=living?Phase.LIVING:Phase.GOLDEN;
        if(phase==chosen)return false;
        phase=chosen;
        // Repair dawn from the late night; switching a settled realm preserves the chosen afternoon.
        if(!living)time=11500;
        else if(!firstDawn)time=22000;
        setDirty();return true;
    }
    public void sleepToDawn(){if(phase==Phase.LIVING){time=(Math.floorDiv(time,24000)+1)*24000;firstDawn=true;setDirty();}}
    public static RealmCycle load(CompoundTag tag,HolderLookup.Provider provider) {
        var c=new RealmCycle();
        try{c.phase=Phase.valueOf(tag.getString("Phase"));}catch(IllegalArgumentException ignored){}
        c.time=Math.max(0,tag.getLong("Time"));if(c.phase==Phase.PARADISE||c.phase==Phase.GOLDEN)c.time=11500;
        if(c.phase==Phase.STALLED)c.time=18000;
        c.watches=tag.getInt("Watches")&7;c.firstDawn=tag.getBoolean("FirstDawn");
        c.bindings=tag.getInt("Bindings")&7;c.forgings=Math.max(0,tag.getInt("Forgings"));c.ruptures=Math.max(0,tag.getInt("Ruptures"));c.pressureTicks=Math.max(0,Math.min(12000,tag.getInt("Pressure")));
        if(tag.contains("Forge",Tag.TAG_LONG))c.forge=BlockPos.of(tag.getLong("Forge"));
        for(long pos:tag.getLongArray("Wards"))c.addWard(BlockPos.of(pos));
        return c;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider) {
        tag.putInt("Version",1);tag.putString("Phase",phase.name());tag.putLong("Time",time);tag.putInt("Watches",watches);tag.putBoolean("FirstDawn",firstDawn);
        tag.putInt("Bindings",bindings);tag.putInt("Forgings",forgings);tag.putInt("Ruptures",ruptures);tag.putInt("Pressure",pressureTicks);
        if(forge!=null)tag.putLong("Forge",forge.asLong());tag.putLongArray("Wards",wards().stream().mapToLong(BlockPos::asLong).sorted().toArray());return tag;
    }
}
