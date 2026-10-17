package dev.elysium.gametest;

import com.mojang.logging.LogUtils;
import dev.elysium.block.WatchfireBlockEntity;
import dev.elysium.forge.StructureSurvey;
import dev.elysium.structure.WatchfirePiece;
import dev.elysium.structure.WatchfireStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/** Proves all three survey destinations are real generated, saved and bound ruins. */
final class WatchfireNaturalGenerationProbe {
    static WatchfirePiece[] generateAndVerify(ServerLevel level,BlockPos origin) {
        var result=new WatchfirePiece[3];var generator=level.getChunkSource().getGenerator();
        for(int kind=0;kind<3;kind++) {
            var id=StructureSurvey.Destination.values()[kind+2].id();
            var structure=(WatchfireStructure)level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
            var placement=(RandomSpreadStructurePlacement)level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id).placement();
            int inspected=0;
            for(var source:StructureSurvey.candidates(placement,level.getSeed(),origin)) {
                inspected++;
                if(!placement.isStructureChunk(level.getChunkSource().getGeneratorState(),source.x,source.z))continue;
                var context=new Structure.GenerationContext(level.registryAccess(),generator,generator.getBiomeSource(),
                        level.getChunkSource().randomState(),level.getServer().getStructureManager(),level.getSeed(),source,level,structure.biomes()::contains);
                var stub=structure.findGenerationPoint(context);if(stub.isEmpty())continue;
                var destination=StructureSurvey.probe(level,structure,new StructureSurvey.Candidate(source,placement));
                require(destination.isPresent(),"Watchfinder preview did not match an actual saved start");
                var socket=destination.orElseThrow();level.getChunk(socket);
                var start=level.getChunk(source.x,source.z).getStartForStructure(structure);
                require(start!=null&&start.isValid(),"Watch has no saved start");
                var piece=(WatchfirePiece)start.getPieces().getFirst();var bounds=piece.getBoundingBox();
                for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++) {
                    require(level.getChunk(x,z).getReferencesForStructure(structure).contains(source.toLong()),"Watch chunk missing its reference");
                    level.setChunkForced(x,z,true);
                }
                require(level.getBlockEntity(socket) instanceof WatchfireBlockEntity fire&&fire.watch()==kind&&!fire.restored(),"Natural watch is missing, unbound or already restored");
                result[kind]=piece;
                LogUtils.getLogger().info("ELYSIUM_NATURAL_WATCH_VERIFIED kind={} seed={} source={} socket={} candidates={}",kind,level.getSeed(),source,socket,inspected);
                break;
            }
            require(result[kind]!=null,"No natural watch "+kind+" within the real wayfinder's bounded search");
        }
        return result;
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private WatchfireNaturalGenerationProbe(){}
}
