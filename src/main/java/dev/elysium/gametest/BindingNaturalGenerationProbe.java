package dev.elysium.gametest;

import dev.elysium.block.BindingBlockEntity;
import dev.elysium.forge.StructureSurvey;
import dev.elysium.structure.DeepBindingPiece;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

final class BindingNaturalGenerationProbe {
    static DeepBindingPiece[] generateAndVerify(ServerLevel l){
        var result=new DeepBindingPiece[3];
        for(int kind=0;kind<3;kind++){
            var id=StructureSurvey.Destination.values()[StructureSurvey.Destination.HUNGER.ordinal()+kind].id();var structure=l.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
            var placement=(RandomSpreadStructurePlacement)l.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id).placement();
            for(var source:StructureSurvey.candidates(placement,l.getSeed(),new BlockPos(0,-480,0))){
                if(StructureSurvey.probe(l,structure,new StructureSurvey.Candidate(source,placement)).isEmpty())continue;
                var start=l.getChunkSource().getChunk(source.x,source.z,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS,true).getStartForStructure(structure);
                var piece=(DeepBindingPiece)start.getPieces().getFirst();var approach=piece.approachRoute();
                if(!l.getChunk(approach.getLast()).getReferencesForStructure(structure).contains(source.toLong()))throw new IllegalStateException("Binding approach cannot find its start: "+kind);
                var b=piece.getBoundingBox();for(int x=(b.minX()-2)>>4;x<=(b.maxX()+2)>>4;x++)for(int z=(b.minZ()-2)>>4;z<=(b.maxZ()+2)>>4;z++){l.getChunk(x,z);l.setChunkForced(x,z,true);}
                for(var p:approach)if(l.getBlockState(p).isAir()||!l.getBlockState(p.above()).isAir()||!l.getBlockState(p.above(2)).isAir())throw new IllegalStateException("Blocked natural binding approach "+kind+" at "+p);
                if(!(l.getBlockEntity(piece.core()) instanceof BindingBlockEntity core)||core.kind()!=kind)throw new IllegalStateException("Wrong natural restraint at "+piece.core());
                for(int z=1;z<=26;z++){var p=piece.core().south(z);if(!l.getBlockState(p).isAir()||!l.getBlockState(p.above()).isAir()||l.getBlockState(p.below()).isAir())throw new IllegalStateException("Binding inner path is blocked at "+p);}
                result[kind]=piece;com.mojang.logging.LogUtils.getLogger().info("ELYSIUM_NATURAL_BINDING_VERIFIED kind={} source={} core={}",kind,source,piece.core());break;
            }
            if(result[kind]==null)throw new IllegalStateException("No natural binding "+kind+" within the bounded survey");
        }
        return result;
    }
    private BindingNaturalGenerationProbe(){}
}
