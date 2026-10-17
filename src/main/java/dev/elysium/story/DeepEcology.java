package dev.elysium.story;

import dev.elysium.Elysium;
import dev.elysium.entity.AshenHoundEntity;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModEntities;
import dev.elysium.worldgen.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Deep inhabitants use occupied, already loaded ground and a separate bounded population. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class DeepEcology {
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||Math.floorMod(p.tickCount+p.getId(),200)!=0)return;
        var l=p.serverLevel();
        if(!l.dimension().equals(ElysiumTravel.DIMENSION)||p.getY()>=TartarusTerrain.TOP||p.isCreative()||p.isSpectator()||!p.isAlive()||l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||!l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        if(!(l.getChunkSource().getGenerator() instanceof StratifiedChunkGenerator generator))return;
        if(l.getEntitiesOfClass(AshenHoundEntity.class,p.getBoundingBox().inflate(64)).size()>=4)return;
        int total=0;for(var e:l.getAllEntities())if(e instanceof AshenHoundEntity&&++total>=40)return;
        var terrain=generator.terrain(l.getChunkSource().randomState());
        for(int i=0;i<8;i++){
            double angle=l.random.nextDouble()*Math.PI*2;int distance=26+l.random.nextInt(17),x=p.getBlockX()+(int)(Math.cos(angle)*distance),z=p.getBlockZ()+(int)(Math.sin(angle)*distance);
            var chunk=l.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null||terrain.region(x,z)==TartarusTerrain.Region.HOLLOW_MIRE)continue;
            int y=terrain.floor(x,z)+1;var pos=new BlockPos(x,y,z);
            if(Math.abs(y-p.getY())>20||!l.getBlockState(pos.below()).isSolid()||!l.getBlockState(pos).isAir()||!l.getBlockState(pos.above()).isAir()||Nightfall.protectedAt(l,pos))continue;
            var mob=ModEntities.ASHEN_HOUND.get().create(l);if(mob==null)return;mob.setPos(x+.5,y,z+.5);if(!l.noCollision(mob))continue;l.addFreshEntity(mob);break;
        }
    }
}
