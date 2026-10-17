package dev.elysium.story;

import dev.elysium.Elysium;
import dev.elysium.block.WatchfireBlockEntity;
import dev.elysium.entity.CinderWispEntity;
import dev.elysium.registry.ModEntities;
import dev.elysium.portal.ElysiumTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Bounded, loaded-surface encounters during the unresolved night; light and repaired watches shelter travellers. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class Nightfall {
    public static boolean protectedAt(ServerLevel level,BlockPos pos) {
        for(var ward:RealmCycle.get(level).wardsNear(pos))if(ward.distSqr(pos)<=48*48) {
            var chunk=level.getChunkSource().getChunkNow(ward.getX()>>4,ward.getZ()>>4);
            if(chunk!=null&&chunk.getBlockEntity(ward) instanceof WatchfireBlockEntity fire&&fire.restored())return true;
            if(chunk!=null&&ward.distSqr(pos)<=12*12&&chunk.getBlockState(ward).is(dev.elysium.registry.ModBlocks.WARD_LAMP.get()))return true;
        }
        return false;
    }
    public static boolean eligible(ServerPlayer p) {
        var level=p.serverLevel();
        return level.dimension().equals(ElysiumTravel.DIMENSION)&&!p.isCreative()&&!p.isSpectator()&&p.isAlive()
            &&p.getY()>=level.getSeaLevel()&&level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL
            &&level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)&&RealmCycle.get(level).unsettled();
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer p)||Math.floorMod(p.tickCount+p.getId(),200)!=0||!eligible(p))return;
        var level=p.serverLevel();if(level.random.nextInt(4)!=0||protectedAt(level,p.blockPosition()))return;
        if(level.getEntitiesOfClass(CinderWispEntity.class,p.getBoundingBox().inflate(64)).size()>=2)return;
        int total=0;for(var e:level.getAllEntities())if(e instanceof CinderWispEntity&&++total>=12)return;
        for(int attempt=0;attempt<6;attempt++) {
            double angle=level.random.nextDouble()*Math.PI*2;int distance=24+level.random.nextInt(17);
            int x=p.getBlockX()+(int)(Math.cos(angle)*distance),z=p.getBlockZ()+(int)(Math.sin(angle)*distance);
            var chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)continue;
            int y=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x&15,z&15)+2;var pos=new BlockPos(x,y,z);
            if(y<level.getSeaLevel()||Math.abs(y-p.getY())>16||!level.canSeeSky(pos)||!level.getBlockState(pos).isAir()
                ||level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos)>6||protectedAt(level,pos))continue;
            var wisp=ModEntities.CINDER_WISP.get().create(level);if(wisp==null)return;
            wisp.setPos(x+.5,y,z+.5);if(!level.noCollision(wisp))continue;
            level.addFreshEntity(wisp);break;
        }
    }
    private Nightfall(){}
}
