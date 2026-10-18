package dev.elysium.story;

import dev.elysium.Elysium;
import dev.elysium.entity.*;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModEntities;
import dev.elysium.worldgen.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Occupied, loaded habitat only. Hostile and ambient populations have separate local/realm caps. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class DeepEcology {
    public static boolean hostile(Mob mob){return mob instanceof AshenHoundEntity||mob instanceof MireMournerEntity||mob instanceof ChainWardenEntity;}
    public static boolean ambient(Mob mob){return mob instanceof AshMothEntity;}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||Math.floorMod(p.tickCount+p.getId(),200)!=0)return;
        var l=p.serverLevel();
        if(!l.dimension().equals(ElysiumTravel.DIMENSION)||p.getY()>=TartarusTerrain.TOP||p.isCreative()||p.isSpectator()||!p.isAlive()||!l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        if(!(l.getChunkSource().getGenerator() instanceof StratifiedChunkGenerator generator))return;
        var terrain=generator.terrain(l.getChunkSource().randomState());
        populate(l,p,terrain,true);
        if(l.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL)populate(l,p,terrain,false);
    }
    private static void populate(ServerLevel l,ServerPlayer p,TartarusTerrain terrain,boolean moth){
        if(l.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(64),e->moth?ambient(e):hostile(e)).size()>=(moth?5:4))return;
        int total=0;for(var e:l.getAllEntities())if(e instanceof Mob m&&(moth?ambient(m):hostile(m))&&++total>=(moth?48:40))return;
        for(int i=0;i<8;i++){
            double angle=l.random.nextDouble()*Math.PI*2;int distance=moth?12+l.random.nextInt(16):26+l.random.nextInt(17);
            int x=p.getBlockX()+(int)(Math.cos(angle)*distance),z=p.getBlockZ()+(int)(Math.sin(angle)*distance);
            if(l.getChunkSource().getChunkNow(x>>4,z>>4)==null)continue;
            var region=terrain.region(x,z);int y=Math.max(terrain.floor(x,z),TartarusTerrain.WATERLINE)+1;
            var pos=new BlockPos(x,y,z);if(Math.abs(y-p.getY())>20||Nightfall.protectedAt(l,pos)&&!moth)continue;
            Mob mob;
            if(moth){mob=ModEntities.ASH_MOTH.get().create(l);pos=pos.above(2);}
            else if(region==TartarusTerrain.Region.HOLLOW_MIRE)mob=ModEntities.MIRE_MOURNER.get().create(l);
            else if(region==TartarusTerrain.Region.CHAIN_FOREST&&l.random.nextInt(4)==0)mob=ModEntities.CHAIN_WARDEN.get().create(l);
            else mob=ModEntities.ASHEN_HOUND.get().create(l);
            if(mob==null)return;
            if(!moth&&!l.getBlockState(pos.below()).isSolid()&&!(mob instanceof MireMournerEntity&&!l.getFluidState(pos.below()).isEmpty()))continue;
            if(!l.getBlockState(pos).isAir())continue;
            mob.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
            // Wide or tall entities must not load neighbours just to test their spawn.
            var box=mob.getBoundingBox();boolean loaded=true;
            for(int cx=((int)Math.floor(box.minX))>>4;cx<=((int)Math.floor(box.maxX))>>4;cx++)for(int cz=((int)Math.floor(box.minZ))>>4;cz<=((int)Math.floor(box.maxZ))>>4;cz++)if(l.getChunkSource().getChunkNow(cx,cz)==null)loaded=false;
            if(!loaded||!l.noCollision(mob))continue;
            l.addFreshEntity(mob);return;
        }
    }
    private DeepEcology(){}
}
