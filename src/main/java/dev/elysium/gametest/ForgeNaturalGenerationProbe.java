package dev.elysium.gametest;

import com.mojang.logging.LogUtils;
import dev.elysium.block.ForgeServiceBlock;
import dev.elysium.block.ForgeServiceBlockEntity;
import dev.elysium.forge.StructureSurvey;
import dev.elysium.registry.ModBlocks;
import dev.elysium.structure.ForgeFixtures;
import dev.elysium.structure.ForgePlan;
import dev.elysium.structure.HephaestusForgePiece;
import dev.elysium.structure.HephaestusForgeStructure;
import dev.elysium.structure.TartarusChainStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/** Ordinary world generation, including references; no explicit placement or inserted starts. */
final class ForgeNaturalGenerationProbe {
    static ForgePlan generateAndVerify(ServerLevel level) {
        var generator=level.getChunkSource().getGenerator();var id=StructureSurvey.Destination.FORGE.id();
        var structure=(HephaestusForgeStructure)level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        var placement=(RandomSpreadStructurePlacement)level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id).placement();
        for(var source:StructureSurvey.candidates(placement,level.getSeed(),new BlockPos(0,120,0))) {
            if(!placement.isStructureChunk(level.getChunkSource().getGeneratorState(),source.x,source.z))continue;
            var context=new Structure.GenerationContext(level.registryAccess(),generator,generator.getBiomeSource(),
                    level.getChunkSource().randomState(),level.getServer().getStructureManager(),level.getSeed(),source,level,structure.biomes()::contains);
            var stub=structure.findGenerationPoint(context);if(stub.isEmpty())continue;
            var preview=(HephaestusForgePiece)stub.get().getPiecesBuilder().build().pieces().getFirst();
            var hearthChunk=level.getChunk(preview.hearth());
            require(hearthChunk.getReferencesForStructure(structure).contains(source.toLong()),"Natural Forge hearth has no structure reference");
            var start=level.getChunk(source.x,source.z).getStartForStructure(structure);
            require(start!=null&&start.isValid(),"Natural Forge did not generate its saved start");
            var piece=(HephaestusForgePiece)start.getPieces().getFirst();var plan=piece.plan();var bounds=start.getBoundingBox();
            require(TartarusChainStructure.withinReferenceReach(source,bounds),"Forge exceeds normal reference reach");
            int chunks=0;
            for(int x=bounds.maxX()>>4;x>=bounds.minX()>>4;x--)for(int z=bounds.maxZ()>>4;z>=bounds.minZ()>>4;z--) {
                var chunk=level.getChunk(x,z);
                require(chunk.getReferencesForStructure(structure).contains(source.toLong()),"Natural Forge chunk has no saved reference");
                level.setChunkForced(x,z,true);chunks++;
            }
            require(level.getBlockState(piece.hearth()).is(ModBlocks.FORGE_HEARTH.get()),"Natural hearth missing");
            require(level.getBlockState(dev.elysium.block.CelestialGovernorBlock.position(plan)).is(ModBlocks.CELESTIAL_GOVERNOR.get()),"Natural governor missing");
            require(level.getBlockState(dev.elysium.forge.ForgeHammer.world(plan,1,2,103)).isAir(),"Governor approach has blocked headroom");
            for(var f:ForgeFixtures.ALL) {
                var pos=f.worldPosition(plan);var state=level.getBlockState(pos);
                if(f.local().equals(ForgeFixtures.MISSING)){require(state.isAir(),"Broken runner must start missing");continue;}
                require(state.getBlock() instanceof ForgeServiceBlock&&!state.getValue(ForgeServiceBlock.LIT),"Natural fixture missing or lit at "+pos);
                require(level.getBlockEntity(pos) instanceof ForgeServiceBlockEntity be&&be.belongsTo(plan),"Natural fixture not bound to its Forge");
            }
            LogUtils.getLogger().info("ELYSIUM_NATURAL_FORGE_VERIFIED seed={} source={} entrance={} turn={} floor={} chunks={} fixtures={}",
                    level.getSeed(),source,plan.entrance(),plan.turn(),plan.floor(),chunks,ForgeFixtures.ALL.size());
            return plan;
        }
        throw new IllegalStateException("No natural Forge within the survey search region");
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private ForgeNaturalGenerationProbe(){}
}
