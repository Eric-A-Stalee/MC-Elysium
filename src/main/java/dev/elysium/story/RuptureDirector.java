package dev.elysium.story;

import dev.elysium.Elysium;
import dev.elysium.block.RiftBlockEntity;
import dev.elysium.registry.ModBlocks;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Pressure spends itself only on a real loaded-surface event; no background chunk loading or offline punishment. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class RuptureDirector {
    public static boolean open(ServerLevel l,RupturePlan plan,int kind){
        if(kind<0||kind>2||!plan.loaded(l)||!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||!l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)||RealmCycle.get(l).bindingHeld(kind)||Nightfall.protectedAt(l,plan.core().above(6)))return false;
        if(!RupturePlan.natural(l.getBlockState(plan.core()))||l.getBlockState(plan.core()).hasBlockEntity())return false;
        l.setBlock(plan.core(),ModBlocks.SURFACE_RIFT.get().defaultBlockState(),3);if(!(l.getBlockEntity(plan.core()) instanceof RiftBlockEntity core))return false;
        core.bind(plan,kind);RealmCycle.get(l).ruptureOpened();return true;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||Math.floorMod(p.tickCount+p.getId(),100)!=0||!Nightfall.eligible(p))return;
        var l=p.serverLevel();var cycle=RealmCycle.get(l);if(!cycle.ruptureDue()||Nightfall.protectedAt(l,p.blockPosition())||!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return;
        int loaded=0;
        for(var player:l.players())for(int cx=(player.getBlockX()-80)>>4;cx<=(player.getBlockX()+80)>>4;cx++)for(int cz=(player.getBlockZ()-80)>>4;cz<=(player.getBlockZ()+80)>>4;cz++){
            var chunk=l.getChunkSource().getChunkNow(cx,cz);if(chunk==null)continue;
            for(var be:chunk.getBlockEntities().values())if(be instanceof RiftBlockEntity r&&!r.sealed()&&(r.getBlockPos().distSqr(p.blockPosition())<100*100||++loaded>=2))return;
        }
        for(int i=0;i<8;i++){
            double a=l.random.nextDouble()*Math.PI*2;int d=34+l.random.nextInt(25);var plan=RupturePlan.survey(l,p.getBlockX()+(int)(Math.cos(a)*d),p.getBlockZ()+(int)(Math.sin(a)*d));if(plan.isEmpty())continue;
            int kind=l.random.nextInt(3);for(int j=0;j<3&&cycle.bindingHeld(kind);j++)kind=(kind+1)%3;
            if(open(l,plan.get(),kind))return;
        }
    }
    private RuptureDirector(){}
}
