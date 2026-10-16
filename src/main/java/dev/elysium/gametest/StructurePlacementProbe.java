package dev.elysium.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** GameTest's WorldOptions disable automatic structures. Exercise the real
 * start, serialization and clipped placement APIs explicitly on its real terrain. */
final class StructurePlacementProbe {
    static StructureStart place(GameTestHelper helper, ServerLevel level, Structure structure, ChunkPos source) {
        var generator = level.getChunkSource().getGenerator();
        var start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getServer().getStructureManager(), level.getSeed(),
                source, 0, level, structure.biomes()::contains);
        helper.assertTrue(start.isValid(), "The selected terrain candidate must pass Structure.generate");
        var context = StructurePieceSerializationContext.fromLevel(level);
        var tag = start.createTag(context, source);
        var saved = StructureStart.loadStaticStart(context, tag, level.getSeed());
        helper.assertTrue(saved != null && saved.isValid() && saved.createTag(context, source).equals(tag),
                "The complete structure start must survive Minecraft's NBT loader");
        var box = saved.getBoundingBox();
        helper.assertTrue(dev.elysium.structure.TartarusChainStructure.withinReferenceReach(source,box),
                "Explicit placement must not hide a structure that extends beyond natural reference reach: "+box);
        // Reverse coordinate order; every piece receives only the current chunk's clip.
        for (int x = box.maxX() >> 4; x >= box.minX() >> 4; x--) {
            for (int z = box.maxZ() >> 4; z >= box.minZ() >> 4; z--) {
                var loaded=level.getChunk(x, z);
                var existingEntities=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
                for(var pos:loaded.getBlockEntitiesPos())existingEntities.put(pos,level.getBlockState(pos));
                var chunk = new ChunkPos(x, z);
                var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                saved.placeInChunk(level, level.structureManager(), generator,
                        RandomSource.create(chunk.toLong() ^ level.getSeed()), clip, chunk);
                existingEntities.forEach((pos,before)->{
                    var after=level.getBlockState(pos);
                    if(!before.equals(after))com.mojang.logging.LogUtils.getLogger().info(
                            "Explicit structure probe replaced existing block entity at {}: {} -> {}",pos,before,after);
                });
            }
        }
        return saved;
    }

    private StructurePlacementProbe() {}
}
