package dev.elysium.block;

import dev.elysium.forge.ForgeLighting;
import dev.elysium.registry.ModBlockEntities;
import dev.elysium.structure.ForgeFixtures;
import dev.elysium.structure.ForgePlan;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Only the brazier ticks. Other fixtures check their live source on load and while illuminated. */
public final class ForgeServiceBlockEntity extends BlockEntity {
    public static final int FUEL_UNIT=12000,MAX_FUEL=8*FUEL_UNIT;
    private ForgePlan plan;
    private int fuelTicks;
    private boolean airflow,ignited;
    private Set<BlockPos> energized=new HashSet<>();
    public ForgeServiceBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.FORGE_SERVICE.get(),pos,state); }
    public ForgePlan plan() { return plan; }
    public boolean belongsTo(ForgePlan other) { return other.equals(plan); }
    public int fuelTicks() { return fuelTicks; }
    public boolean airflow() { return airflow; }
    public boolean ignited() { return ignited; }
    public boolean burning() { return airflow&&ignited&&fuelTicks>0; }
    public boolean source() { return plan!=null&&worldPosition.equals(ForgeFixtures.source(plan))&&getBlockState().getBlock() instanceof ForgeServiceBlock block&&block.controller(); }
    public Set<BlockPos> energized() { return energized; }
    public void setEnergized(Set<BlockPos> value) { energized=value; }
    public boolean addFuel(int ticks) { if(!source()||ticks<=0||fuelTicks>MAX_FUEL-ticks)return false;fuelTicks+=ticks;setChanged();return true; }
    public boolean ignite() { if(!source()||fuelTicks==0||ignited)return false;ignited=true;setChanged();return true; }
    public void toggleAirflow() { airflow=!airflow;setChanged(); }
    public void consume(int ticks) {
        if(!burning())return;
        fuelTicks=Math.max(0,fuelTicks-ticks);
        if(fuelTicks==0)ignited=false;
        setChanged();
    }
    public void bind(ForgePlan plan) {
        if(ForgeFixtures.at(plan,worldPosition)==null)throw new IllegalArgumentException("Fixture outside the physical Forge network");
        this.plan=plan;setChanged();
        if(level instanceof ServerLevel server)server.scheduleTick(worldPosition,getBlockState().getBlock(),1);
    }
    @Override public void onLoad() {
        super.onLoad();
        if(level instanceof ServerLevel server)server.scheduleTick(worldPosition,getBlockState().getBlock(),1);
    }
    public static void serverTick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,ForgeServiceBlockEntity service) {
        if(level.getGameTime()%10==0&&service.source())ForgeLighting.advance((ServerLevel)level,service,10);
    }
    public void reconcile(ServerLevel server) {
        var state=getBlockState();if(!(state.getBlock() instanceof ForgeServiceBlock))return;
        var origin=plan==null?null:ForgeLighting.loadedService(server,ForgeFixtures.source(plan));
        boolean active=origin!=null&&origin.source()&&origin.burning()&&origin.energized.contains(worldPosition)
                &&state.getValue(ForgeServiceBlock.DECAY)==0;
        if(state.getValue(ForgeServiceBlock.LIT)!=active)server.setBlock(worldPosition,state.setValue(ForgeServiceBlock.LIT,active),3);
        if(active)server.scheduleTick(worldPosition,state.getBlock(),20);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.saveAdditional(tag,provider);
        if(plan!=null) {tag.putLong("Entrance",plan.key());tag.putInt("Turn",plan.turn());}
        if(source()) {tag.putInt("Fuel",fuelTicks);tag.putBoolean("Airflow",airflow);tag.putBoolean("Ignited",ignited);}
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.loadAdditional(tag,provider);plan=null;energized=new HashSet<>();
        int turn=tag.getInt("Turn");
        if(tag.contains("Entrance",Tag.TAG_LONG)&&turn>=0&&turn<4) {
            var entry=BlockPos.of(tag.getLong("Entrance"));
            var candidate=new ForgePlan(entry.getX(),entry.getZ(),entry.getY(),turn);
            if(ForgeFixtures.at(candidate,worldPosition)!=null)plan=candidate;
        }
        fuelTicks=Math.max(0,Math.min(MAX_FUEL,tag.getInt("Fuel")));airflow=tag.getBoolean("Airflow");ignited=tag.getBoolean("Ignited")&&fuelTicks>0;
    }
}
