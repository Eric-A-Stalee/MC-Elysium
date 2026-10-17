package dev.elysium.forge;

import dev.elysium.Elysium;
import dev.elysium.item.ResonanceItem;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.structure.HephaestusForgePiece;
import dev.elysium.structure.TartarusChainPiece;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** One candidate per server tick across all players. Uses actual saved starts, never speculative map targets. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class StructureSurvey {
    public enum Destination {
        FORGE("hephaestus_forge"), CHAIN("tartarus_chain"), HARVEST_WATCH("harvest_watch"), WATER_WATCH("water_watch"), HIGH_WATCH("high_watch");
        private final String path;
        Destination(String path){this.path=path;}
        public ResourceLocation id(){return ResourceLocation.fromNamespaceAndPath(Elysium.MOD_ID,path);}
    }
    public record Candidate(ChunkPos chunk,RandomSpreadStructurePlacement placement){}
    private record Request(UUID player,UUID item,Destination destination,Structure structure,List<Candidate> candidates,int index,long expires) {
        Request next(){return new Request(player,item,destination,structure,candidates,index+1,expires);}
    }
    private static final Map<MinecraftServer,ArrayDeque<Request>> QUEUES=new IdentityHashMap<>();
    public static final int REGION_RADIUS=8, MAX_CANDIDATES=289, MAX_PLAYERS=8;

    public static List<ChunkPos> candidates(RandomSpreadStructurePlacement placement,long seed,BlockPos origin) {
        int rx=Math.floorDiv(origin.getX()>>4,placement.spacing()),rz=Math.floorDiv(origin.getZ()>>4,placement.spacing());
        var candidates=new ArrayList<ChunkPos>();
        for(int x=-REGION_RADIUS;x<=REGION_RADIUS;x++)for(int z=-REGION_RADIUS;z<=REGION_RADIUS;z++)
            candidates.add(placement.getPotentialStructureChunk(seed,(rx+x)*placement.spacing(),(rz+z)*placement.spacing()));
        candidates.sort(Comparator.comparingDouble(p->distance(p,origin)));return List.copyOf(candidates);
    }
    private static double distance(ChunkPos p,BlockPos origin){return Math.hypot(p.getMiddleBlockX()-origin.getX(),p.getMiddleBlockZ()-origin.getZ());}
    public static void begin(ServerPlayer player,ItemStack item,Destination destination) {
        var level=player.serverLevel();
        if(!level.dimension().equals(ElysiumTravel.DIMENSION)){ForgeLore.tell(player,"outside");return;}
        if(!level.structureManager().shouldGenerateStructures()){ForgeLore.tell(player,"survey_disabled");return;}
        var queue=QUEUES.computeIfAbsent(player.server,s->new ArrayDeque<>());
        if(queue.stream().anyMatch(r->r.player.equals(player.getUUID()))){ForgeLore.tell(player,"survey_pending");return;}
        if(queue.size()>=MAX_PLAYERS){ForgeLore.tell(player,"survey_busy");return;}
        var holder=level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(ResourceKey.create(Registries.STRUCTURE,destination.id()));
        if(holder.isEmpty()){ForgeLore.tell(player,"survey_missing");return;}
        var placements=level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder.get());
        var candidates=new ArrayList<Candidate>();
        for(var placement:placements)if(placement instanceof RandomSpreadStructurePlacement random)
            for(var chunk:candidates(random,level.getSeed(),player.blockPosition()))candidates.add(new Candidate(chunk,random));
        candidates.sort(Comparator.comparingDouble(c->distance(c.chunk,player.blockPosition())));
        if(candidates.isEmpty()){ForgeLore.tell(player,"survey_missing");return;}
        queue.add(new Request(player.getUUID(),ResonanceItem.identity(item),destination,holder.get().value(),
                List.copyOf(candidates.subList(0,Math.min(MAX_CANDIDATES,candidates.size()))),0,level.getGameTime()+4800));
        ForgeLore.tell(player,"survey_started");
    }
    public static Optional<BlockPos> destination(StructureStart start) {
        if(start==null||!start.isValid())return Optional.empty();
        for(var piece:start.getPieces()) {
            if(piece instanceof HephaestusForgePiece forge) {
                return Optional.of(forge.plan().entrance());
            }
            if(piece instanceof dev.elysium.structure.WatchfirePiece watch)return Optional.of(watch.socket());
            if(piece instanceof TartarusChainPiece chain) {
                var p=chain.plan();return Optional.of(new BlockPos(p.x(),p.top(),p.z()));
            }
        }
        return Optional.empty();
    }
    public static Optional<BlockPos> probe(ServerLevel level,Structure structure,Candidate candidate) {
        var chunk=candidate.chunk;
        if(!level.getWorldBorder().isWithinBounds(new BlockPos(chunk.getMiddleBlockX(),64,chunk.getMiddleBlockZ())))return Optional.empty();
        if(!candidate.placement.isStructureChunk(level.getChunkSource().getGeneratorState(),chunk.x,chunk.z))return Optional.empty();
        var loaded=level.getChunkSource().getChunkNow(chunk.x,chunk.z);
        if(loaded!=null)return destination(loaded.getStartForStructure(structure));
        // This respects old, already generated chunks lacking the new structure. A successful noise plan alone does not.
        if(level.structureManager().checkStructurePresence(chunk,structure,candidate.placement,false)==StructureCheckResult.START_NOT_PRESENT)return Optional.empty();
        var actual=level.getChunkSource().getChunk(chunk.x,chunk.z,ChunkStatus.STRUCTURE_STARTS,true);
        return actual==null?Optional.empty():destination(actual.getStartForStructure(structure));
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var queue=QUEUES.get(event.getServer());if(queue==null||queue.isEmpty())return;
        var request=queue.remove();var player=event.getServer().getPlayerList().getPlayer(request.player);
        if(player==null || !player.level().dimension().equals(ElysiumTravel.DIMENSION))return;
        var level=player.serverLevel();ItemStack item=ItemStack.EMPTY;
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(ResonanceItem.identifies(player.getInventory().getItem(i),request.item)) {
            item=player.getInventory().getItem(i);break;
        }
        if(item.isEmpty())return;
        if(request.index>=request.candidates.size()||level.getGameTime()>request.expires){ForgeLore.tell(player,"survey_empty");return;}
        if(level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(request.destination.id())!=request.structure){ForgeLore.tell(player,"survey_changed");return;}
        try {
            var result=probe(level,request.structure,request.candidates.get(request.index));
            if(result.isPresent()) {
                ResonanceItem.bind(item,result.get());
                if(item.getItem() instanceof dev.elysium.item.DawnWayfinderItem) {
                    if(ResonanceItem.data(item).getInt("watch_kind")!=request.destination.ordinal()-2){net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,item,t->t.remove("elysium_target"));return;}
                    dev.elysium.item.DawnWayfinderItem.bearing(player,result.get(),ResonanceItem.data(item).getInt("watch_kind"));return;
                }
                if(request.destination==Destination.FORGE)ForgeLore.tell(player,"survey_found",result.get().getX(),result.get().getZ());
                else ForgeLore.tell(player,"chain_found");
                ResonanceItem.bearing(player,result.get(),request.destination==Destination.FORGE);
            } else queue.add(request.next());
        } catch(RuntimeException error) {
            com.mojang.logging.LogUtils.getLogger().warn("Elysium survey could not inspect a structure candidate",error);
            ForgeLore.tell(player,"survey_changed");
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){QUEUES.remove(event.getServer());}
    private StructureSurvey(){}
}
