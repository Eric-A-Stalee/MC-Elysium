package dev.elysium.gametest;

import dev.elysium.portal.ElysiumTravel;
import dev.elysium.structure.*;
import dev.elysium.worldgen.*;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class UnderworldGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=500)
    public static void continuousStrataHaveRealBiomesAndDryWalkableAir(GameTestHelper h) {
        var realm=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        h.assertTrue(realm.getChunkSource().getGenerator() instanceof StratifiedChunkGenerator,"The registered generator must survive world loading");
        var generator=(StratifiedChunkGenerator)realm.getChunkSource().getGenerator();
        var random=realm.getChunkSource().randomState();var terrain=generator.terrain(random);
        var again=new TartarusTerrain(random);var regions=EnumSet.noneOf(TartarusTerrain.Region.class);
        int inspected=0;
        for(int x=-8192;x<=8192;x+=256)for(int z=-8192;z<=8192;z+=256) {
            var column=terrain.column(x,z);regions.add(column.region);
            h.assertTrue(column.roof-column.floor>140,"Tartarus needs world-scale vertical clearance");
            h.assertTrue(column.roof<TartarusTerrain.TOP&&column.floor>TartarusTerrain.MIN_Y+4,"Both layers retain a solid separation and bedrock floor");
            h.assertTrue(column.floor==again.floor(x,z)&&column.roof==again.roof(x,z),"Seeded sampling must be deterministic");
        }
        h.assertTrue(regions.size()==4,"A bounded regional survey must find all four underworld climates");
        for(int x:new int[]{-1025,-16,0,1039})for(int z:new int[]{-1025,1039}) {
            var column=terrain.column(x,z);var base=generator.getBaseColumn(x,z,realm,random);
            for(int y:new int[]{-636,-590,column.floor,column.roof-5,-249}) {
                h.assertTrue(base.getBlock(y).equals(column.at(y)),"Structure survey and generated geology must share the same formula");
                var pos=new BlockPos(x,y,z);
                var actual=realm.getBlockState(pos);var wanted=column.at(y);
                // Empty chunk sections canonicalize cave air to ordinary air.
                h.assertTrue(wanted.isAir()?actual.isAir():actual.equals(wanted),"Ordinary generated chunks must contain the surveyed underworld at "+pos+": "+actual+" expected "+wanted);
                inspected++;
            }
            var biome=realm.getNoiseBiome(x >> 2,-100,z >> 2);
            h.assertTrue(biome.unwrapKey().orElseThrow().location().getPath().equals(switch(terrain.region((x >> 2) << 2,(z >> 2) << 2)){
                case ASH_WASTES->"ash_wastes";case CHAIN_FOREST->"chain_forest";case HOLLOW_MIRE->"hollow_mire";case TITAN_GRAVE->"titan_grave";}),"Regional terrain and biome must agree");
            h.assertTrue(realm.getBlockState(new BlockPos(x,-248,z)).getFluidState().isEmpty(),"Vanilla lava cannot flood Tartarus");
        }
        h.assertTrue(inspected==40,"All positive and negative boundary samples ran");h.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void windingDescentIsContinuousAndSurvivesSerialization(GameTestHelper h) {
        var realm=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        var terrain=((StratifiedChunkGenerator)realm.getChunkSource().getGenerator()).terrain(realm.getChunkSource().randomState());
        for(int x:new int[]{-1024,0,2096}) {
            var piece=new UnderworldDescentPiece(new TartarusDepthsPlan(x,-300,7),terrain);
            var context=StructurePieceSerializationContext.fromLevel(realm);
            var restored=new UnderworldDescentPiece(piece.createTag(context));
            h.assertTrue(piece.createTag(context).equals(restored.createTag(context)),"Surveyed stair landing survives NBT");
            UnderworldDescentPiece.Step last=null;
            for(var step:restored.route()) {
                if(last!=null)h.assertTrue(Math.abs(step.y()-last.y())<=1&&Math.abs(step.x()-last.x())+Math.abs(step.z()-last.z())<=1,"Descent cannot require flight");
                last=step;var pos=restored.at(step);
                h.assertTrue(!restored.materialAt(pos.getX(),pos.getY(),pos.getZ()).isAir(),"Every stair centre has a floor");
                for(int dy=1;dy<=2;dy++)h.assertTrue(restored.materialAt(pos.getX(),pos.getY()+dy,pos.getZ()).isAir(),"Every stair centre has headroom: "+pos);
            }
            h.assertTrue(last!=null&&last.y()<-400,"Stair reaches the world beneath the old chamber");
        }
        h.succeed();
    }
}
