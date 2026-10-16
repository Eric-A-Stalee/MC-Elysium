package dev.elysium.gametest;

import com.mojang.authlib.GameProfile;
import dev.elysium.block.*;
import dev.elysium.entity.SlagChimeraEntity;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.*;
import dev.elysium.structure.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class TartarusExpeditionGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=180)
    public static void inspectionWayIsWalkableAndDarkAcrossSavedSeeds(GameTestHelper h) {
        var realm=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        h.assertTrue(realm.getMinBuildHeight()==-256&&realm.getMaxBuildHeight()==320,"Extend only the lower world boundary");
        for(long seed:new long[]{0,1,789,-2381,Long.MAX_VALUE}) {
            var plan=new TartarusDepthsPlan(128,-128,seed);var piece=new TartarusDepthsPiece(plan);
            TartarusDepthsPlan.Step previous=null;
            for(var step:plan.route()) {
                if(previous!=null)h.assertTrue(Math.abs(step.y()-previous.y())<=1&&Math.abs(step.x()-previous.x())<=1&&Math.abs(step.z()-previous.z())<=1,"The inspection route may not require flight");
                previous=step;
                if(TartarusDepthsPlan.bridgeGap(step.x(),step.z())) {h.assertTrue(piece.materialAt(step.x(),step.y(),step.z()).isAir(),"The counterweight crossing starts genuinely broken");continue;}
                var floor=piece.materialAt(step.x(),step.y(),step.z());
                h.assertTrue(floor!=null&&!floor.isAir(),"Missing route floor: "+step);
                if(plan.at(step.x(),step.y()+1,step.z()).equals(plan.heart()))continue;
                for(int y=1;y<=2;y++)h.assertTrue(piece.materialAt(step.x(),step.y()+y,step.z()).isAir(),"Blocked headroom: "+step+" +"+y);
            }
            var context=StructurePieceSerializationContext.fromLevel(realm);var tag=piece.createTag(context);
            var loaded=(TartarusDepthsPiece)ModStructures.TARTARUS_DEPTHS_PIECE.get().load(context,tag);
            h.assertTrue(tag.equals(loaded.createTag(context)),"Depth plan and seed must round-trip through Minecraft NBT");
            for(int x=100;x<151;x+=5)for(int z=190;z<251;z+=5)for(int y=-231;y<-190;y+=5) {
                var state=loaded.materialAt(x,y,z);
                if(state!=null)h.assertTrue(state.getLightEmission()==0,"The sealed chamber cannot contain maintained or ambient lamps");
            }
            h.assertTrue(piece.materialAt(0,-30,210)==null,"Deep region must not excavate through the upper geology");
        }
        h.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=120)
    public static void restoredWorkbenchConsumesMaterialsAndProducesARecoverableLamp(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,3,2));var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"LampMaker"));
        level.setBlock(pos,ModBlocks.KEEPER_WORKBENCH.get().defaultBlockState(),3);
        player.getInventory().setItem(1,new ItemStack(Items.COPPER_INGOT,2));player.getInventory().setItem(2,new ItemStack(Items.AMETHYST_SHARD,2));
        var lantern=new ItemStack(Items.LANTERN,2);
        h.assertTrue(!KeeperWorkbenchBlock.craft(level,pos,player,lantern)&&lantern.getCount()==2,"A cold bench cannot consume or craft");
        level.setBlock(pos.above(2),ModBlocks.FORGE_SERVICE_LAMP.get().defaultBlockState().setValue(ForgeServiceBlock.LIT,true),3);
        h.assertTrue(KeeperWorkbenchBlock.craft(level,pos,player,lantern),"A restored workshop must craft the expedition lamp");
        h.assertTrue(lantern.getCount()==1&&player.getInventory().getItem(1).getCount()==1&&player.getInventory().getItem(2).getCount()==1,"Exactly one lantern, copper and crystal per lamp");
        h.assertTrue(count(player,ModBlocks.KEEPERS_LAMP.get().asItem())==1&&ModBlocks.KEEPERS_LAMP.get().defaultBlockState().getLightEmission()==13,"The real item must be supplied and place useful light");
        var plan=new ForgePlan(0,0,140,0);var forge=new HephaestusForgePiece(plan);
        h.assertTrue(forge.materialAt(-42,plan.floor()+1,60).is(ModBlocks.KEEPER_WORKBENCH.get()),"A real Forge must contain its bench");
        h.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=240)
    public static void crossingRepairIsAtomicAndPersists(GameTestHelper h) {
        var level=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);var plan=new TartarusDepthsPlan(40000,40000,9);
        var pos=plan.bridgeControl();level.setBlock(pos,ModBlocks.TARTARUS_COUNTERWEIGHT.get().defaultBlockState(),3);
        var control=(DepthsControlBlockEntity)level.getBlockEntity(pos);control.bind(plan);
        for(int u=6;u<=22;u++)for(int v=189;v<=191;v++)level.setBlock(plan.at(u,-184,v),Blocks.AIR.defaultBlockState(),3);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SpanMender"));var chains=new ItemStack(Items.CHAIN,5);
        var obstacle=plan.at(20,-184,190);level.setBlock(obstacle,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
        h.assertTrue(!control.repair(player,chains)&&chains.getCount()==5&&level.getBlockState(plan.at(6,-184,190)).isAir(),"An obstruction may not consume chains or partially build");
        level.removeBlock(obstacle,false);h.assertTrue(control.repair(player,chains)&&chains.getCount()==1,"Four chains must raise the complete crossing");
        for(int u=6;u<=22;u++)h.assertTrue(!level.getBlockState(plan.at(u,-184,190)).isAir(),"Every deck column must connect");
        var loaded=(DepthsControlBlockEntity)BlockEntity.loadStatic(pos,control.getBlockState(),control.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        h.assertTrue(loaded!=null&&loaded.repaired(),"Bridge repair must survive actual block-entity loading");
        h.assertTrue(!control.repair(player,chains)&&chains.getCount()==1,"Repeated use cannot charge again");h.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=240)
    public static void encounterOwnershipAndFlameClaimsSurviveReload(GameTestHelper h) {
        var level=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);var plan=new TartarusDepthsPlan(42000,42000,11);
        var piece=new TartarusDepthsPiece(plan);var pos=plan.heart();
        for(int x=plan.x+96;x<=plan.x+160;x+=16)for(int z=plan.z+192;z<=plan.z+256;z+=16)level.getChunk(x>>4,z>>4);
        // Only the arena's relevant volume, not a second full-region generation test.
        var clip=new BoundingBox(plan.x+110,-234,plan.z+200,plan.x+146,-215,plan.z+251);
        piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),level.random,clip,new net.minecraft.world.level.ChunkPos(pos),pos);
        var heart=(DepthsControlBlockEntity)level.getBlockEntity(pos);h.assertTrue(heart!=null&&heart.plan()!=null,"Clipped generation must place and bind the heart");
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FlameSeeker"));player.setPos(Vec3.atCenterOf(pos));
        heart.use(player,new ItemStack(ModItems.CHAIN_RESONATOR.get()));var id=heart.creature();
        h.assertTrue(id!=null&&level.getEntity(id) instanceof SlagChimeraEntity,"The resonator must awaken a real creature");
        heart.use(player,new ItemStack(ModItems.CHAIN_RESONATOR.get()));h.assertTrue(id.equals(heart.creature()),"Repeated activation may not duplicate the boss");
        var mob=(SlagChimeraEntity)level.getEntity(id);mob.setHealth(72);var tag=new CompoundTag();mob.saveWithoutId(tag);
        var reloaded=ModEntities.SLAG_CHIMERA.get().create(level);reloaded.load(tag);
        h.assertTrue(reloaded.getHealth()==72&&pos.equals(reloaded.control()),"Health and encounter ownership must persist");
        mob.hurt(level.damageSources().genericKill(),10000);
        h.assertTrue(heart.defeated(),"Only the owned creature's death resolves the binding");
        h.assertTrue(heart.claim(player,ItemStack.EMPTY)&&count(player,ModItems.FIRST_FLAME.get())==1,"A participant can recover the First Flame");
        h.assertTrue(!heart.claim(player,ItemStack.EMPTY),"The same player cannot claim repeatedly");
        var saved=(DepthsControlBlockEntity)BlockEntity.loadStatic(pos,heart.getBlockState(),heart.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        h.assertTrue(saved!=null&&saved.defeated()&&id.equals(saved.creature()),"Victory and ownership must survive reload");
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(ModItems.FIRST_FLAME.get()))player.getInventory().getItem(i).setCount(0);
        var offering=new ItemStack(ModItems.ELYSIUM_FRAGMENT.get(),2);
        h.assertTrue(heart.claim(player,offering)&&offering.getCount()==1,"A lost flame can be recovered for a fragment");
        h.assertTrue(!saved.claim(player,ItemStack.EMPTY),"Saved per-player claims prevent duplicate rewards");h.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void realEyeHitboxRewardsTimingAndCoverBlocksAttacks(GameTestHelper h) {
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(2,8,2));
        var body=ModEntities.SLAG_CHIMERA.get().create(level);body.setPos(Vec3.atBottomCenterOf(base));body.setYRot(0);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"EyeTester"));player.setPos(Vec3.atBottomCenterOf(base.south(16)));
        for(var p:BlockPos.betweenClosed(base.offset(-3,0,-3),base.offset(3,13,19)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
        var source=level.damageSources().playerAttack(player);float health=body.getHealth();body.hurt(source,10);float armored=health-body.getHealth();
        body.invulnerableTime=0;body.phase(4,30);health=body.getHealth();body.eye().hurt(source,10);
        h.assertTrue(health-body.getHealth()>armored*3,"A real eye hit during recovery must beat armored body damage");
        h.assertTrue(body.eyeHeightOffset(0)==4&&body.getParts().length==1,"Recovery lowers the single targetable eye to melee height");
        body.phase(1,0);h.assertTrue(body.canStrike(player,1),"Open forward gaze must reach the player");
        for(var p:BlockPos.betweenClosed(base.offset(-4,0,8),base.offset(4,14,8)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(!body.canStrike(player,1),"Real block collision must stop an attack through cover");
        player.getAbilities().instabuild=true;h.assertTrue(!body.canStrike(player,3),"Creative observers cannot become combat targets");h.succeed();
    }
    private static int count(FakePlayer p,net.minecraft.world.item.Item item) {int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(item))n+=s.getCount();}return n;}
    private TartarusExpeditionGameTests(){}
}
