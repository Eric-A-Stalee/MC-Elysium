package dev.elysium.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import dev.elysium.forge.*;
import dev.elysium.item.ResonanceItem;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModItems;
import dev.elysium.registry.ModStructures;
import dev.elysium.structure.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class ForgeGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void buriedForgeHasConnectedRoomsAndUpperGalleries(GameTestHelper helper) {
        helper.assertTrue(ForgePlan.find((x,z)->75,0,0,63,-64).isEmpty(),"Low meadows must reject the Forge");
        helper.assertTrue(ForgePlan.find((x,z)->160,0,0,63,-64).isEmpty(),"A flat plateau must not pretend to be a mountain entrance");
        for(int turn=0;turn<4;turn++) {
            var orientation=new ForgePlan(160,-320,140,turn);
            var fitted=ForgePlan.find((x,z)->141+Math.max(0,orientation.localV(x,z)-2)*2,160,-320,63,-64).orElseThrow();
            helper.assertTrue(fitted.turn()==turn,"The entrance must face the actual descending shoulder");
            var piece=new HephaestusForgePiece(fitted);
            var context=StructurePieceSerializationContext.fromLevel(helper.getLevel());
            var tag=piece.createTag(context);
            var loaded=(HephaestusForgePiece)ModStructures.HEPHAESTUS_FORGE_PIECE.get().load(context,tag);
            helper.assertTrue(tag.equals(loaded.createTag(context)),"Forge orientation and complete geometry must survive registered loading");
            for(int u=-46;u<=46;u++)for(int v=-5;v<=119;v++) {
                helper.assertTrue(fitted.localU(fitted.worldX(u,v),fitted.worldZ(u,v))==u
                        &&fitted.localV(fitted.worldX(u,v),fitted.worldZ(u,v))==v,"Rotation must round-trip every column");
            }
            Direction descent=Direction.NORTH;
            for(int i=0;i<turn;i++)descent=descent.getClockWise();
            helper.assertTrue(loaded.materialAt(0,fitted.stairFloor(12),12).getValue(StairBlock.FACING)==descent,
                    "Descending stairs must face uphill after rotation");
            if(turn!=0)continue;
            var seen=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();
            queue.add(new BlockPos(ForgePlan.approachCenter(-6),fitted.stairFloor(-6)+1,-6));
            while(!queue.isEmpty()) {
                var at=queue.remove();if(!seen.add(at))continue;
                for(Direction d:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=1;dy++) {
                    var next=at.relative(d).above(dy);
                    if(!seen.contains(next)&&walkable(loaded,next))queue.add(next);
                }
            }
            int b=fitted.floor();
            for(var goal:List.of(new BlockPos(0,b+1,60),new BlockPos(-37,b+1,51),new BlockPos(37,b+1,60),
                    new BlockPos(-8,b+1,76),new BlockPos(0,b+1,111),new BlockPos(-22,b+9,70),new BlockPos(22,b+9,70),
                    new BlockPos(-4,b+1,52),new BlockPos(-39,b+1,55),new BlockPos(34,b+1,65)))
                helper.assertTrue(seen.contains(goal),"A player-sized path must connect the entrance to "+goal);
            helper.assertTrue(loaded.materialAt(0,b+13,76).is(Blocks.POLISHED_BLACKSTONE),"The hammer must hang above the striking bed");
            helper.assertTrue(loaded.materialAt(0,fitted.entranceFloor()+10,70)==null,"Unexcavated mountain roof must remain natural rock");
        }
        helper.succeed();
    }
    private static boolean walkable(HephaestusForgePiece piece,BlockPos p) {
        var feet=piece.materialAt(p.getX(),p.getY(),p.getZ());
        var head=piece.materialAt(p.getX(),p.getY()+1,p.getZ());
        var floor=piece.materialAt(p.getX(),p.getY()-1,p.getZ());
        return passable(feet)&&passable(head)&&floor!=null&&!passable(floor)&&floor.getFluidState().isEmpty();
    }
    private static boolean passable(net.minecraft.world.level.block.state.BlockState state) {
        return state!=null&&(state.isAir()||state.is(Blocks.RAIL)||state.is(ModBlocks.FORGE_DEBRIS.get()));
    }

    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void forgePlacementHonoursChunkClipsAndKeepsUncutRock(GameTestHelper helper) {
        var level=helper.getLevel();var center=helper.absolutePos(new BlockPos(2,90,2));
        var piece=new HephaestusForgePiece(new ForgePlan(center.getX(),center.getZ(),center.getY(),1));
        var inside=piece.hearth();var outside=inside.east();var roof=piece.at(0,center.getY()+10,61);
        level.setBlock(outside,Blocks.RED_CONCRETE.defaultBlockState(),2);
        level.setBlock(roof,Blocks.STONE.defaultBlockState(),2);
        var clip=new BoundingBox(inside.getX(),inside.getY(),inside.getZ(),inside.getX(),roof.getY(),inside.getZ());
        piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),level.random,clip,new ChunkPos(inside),inside);
        helper.assertTrue(level.getBlockState(inside).is(ModBlocks.FORGE_HEARTH.get()),"The rotated hearth must be placed at its saved location");
        helper.assertTrue(level.getBlockState(outside).is(Blocks.RED_CONCRETE),"A clipped piece may not write the neighbouring column");
        helper.assertTrue(level.getBlockState(roof).is(Blocks.STONE),"Uncut geology above the foundry must survive placement");
        helper.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=160)
    public static void discoveriesArePersonalPersistentAndReplaceable(GameTestHelper helper) {
        var realm=realm(helper);var player=reader(realm,"ForgeReader");var second=reader(realm,"OtherReader");
        var pos=new BlockPos(24,140,24);var hit=hit(pos);
        var record=ModBlocks.FORGE_ARCHIVE.get().defaultBlockState();
        var book=new ItemStack(Items.BOOK);player.setItemInHand(InteractionHand.MAIN_HAND,book);
        record.useItemOn(book,realm,player,InteractionHand.MAIN_HAND,hit);
        helper.assertTrue(book.getCount()==1&&count(player,Items.WRITTEN_BOOK)==1&&count(player,ModItems.FORGE_SURVEY.get())==1,
                "First reading is free even with a book in hand, and supplies the survey");
        record.useWithoutItem(realm,player,hit);record.useWithoutItem(realm,second,hit);
        helper.assertTrue(count(player,Items.WRITTEN_BOOK)==1&&count(second,Items.WRITTEN_BOOK)==1,"Re-reading must not duplicate rewards or exclude another player");
        record.useItemOn(book,realm,player,InteractionHand.MAIN_HAND,hit);
        helper.assertTrue(book.isEmpty()&&count(player,Items.WRITTEN_BOOK)==2,"A later deliberate copy must consume one blank book");
        var hearth=ModBlocks.FORGE_HEARTH.get().defaultBlockState();
        hearth.useWithoutItem(realm,player,hit);hearth.useWithoutItem(realm,player,hit);hearth.useWithoutItem(realm,second,hit);
        helper.assertTrue(count(player,ModItems.CHAIN_RESONATOR.get())==1&&count(second,ModItems.CHAIN_RESONATOR.get())==1,
                "Each visitor must receive exactly one instrument");
        var relic=first(player,ModItems.CHAIN_RESONATOR.get());var identity=ResonanceItem.identity(relic);
        var target=new BlockPos(-800,-42,1200);ResonanceItem.bind(relic,target);
        var reloaded=ItemStack.parse(realm.registryAccess(),relic.save(realm.registryAccess())).orElseThrow();
        helper.assertTrue(ResonanceItem.identifies(reloaded,identity)&&ResonanceItem.target(reloaded).orElseThrow().equals(target)
                &&BlockPos.of(ResonanceItem.data(reloaded).getLong("elysium_forge")).equals(pos),"Item save/reload must retain target, hearth and survey identity");
        var written=first(player,Items.WRITTEN_BOOK);
        var pages=ItemStack.parse(realm.registryAccess(),written.save(realm.registryAccess())).orElseThrow().get(DataComponents.WRITTEN_BOOK_CONTENT);
        helper.assertTrue(pages!=null&&pages.pages().size()==4&&pages.resolved(),"Archive books must remain readable written books after save/reload");
        var crystal=new ItemStack(Items.AMETHYST_SHARD,2);player.setItemInHand(InteractionHand.OFF_HAND,crystal);
        hearth.useItemOn(crystal,realm,player,InteractionHand.OFF_HAND,hit);
        helper.assertTrue(crystal.getCount()==2,"A replacement may not consume crystal when the instrument is still held");
        relic.setCount(0);hearth.useItemOn(crystal,realm,player,InteractionHand.OFF_HAND,hit);
        helper.assertTrue(crystal.getCount()==1&&count(player,ModItems.CHAIN_RESONATOR.get())==1,"A lost instrument must be recoverable with one amethyst shard");
        var saved=ForgeDiscoveries.get(realm).save(new CompoundTag(),realm.registryAccess());
        var loaded=ForgeDiscoveries.load(saved,realm.registryAccess());
        helper.assertTrue(loaded.has(player.getUUID(),ForgeDiscoveries.SURVEY)&&loaded.has(second.getUUID(),ForgeDiscoveries.HEARTH)
                &&!loaded.discover(player.getUUID(),ForgeDiscoveries.HEARTH),"Discovery state must survive world reload without granting again");
        var elsewhere=reader(helper.getLevel(),"OutsideReader");
        hearth.useWithoutItem(helper.getLevel(),elsewhere,hit);
        helper.assertTrue(count(elsewhere,ModItems.CHAIN_RESONATOR.get())==0,"Moving the hearth to another dimension must not create an Elysium reward");
        helper.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=160)
    public static void resonatorRequiresTheRealBuriedAnchor(GameTestHelper helper) {
        var realm=realm(helper);
        // NeoForge deliberately suppresses advancements for FakePlayer, so exercise
        // the full interaction with Minecraft's connected test player here.
        var player=helper.makeMockServerPlayerInLevel();
        try {
        var structure=realm.registryAccess().registryOrThrow(Registries.STRUCTURE).get(StructureSurvey.Destination.CHAIN.id());
        var plan=new TartarusChainPlan(100000,100000,160,-48,true,12,22);
        var piece=new TartarusChainPiece(plan);var source=new ChunkPos(plan.x()>>4,plan.z()>>4);
        var start=new StructureStart(structure,source,0,new PiecesContainer(List.of(piece)));
        realm.getChunk(source.x,source.z).setStartForStructure(structure,start);
        realm.getChunk(source.x,source.z).addReferenceForStructure(structure,source.toLong());
        var anchor=new BlockPos(plan.x(),plan.floor()+6,plan.z());var high=anchor.above(35);var fake=anchor.offset(32,0,0);
        player.teleportTo(realm,anchor.getX()+.5,anchor.getY()+2,anchor.getZ()+.5,0,0);
        var relic=new ItemStack(ModItems.CHAIN_RESONATOR.get());player.setItemInHand(InteractionHand.MAIN_HAND,relic);
        for(var pos:List.of(fake,high,anchor)) {
            realm.setBlock(pos,ModBlocks.TARTARUS_CHAIN_BLOCK.get().defaultBlockState(),2);
            ModItems.CHAIN_RESONATOR.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit(pos)));
            helper.assertTrue(ForgeDiscoveries.get(realm).has(player.getUUID(),ForgeDiscoveries.ANCHOR)==pos.equals(anchor),
                    "A carried chain block and a high link must not substitute for the buried anchor");
        }
        ModItems.CHAIN_RESONATOR.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit(anchor)));
        helper.assertTrue(count(player,Items.WRITTEN_BOOK)==1&&ResonanceItem.data(relic).getBoolean("elysium_anchor_heard"),
                "The anchor records its echo once and marks the instrument");
        var advancement=realm.getServer().getAdvancements().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("elysium","voice_in_iron"));
        helper.assertTrue(advancement!=null&&player.getAdvancements().getOrStartProgress(advancement).isDone(),"The final discovery must award its advancement");
        helper.succeed();
        } finally {realm.getServer().getPlayerList().remove(player);}
    }

    @GameTest(template="portal_test_empty",timeoutTicks=160)
    public static void existingTownScholarsOfferTheSurveyOnlyInElysium(GameTestHelper helper) {
        var realm=realm(helper);var player=reader(realm,"TownReader");
        for(var profession:List.of(VillagerProfession.CARTOGRAPHER,VillagerProfession.LIBRARIAN,VillagerProfession.FARMER)) {
            var villager=new Villager(EntityType.VILLAGER,realm);
            villager.setVillagerData(villager.getVillagerData().setProfession(profession));
            var event=new PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,villager);
            ForgeTradeEvents.trade(event);ForgeTradeEvents.trade(event);
            long offers=villager.getOffers().stream().filter(o->o.getResult().is(ModItems.FORGE_SURVEY.get())).count();
            helper.assertTrue(offers==(profession==VillagerProfession.FARMER?0:1),"Scholar interactions must add one supplemental offer without duplicating it");
        }
        var outside=new Villager(EntityType.VILLAGER,helper.getLevel());
        outside.setVillagerData(outside.getVillagerData().setProfession(VillagerProfession.LIBRARIAN));
        ForgeTradeEvents.trade(new PlayerInteractEvent.EntityInteract(reader(helper.getLevel(),"OutsideTrader"),InteractionHand.MAIN_HAND,outside));
        helper.assertTrue(outside.getOffers().stream().noneMatch(o->o.getResult().is(ModItems.FORGE_SURVEY.get())),"Overworld librarian trades must remain untouched");
        helper.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=2400)
    public static void naturalForgeCanBeFoundWithinOneSurveysReach(GameTestHelper helper) {
        var realm=realm(helper);var generator=realm.getChunkSource().getGenerator();var id=StructureSurvey.Destination.FORGE.id();
        var structure=(HephaestusForgeStructure)realm.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        var placement=(RandomSpreadStructurePlacement)realm.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id).placement();
        var candidates=StructureSurvey.candidates(placement,realm.getSeed(),new BlockPos(0,120,0));
        helper.assertTrue(candidates.size()==StructureSurvey.MAX_CANDIDATES&&new HashSet<>(candidates).size()==candidates.size(),"The survey must have a bounded unique candidate list");
        class Probe implements Runnable {
            int index,eligible;
            @Override public void run() {
                for(int batch=0;batch<3&&index<candidates.size();batch++,index++) {
                    var chunk=candidates.get(index);
                    if(!placement.isStructureChunk(realm.getChunkSource().getGeneratorState(),chunk.x,chunk.z))continue;
                    eligible++;
                    var context=new Structure.GenerationContext(realm.registryAccess(),generator,generator.getBiomeSource(),
                            realm.getChunkSource().randomState(),realm.getServer().getStructureManager(),realm.getSeed(),chunk,realm,structure.biomes()::contains);
                    if(structure.findGenerationPoint(context).isEmpty())continue;
                    // A plausible new plan must never invent a Forge in a completed pre-upgrade chunk.
                    realm.getChunk(chunk.x,chunk.z);
                    helper.assertTrue(StructureSurvey.probe(realm,structure,new StructureSurvey.Candidate(chunk,placement)).isEmpty(),
                            "A surveyed noise plan is not an existing structure start");
                    var start=StructurePlacementProbe.place(helper,realm,structure,chunk);
                    var piece=(HephaestusForgePiece)start.getPieces().getFirst();var p=piece.plan();
                    realm.getChunk(chunk.x,chunk.z).setStartForStructure(structure,start);
                    var target=StructureSurvey.probe(realm,structure,new StructureSurvey.Candidate(chunk,placement));
                    helper.assertTrue(target.isPresent()&&target.get().equals(p.entrance()),
                            "Survey must return the saved doorway, not a guessed underground centre");
                    helper.assertTrue(realm.getBlockState(piece.hearth()).is(ModBlocks.FORGE_HEARTH.get())
                            &&realm.getBlockState(piece.ledger()).is(ModBlocks.FORGE_LEDGER.get()),"Natural clipped placement must deliver the progression blocks");
                    helper.assertTrue(realm.getBlockState(piece.at(0,p.floor()+1,55)).isAir(),"Natural terrain must be excavated into the connected foundry");
                    exportSections(piece);
                    LogUtils.getLogger().info("Elysium natural Forge: seed={}, chunk={}, entrance={}, turn={}, floor={}, candidates={}",
                            realm.getSeed(),chunk,target.get(),p.turn(),p.floor(),index+1);
                    helper.succeed();return;
                }
                if(index>=candidates.size())LogUtils.getLogger().info("Forge survey exhausted {} eligible placements of {} candidates",eligible,index);
                helper.assertTrue(index<candidates.size(),"No buried Forge was discoverable within the actual folio search radius");
                helper.runAfterDelay(1,()->run());
            }
        }
        new Probe().run();
    }

    private static ServerLevel realm(GameTestHelper helper){return Objects.requireNonNull(helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION));}
    private static FakePlayer reader(ServerLevel level,String name){return new FakePlayer(level,new GameProfile(UUID.randomUUID(),name));}
    private static BlockHitResult hit(BlockPos pos){return new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);}
    private static int count(ServerPlayer player,Item item){int n=0;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(item))n+=player.getInventory().getItem(i).getCount();return n;}
    private static ItemStack first(ServerPlayer player,Item item){for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(item))return player.getInventory().getItem(i);throw new IllegalStateException("Missing "+item);}
    /** Actual block-state slices for visual review, included in the CI log artifact. */
    private static void exportSections(HephaestusForgePiece piece) {
        var out=new com.google.gson.JsonObject();var slices=new com.google.gson.JsonArray();int b=piece.plan().floor();
        for(int h:new int[]{1,9})for(int u=-46;u<=46;u++)for(int v=-5;v<=119;v++) {
            var state=piece.materialAt(u,b+h,v);if(state==null)continue;
            var row=new com.google.gson.JsonArray();row.add(u);row.add(h);row.add(v);
            row.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());slices.add(row);
        }
        for(int y=b-2;y<=piece.plan().entranceFloor()+11;y++)for(int v=-5;v<=119;v++) {
            var state=piece.materialAt(0,y,v);if(state==null)continue;
            var row=new com.google.gson.JsonArray();row.add(0);row.add(y-b);row.add(v);
            row.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());slices.add(row);
        }
        out.add("blocks",slices);out.addProperty("floor",b);out.addProperty("turn",piece.plan().turn());
        try {var path=java.nio.file.Path.of("logs/forge-sections.json");java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.writeString(path,out.toString());}
        catch(java.io.IOException e){throw new IllegalStateException("Could not export Forge review slices",e);}
    }
    private ForgeGameTests(){}
}
