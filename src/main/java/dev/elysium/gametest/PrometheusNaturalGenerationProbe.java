package dev.elysium.gametest;

import dev.elysium.block.PrometheusBlockEntity;
import dev.elysium.forge.StructureSurvey;
import dev.elysium.structure.PrometheusPrisonPiece;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/** Validates a located ordinary start, its distant chunk reference and the actual approach. */
final class PrometheusNaturalGenerationProbe {
    static PrometheusPrisonPiece generateAndVerify(ServerLevel l){
        var id=StructureSurvey.Destination.PROMETHEUS.id();var structure=l.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        var placement=(RandomSpreadStructurePlacement)l.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id).placement();
        for(var source:StructureSurvey.candidates(placement,l.getSeed(),new BlockPos(0,-480,0))){
            var dest=StructureSurvey.probe(l,structure,new StructureSurvey.Candidate(source,placement));if(dest.isEmpty())continue;
            var start=l.getChunkSource().getChunk(source.x,source.z,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS,true).getStartForStructure(structure);
            var piece=(PrometheusPrisonPiece)start.getPieces().getFirst();
            if(!l.getChunk(piece.approach()).getReferencesForStructure(structure).contains(source.toLong()))throw new IllegalStateException("Prison approach cannot discover its distant start");
            var bounds=piece.getBoundingBox();int chunks=0;
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++){l.getChunk(x,z);l.setChunkForced(x,z,true);chunks++;}
            for(int x=(piece.arena().getX()-34)>>4;x<=(piece.arena().getX()+34)>>4;x++)for(int z=(piece.arena().getZ()-34)>>4;z<=(piece.arena().getZ()+34)>>4;z++){l.getChunk(x,z);l.setChunkForced(x,z,true);}
            for(var p:piece.approachRoute())if(l.getBlockState(p).isAir()||!l.getBlockState(p.above()).isAir()||!l.getBlockState(p.above(2)).isAir())throw new IllegalStateException("Prison approach is blocked at "+p);
            if(!(l.getBlockEntity(piece.voice()) instanceof PrometheusBlockEntity controller)||!piece.arena().equals(controller.arena()))throw new IllegalStateException("Natural prison has no bound teacher");
            for(int dx=-12;dx<=12;dx+=3)for(int dz=-12;dz<=12;dz+=3)if(dx*dx+dz*dz<=144){var p=piece.arena().offset(dx,0,dz);if(!l.getBlockState(p).isAir()||l.getBlockState(p.below()).isAir())throw new IllegalStateException("Natural fighting ledge differs at "+p);}
            com.mojang.logging.LogUtils.getLogger().info("ELYSIUM_NATURAL_PROMETHEUS_VERIFIED source={} voice={} arena={} chunks={}",source,piece.voice(),piece.arena(),chunks);return piece;
        }
        throw new IllegalStateException("No natural prison in the bounded resonator survey");
    }
}
