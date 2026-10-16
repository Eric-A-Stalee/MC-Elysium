package dev.elysium.gametest;

import com.mojang.logging.LogUtils;
import dev.elysium.block.DepthsControlBlockEntity;
import dev.elysium.registry.ModBlocks;
import dev.elysium.structure.TartarusChainPiece;
import dev.elysium.structure.TartarusChainStructure;
import dev.elysium.structure.TartarusDepthsPiece;
import dev.elysium.structure.TartarusDepthsPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/** Uses the ordinary chunk pipeline, including structure references and decoration.
 * Never calls postProcess/placeInChunk or inserts structure starts/references. */
final class TartarusNaturalGenerationProbe {
    static TartarusDepthsPlan generateAndVerify(ServerLevel level) {
        var generator = level.getChunkSource().getGenerator();
        var id = ResourceLocation.fromNamespaceAndPath("elysium", "tartarus_chain");
        var structure = (TartarusChainStructure) level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE).get(id);
        var placement = (RandomSpreadStructurePlacement) level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE_SET).get(id).placement();
        for (int index = 0; index < 6400; index++) {
            var source = placement.getPotentialStructureChunk(level.getSeed(),
                    (index % 80 - 40) * 48, (index / 80 - 40) * 48);
            var context = new Structure.GenerationContext(level.registryAccess(), generator,
                    generator.getBiomeSource(), level.getChunkSource().randomState(),
                    level.getServer().getStructureManager(), level.getSeed(), source,
                    level, structure.biomes()::contains);
            var stub = structure.findGenerationPoint(context);
            if (stub.isEmpty()) continue;
            var preview = (TartarusDepthsPiece) stub.get().getPiecesBuilder().build().pieces().get(1);
            var expected = preview.plan();

            // Arrive at the far end first. This fails for alpha 18: the distant
            // chunk never discovers the chain's start and remains plain stone.
            var heartChunk = level.getChunk(expected.heart());
            require(heartChunk.getReferencesForStructure(structure).contains(source.toLong()),
                    "Natural Tartarus heart chunk " + heartChunk.getPos()
                            + " has no reference to start " + source
                            + "; actual block=" + level.getBlockState(expected.heart()));
            var start = level.getChunk(source.x, source.z).getStartForStructure(structure);
            require(start != null && start.isValid(), "Natural chain start was not generated");
            var chain = ((TartarusChainPiece) start.getPieces().getFirst()).plan();
            var piece = (TartarusDepthsPiece) start.getPieces().get(1);
            var plan = piece.plan();
            require(plan.heart().equals(expected.heart()) && plan.seed == expected.seed,
                    "Natural start differs from deterministic candidate");
            var bounds = start.getBoundingBox();
            int chunks = 0;
            for (int x = bounds.maxX() >> 4; x >= bounds.minX() >> 4; x--) {
                for (int z = bounds.maxZ() >> 4; z >= bounds.minZ() >> 4; z--) {
                    var chunk = level.getChunk(x, z);
                    require(chunk.getReferencesForStructure(structure).contains(source.toLong()),
                            "Missing natural structure reference in " + new ChunkPos(x, z));
                    level.setChunkForced(x, z, true);
                    chunks++;
                }
            }
            require(level.getBlockState(new BlockPos(chain.x(), chain.floor() + 6, chain.z()))
                    .is(ModBlocks.TARTARUS_CHAIN_BLOCK.get()), "Anchor did not generate");
            for (int v = TartarusDepthsPlan.ANCHOR_Z + 20; v < plan.route().getFirst().z(); v++)
                for (int y = -47; y <= -46; y++) {
                    var pos = plan.at(10, y, v);
                    require(level.getBlockState(pos).isAir(), "Anchor-to-passage mouth is blocked at " + pos);
                }
            int steps = 0;
            for (var step : plan.route()) {
                var pos = plan.at(step.x(), step.y(), step.z());
                var floor = level.getBlockState(pos);
                if (TartarusDepthsPlan.bridgeGap(step.x(), step.z())) {
                    require(floor.isAir(), "Natural crossing should initially be broken: " + pos);
                    continue;
                }
                require(floor.equals(piece.materialAt(step.x(), step.y(), step.z())),
                        "Natural route floor differs at " + pos + ": " + floor);
                for (int dy = 1; dy <= 2; dy++) {
                    if (pos.above(dy).equals(plan.heart())) continue;
                    require(level.getBlockState(pos.above(dy)).isAir(),
                            "Natural route is blocked at " + pos.above(dy)
                                    + ": " + level.getBlockState(pos.above(dy)));
                }
                steps++;
            }
            for (var pos : new BlockPos[]{plan.heart(), plan.bridgeControl()}) {
                require(level.getBlockEntity(pos) instanceof DepthsControlBlockEntity control
                                && control.plan() != null,
                        "Natural control block entity is missing or unbound at " + pos);
            }
            require(level.getBlockState(plan.monster()).isAir(), "Creature chamber remains solid");
            int samples = 0;
            for (int u = TartarusDepthsPlan.MIN_X + 2; u <= TartarusDepthsPlan.MAX_X; u += 8)
                for (int v = TartarusDepthsPlan.MIN_Z + 1; v <= TartarusDepthsPlan.MAX_Z; v += 8)
                    for (int y = -242; y <= -64; y += 9) {
                        var wanted = piece.materialAt(u, y, v);
                        if (wanted == null) continue;
                        var pos = plan.at(u, y, v);
                        var actual = level.getBlockState(pos);
                        require(wanted.isAir() ? actual.isAir() : wanted.equals(actual),
                                "Natural basin/chamber differs at " + pos + ": " + actual + " expected " + wanted);
                        samples++;
                    }
            LogUtils.getLogger().info("ELYSIUM_NATURAL_TARTARUS_VERIFIED seed={} source={} anchor={} depthsOrigin={},{} chunks={} routeSteps={} materialSamples={} heart={}",
                    level.getSeed(), source, new BlockPos(chain.x(), chain.floor(), chain.z()),
                    plan.x, plan.z, chunks, steps, samples, plan.heart());
            return plan;
        }
        throw new IllegalStateException("No natural chain found in bounded candidate survey");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private TartarusNaturalGenerationProbe() {}
}
