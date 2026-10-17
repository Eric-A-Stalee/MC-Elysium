package dev.elysium.story;

import dev.elysium.Elysium;
import dev.elysium.forge.ForgeLore;
import dev.elysium.portal.ElysiumTravel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server clock and client projection share a dimension-scoped read path, never a global static time. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class RealmClock {
    private record View(long time,long receivedAt,boolean moving){}
    private static final Map<Level,RealmCycle> SERVERS=new ConcurrentHashMap<>();
    private static final Map<Level,View> CLIENTS=new ConcurrentHashMap<>();
    public static long time(Level level) {
        var state=SERVERS.get(level);if(state!=null)return state.time();
        var view=CLIENTS.get(level);if(view==null)return 11500;
        return view.time+(view.moving?Math.max(0,Math.min(40,level.getGameTime()-view.receivedAt)):0);
    }
    public static void accept(Level level,long time,boolean moving){if(level.dimension().equals(ElysiumTravel.DIMENSION))CLIENTS.put(level,new View(Math.max(0,time),level.getGameTime(),moving));}
    @SubscribeEvent public static void load(LevelEvent.Load event) {
        if(event.getLevel() instanceof ServerLevel server&&server.dimension().equals(ElysiumTravel.DIMENSION))SERVERS.put(server,RealmCycle.get(server));
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof Level level){SERVERS.remove(level);CLIENTS.remove(level);}}
    public static void sync(ServerPlayer player) {
        if(!player.level().dimension().equals(ElysiumTravel.DIMENSION))return;
        var state=RealmCycle.get(player.serverLevel());
        PacketDistributor.sendToPlayer(player,new RealmClockPayload(state.time(),state.moving()));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level)||!level.dimension().equals(ElysiumTravel.DIMENSION))return;
        var cycle=RealmCycle.get(level);SERVERS.putIfAbsent(level,cycle);
        var before=cycle.phase();boolean dawn=cycle.firstDawn();
        cycle.advance(!level.players().isEmpty());
        if(before!=cycle.phase())announce(level,cycle.phase()==RealmCycle.Phase.NIGHT?"night":"stalled");
        if(!dawn&&cycle.firstDawn())announce(level,"dawn");
        if(level.getGameTime()%20==0)for(var player:level.players())sync(player);
    }
    public static void announce(ServerLevel level,String chapter) {
        for(var p:level.players()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(30,100,40));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("story.elysium."+chapter+".subtitle")));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("story.elysium."+chapter+".title")));
            ForgeLore.award(p,chapter.equals("sunset")?"first_sunset":chapter.equals("dawn")?"first_dawn":"first_night");
            sync(p);
        }
    }
    @SubscribeEvent public static void sleep(CanPlayerSleepEvent event) {
        if(!event.getLevel().dimension().equals(ElysiumTravel.DIMENSION))return;
        var cycle=RealmCycle.get(event.getEntity().serverLevel());
        if(cycle.phase()!=RealmCycle.Phase.LIVING&&cycle.released()) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            event.getEntity().displayClientMessage(Component.translatable("message.elysium.story.sleep"),true);
        }
    }
    @SubscribeEvent public static void slept(SleepFinishedTimeEvent event) {
        if(event.getLevel() instanceof ServerLevel level&&level.dimension().equals(ElysiumTravel.DIMENSION)) {
            var cycle=RealmCycle.get(level);boolean first=cycle.firstDawn();cycle.sleepToDawn();
            if(!first&&cycle.firstDawn())announce(level,"dawn");
            for(var p:level.players())sync(p);
        }
    }
    private RealmClock(){}
}
