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
    @GameTest(template="portal_test_empty",timeoutTicks=120)
    public static void entireChainFitsNaturalReferenceReach(GameTestHelper h) {
        for(int sx:new int[]{-19,-1,0,13})for(int sz:new int[]{-23,-1,0,17}) {
            var source=new net.minecraft.world.level.ChunkPos(sx,sz);
            for(int dx=1;dx<=13;dx+=4)for(int dz=1;dz<=13;dz+=4) {
                int x=source.getMinBlockX()+TartarusChainStructure.ANCHOR_X_OFFSET+dx;
                int z=source.getMinBlockZ()+TartarusChainStructure.ANCHOR_Z_OFFSET+dz;
                var chain=new TartarusChainPiece(new TartarusChainPlan(x,z,160,-48,true));
                var plan=TartarusDepthsPlan.belowAnchor(x,z,23);
                var depths=new TartarusDepthsPiece(plan);
                h.assertTrue(TartarusChainStructure.withinReferenceReach(source,chain.getBoundingBox())
                                &&TartarusChainStructure.withinReferenceReach(source,depths.getBoundingBox()),
                        "Every candidate offset, including negative chunks, must fit vanilla reference discovery");
                var entrance=plan.route().getFirst();
                h.assertTrue(plan.at(entrance.x(),entrance.y(),entrance.z()).equals(new BlockPos(x+10,-48,z+30)),
                        "The folded route must still meet the upper vault's southern floor");
                h.assertTrue(depths.materialAt(entrance.x(),-47,entrance.z()).isAir(),
                        "The depths piece must cut open the vault entrance");
                for(int v=TartarusDepthsPlan.ANCHOR_Z+20;v<entrance.z();v++)for(int y=-47;y<=-46;y++) {
                    var state=depths.materialAt(10,y,v);
                    if(state==null)state=chain.materialAt(10,y,v-TartarusDepthsPlan.ANCHOR_Z);
                    h.assertTrue(state!=null&&state.isAir(),"The anchor-floor approach may not be sealed by the passage's end cap");
                }
            }
        }
        var oldBounds=new BoundingBox(-87,-245,-4,166,-43,300);
        h.assertTrue(!TartarusChainStructure.withinReferenceReach(new net.minecraft.world.level.ChunkPos(0,0),oldBounds),
                "The alpha 18 footprint must be rejected, not silently truncated");
        h.succeed();
    }
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
            int exposed=0;
            for(int v=160;v<=221;v++)for(int x=-2;x<=2;x++)for(int y=-151;y<=-96;y++)
                if(piece.materialAt(x,y,v)!=null&&piece.materialAt(x,y,v).is(ModBlocks.TARTARUS_CHAIN_BLOCK.get()))exposed++;
            h.assertTrue(exposed>100,"The lower iron must emerge inside Tartarus, not terminate in unrelated upper geology");
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
        for(int u=6;u<=22;u++)for(int v=189;v<=191;v++) {
            level.setBlock(plan.at(u,-184,v),Blocks.AIR.defaultBlockState(),3);
            level.setBlock(plan.at(u,-200,v),TartarusDepthsPiece.fallenDeck(v),3);
        }
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SpanMender"));var chains=new ItemStack(Items.CHAIN,5);
        var obstacle=plan.at(20,-184,190);level.setBlock(obstacle,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
        h.assertTrue(!control.repair(player,chains)&&chains.getCount()==5&&level.getBlockState(plan.at(6,-184,190)).isAir(),"An obstruction may not consume chains or partially build");
        level.removeBlock(obstacle,false);h.assertTrue(control.repair(player,chains)&&chains.getCount()==1,"Four chains must raise the complete crossing");
        for(int u=6;u<=22;u++)h.assertTrue(!level.getBlockState(plan.at(u,-184,190)).isAir(),"Every deck column must connect");
        h.assertTrue(level.getBlockState(plan.at(14,-200,190)).isAir(),"Repair must relocate the surviving deck rather than duplicate its material");
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
        h.assertTrue(level.clip(new net.minecraft.world.level.ClipContext(Vec3.atCenterOf(pos.above(2)),Vec3.atCenterOf(plan.monster().above(4)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty())).getType()==net.minecraft.world.phys.HitResult.Type.MISS,
                "The final passage must open into the chamber, not end in a sealed collar");
        var heart=(DepthsControlBlockEntity)level.getBlockEntity(pos);h.assertTrue(heart!=null&&heart.plan()!=null,"Clipped generation must place and bind the heart");
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FlameSeeker"));player.setPos(Vec3.atCenterOf(pos));
        heart.use(player,new ItemStack(ModItems.CHAIN_RESONATOR.get()));var id=heart.creature();
        h.assertTrue(id!=null&&level.getEntity(id) instanceof SlagChimeraEntity,"The resonator must awaken a real creature");
        heart.use(player,new ItemStack(ModItems.CHAIN_RESONATOR.get()));h.assertTrue(id.equals(heart.creature()),"Repeated activation may not duplicate the boss");
        var mob=(SlagChimeraEntity)level.getEntity(id);
        h.assertTrue(mob.eyeCenter(0).distanceToSqr(Vec3.atCenterOf(pos))<mob.position().distanceToSqr(Vec3.atCenterOf(pos)),"The waking eye must face the approach, not the rear wall");
        mob.setHealth(72);var tag=new CompoundTag();mob.saveWithoutId(tag);
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
    @GameTest(template="portal_test_empty",timeoutTicks=160)
    public static void firstFlameFitsOnlyTheOriginalHearthWithoutMovingTheSun(GameTestHelper h) {
        var level=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        var structure=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("elysium","hephaestus_forge"));
        var plan=new ForgePlan(46000,46000,140,0);var piece=new HephaestusForgePiece(plan);
        var source=new net.minecraft.world.level.ChunkPos(plan.x()>>4,plan.z()>>4);
        var start=new net.minecraft.world.level.levelgen.structure.StructureStart(structure,source,0,
                new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
        level.getChunk(source.x,source.z).setStartForStructure(structure,start);
        var pos=piece.hearth();level.getChunk(pos.getX()>>4,pos.getZ()>>4).addReferenceForStructure(structure,source.toLong());
        var block=ModBlocks.FORGE_HEARTH.get();level.setBlock(pos,block.defaultBlockState(),3);level.setBlock(pos.east(3),block.defaultBlockState(),3);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"Homeward"));var flame=new ItemStack(ModItems.FIRST_FLAME.get());
        long time=level.getDayTime();
        h.assertTrue(!block.receiveFlame(player,pos.east(3),flame)&&flame.getCount()==1,"A moved or copied hearth cannot consume the expedition reward");
        h.assertTrue(block.receiveFlame(player,pos,flame)&&flame.isEmpty(),"The saved original hearth must accept exactly one flame");
        h.assertTrue(level.getBlockState(pos).getValue(DormantHearthBlock.FLAME)&&level.getBlockState(pos).getLightEmission()==8,"Returning the flame lights its physical socket");
        h.assertTrue(level.getDayTime()==time&&level.dimensionType().fixedTime().orElseThrow()==11500,"Completing this chapter may not start the sunset");
        var second=new ItemStack(ModItems.FIRST_FLAME.get());h.assertTrue(!block.receiveFlame(player,pos,second)&&second.getCount()==1,"A filled socket may not consume another reward");
        h.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void eyeLightWorksInAirAndExpiresWithItsSource(GameTestHelper h) {
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(2,160,2));
        for(int x=(base.getX()-16)>>4;x<=(base.getX()+16)>>4;x++)for(int z=(base.getZ()-16)>>4;z<=(base.getZ()+16)>>4;z++) {
            level.setChunkForced(x,z,true);level.getChunk(x,z);
        }
        var mob=ModEntities.SLAG_CHIMERA.get().create(level);mob.bind(base,Vec3.atBottomCenterOf(base));level.addFreshEntity(mob);mob.tick();
        var light=BlockPos.containing(mob.eyeCenter(0));
        h.runAfterDelay(12,()->{
            h.assertTrue(level.getBlockState(light).is(ModBlocks.CHIMERA_GLOW.get()),"The eye must place a real light even in an otherwise empty chunk section: expected="+light+", actual="+level.getBlockState(light)+", eye="+mob.eyeCenter(0)+", tick="+mob.tickCount);
            h.assertTrue(!level.getBlockState(light).isAir()&&level.getBlockState(light).getLightEmission()==8,"Invisible lighting must remain a non-air block for section bookkeeping");
            h.assertTrue(level.getBlockState(light).getShape(level,light).isEmpty(),"Invisible lighting must not intercept the player’s mining or attack ray");
            mob.discard();
            h.runAfterDelay(25,()->{h.assertTrue(!level.getBlockState(light).is(ModBlocks.CHIMERA_GLOW.get()),"An unloaded or removed creature may not leave permanent lights");h.succeed();});
        });
    }
    @GameTest(template="portal_test_empty",timeoutTicks=140)
    public static void carriedLampUsesRealLightWithoutReplacingWalls(GameTestHelper h) {
        var level=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        var player=h.makeMockServerPlayerInLevel();var pos=new BlockPos(48000,-181,48000);
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
        player.teleportTo(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(ModBlocks.KEEPERS_LAMP.get()));
        dev.elysium.forge.KeeperLampLight.illuminate(player);
        h.assertTrue(level.getBlockState(pos.above()).is(ModBlocks.CHIMERA_GLOW.get()),"An offhand lamp must make real light in the depths");
        level.setBlock(pos.above(),Blocks.DIAMOND_BLOCK.defaultBlockState(),3);dev.elysium.forge.KeeperLampLight.illuminate(player);
        h.assertTrue(level.getBlockState(pos.above()).is(Blocks.DIAMOND_BLOCK),"Carried light may never overwrite a solid block");
        level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);dev.elysium.forge.KeeperLampLight.illuminate(player);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);
        h.runAfterDelay(30,()->{
            try {h.assertTrue(!level.getBlockState(pos.above()).is(ModBlocks.CHIMERA_GLOW.get()),"Putting away the lamp must extinguish its temporary light");h.succeed();}
            finally {level.getServer().getPlayerList().remove(player);}
        });
    }
    private static int count(FakePlayer p,net.minecraft.world.item.Item item) {int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(item))n+=s.getCount();}return n;}
    private TartarusExpeditionGameTests(){}
}
