package dev.elysium.structure;

import dev.elysium.block.ForgeServiceBlock;
import dev.elysium.registry.ModBlocks;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Physical service routes, expressed once for generation, repairs and the lighting simulation. */
public final class ForgeFixtures {
    public enum Kind { CHANNEL, LAMP, BRAZIER }
    public record Fixture(int u, int h, int v, Kind kind, int decay) {
        public BlockPos local() { return new BlockPos(u,h,v); }
        public BlockPos worldPosition(ForgePlan p) { return new BlockPos(p.worldX(u,v),p.floor()+h,p.worldZ(u,v)); }
        public boolean controller() { return kind == Kind.BRAZIER; }
    }
    public static final BlockPos SOURCE = new BlockPos(-5,1,52);
    public static final BlockPos HEARTH_FEED = new BlockPos(0,0,61);
    public static final BlockPos ASH = new BlockPos(-28,0,60);
    public static final BlockPos CRACK = new BlockPos(28,0,60);
    public static final BlockPos MISSING = new BlockPos(0,0,98);
    public static final Map<BlockPos,Fixture> NETWORK;
    public static final List<Fixture> ALL;
    static {
        var map=new LinkedHashMap<BlockPos,Fixture>();
        line(map,SOURCE,new BlockPos(-5,0,52));
        path(map,-5,0,52,-5,0,60,0,0,60,-24,0,60,-44,0,60,-44,0,77,-30,0,77);
        path(map,0,0,60,24,0,60,43,0,60,43,0,80);
        line(map,new BlockPos(0,0,60),HEARTH_FEED);
        path(map,-12,0,60,-12,0,90,0,0,90,0,0,115);
        path(map,-24,0,60,-24,0,74);
        path(map,24,0,60,24,0,74);
        path(map,-5,0,52,-5,0,45);
        lamp(map,-5,4,45,-5,0,45);
        lamp(map,-24,4,64,-24,0,64);lamp(map,24,5,64,24,0,64);
        lamp(map,-24,9,74,-24,0,74);lamp(map,24,9,74,24,0,74);
        lamp(map,-4,2,73,-12,0,73);lamp(map,14,1,67,24,0,67);
        lamp(map,-44,4,57,-44,0,60);lamp(map,-44,5,72,-44,0,72);lamp(map,-30,4,77,-30,0,77);
        lamp(map,43,5,69,43,0,69);lamp(map,43,8,80,43,0,80);
        lamp(map,-7,5,107,0,0,107);lamp(map,7,5,107,0,0,107);
        put(map,SOURCE,Kind.BRAZIER,0);put(map,ASH,Kind.CHANNEL,1);put(map,CRACK,Kind.CHANNEL,2);
        NETWORK=Collections.unmodifiableMap(map);ALL=List.copyOf(map.values());
    }
    private static void put(Map<BlockPos,Fixture> m,BlockPos p,Kind kind,int decay) { m.put(p,new Fixture(p.getX(),p.getY(),p.getZ(),kind,decay)); }
    private static void line(Map<BlockPos,Fixture> m,BlockPos a,BlockPos b) {
        if((a.getX()!=b.getX()?1:0)+(a.getY()!=b.getY()?1:0)+(a.getZ()!=b.getZ()?1:0)>1)throw new IllegalArgumentException("Non-cardinal ember route");
        var p=a;put(m,p,Kind.CHANNEL,0);
        while(!p.equals(b)) { p=p.offset(Integer.compare(b.getX(),p.getX()),Integer.compare(b.getY(),p.getY()),Integer.compare(b.getZ(),p.getZ()));put(m,p,Kind.CHANNEL,0); }
    }
    private static void path(Map<BlockPos,Fixture> m,int... xyz) {
        for(int i=3;i<xyz.length;i+=3)line(m,new BlockPos(xyz[i-3],xyz[i-2],xyz[i-1]),new BlockPos(xyz[i],xyz[i+1],xyz[i+2]));
    }
    private static void lamp(Map<BlockPos,Fixture> m,int u,int h,int v,int x,int y,int z) {
        path(m,x,y,z,u,y,z,u,y,v,u,h,v);put(m,new BlockPos(u,h,v),Kind.LAMP,0);
    }
    public static BlockPos source(ForgePlan p) { return NETWORK.get(SOURCE).worldPosition(p); }
    public static BlockPos hearth(ForgePlan p) { return NETWORK.get(HEARTH_FEED).worldPosition(p).above(); }
    public static Fixture at(ForgePlan p,BlockPos world) { return NETWORK.get(new BlockPos(p.localU(world.getX(),world.getZ()),world.getY()-p.floor(),p.localV(world.getX(),world.getZ()))); }
    public static BlockState state(ForgePlan p,Fixture f) {
        var block=switch(f.kind()) {case CHANNEL->ModBlocks.FORGE_EMBER_CHANNEL;case LAMP->ModBlocks.FORGE_SERVICE_LAMP;case BRAZIER->ModBlocks.FORGE_SERVICE_SWITCH;};
        var state=block.get().defaultBlockState().setValue(ForgeServiceBlock.DECAY,f.decay());
        for(var d:Direction.values()) {
            var rotated=d;for(int i=0;i<p.turn();i++)if(rotated.getAxis()!=Direction.Axis.Y)rotated=rotated.getClockWise();
            state=state.setValue(ForgeServiceBlock.connection(rotated),NETWORK.containsKey(f.local().relative(d)));
        }
        return state;
    }
    private ForgeFixtures() {}
}
