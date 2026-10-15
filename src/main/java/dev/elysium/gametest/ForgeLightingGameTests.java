package dev.elysium.gametest;

import com.mojang.authlib.GameProfile;
import dev.elysium.block.ForgeServiceBlock;
import dev.elysium.block.ForgeServiceBlockEntity;
import dev.elysium.forge.ForgeLighting;
import dev.elysium.registry.ModBlocks;
import dev.elysium.structure.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class ForgeLightingGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void deepRuinHasConcealedAccessAndPhysicalRoutes(GameTestHelper helper) {
        for(int turn=0;turn<4;turn++) {
            var p=new ForgePlan(240,-192,140,turn);var piece=new HephaestusForgePiece(p);
            helper.assertTrue(p.entranceFloor()-p.floor()==72&&ForgeDescent.ROUTE.size()>=160,"The descent must be deep and substantial");
            for(var f:ForgeFixtures.ALL) {
                var state=piece.materialAt(f.u(),p.floor()+f.h(),f.v());
                if(f.local().equals(ForgeFixtures.MISSING)) {helper.assertTrue(state!=null&&state.isAir(),"The shrine branch must have an actual missing piece");continue;}
                helper.assertTrue(state!=null&&state.getBlock() instanceof ForgeServiceBlock,"Every physical connection must survive geometry: "+f);
                helper.assertTrue(!state.getValue(ForgeServiceBlock.LIT),"The network must start dormant");
            }
            helper.assertTrue(!piece.materialAt(0,p.entranceFloor()+1,1).isAir(),"The old doorway must remain buried");
            helper.assertTrue(piece.materialAt(0,p.floor()+5,76).is(ModBlocks.FORGE_IRON.get()),"The original hammer/anvil composition must survive");
            helper.assertTrue(piece.materialAt(0,p.entranceFloor()+10,76)==null,"The great vault must stay buried");
        }
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=180)
    public static void fuelIgnitionAirflowAndExhaustionUseRealInteractions(GameTestHelper helper) {
        var level=helper.getLevel();var p=plan(helper,0);placeAll(level,p);
        var source=service(level,p,ForgeFixtures.SOURCE);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"EmberKeeper"));
        var pos=source.getBlockPos();var state=level.getBlockState(pos);var hit=hit(pos);
        state.useWithoutItem(level,player,hit);
        ForgeLighting.advance(level,source,10);
        helper.assertTrue(!lit(level,pos),"An empty handwheel cannot create light");
        var coal=new ItemStack(Items.CHARCOAL,2);player.setItemInHand(InteractionHand.MAIN_HAND,coal);
        state.useItemOn(coal,level,player,InteractionHand.MAIN_HAND,hit);
        helper.assertTrue(coal.getCount()==1&&source.fuelTicks()==12000,"A coal item must buy ten minutes, not free permanent lighting");
        ForgeLighting.advance(level,source,10);helper.assertTrue(!lit(level,pos),"Fuel alone does not ignite");
        var fire=new ItemStack(Items.FLINT_AND_STEEL);player.setItemInHand(InteractionHand.MAIN_HAND,fire);
        state.useItemOn(fire,level,player,InteractionHand.MAIN_HAND,hit);
        helper.assertTrue(fire.getDamageValue()==1&&source.burning(),"Flint and steel must ignite the supplied brazier and lose durability");
        ForgeLighting.advance(level,source,10);
        helper.assertTrue(lit(level,pos)&&!lit(level,world(p,-24,4,64)),"Ignition must begin locally; distant lamps cannot jump ahead");
        int before=source.fuelTicks();source.toggleAirflow();ForgeLighting.advance(level,source,10);
        helper.assertTrue(source.fuelTicks()==before&&!lit(level,pos),"Closing the draught banks the fuel and extinguishes the network");
        source.toggleAirflow();for(int i=0;i<70;i++)ForgeLighting.advance(level,source,10);
        helper.assertTrue(lit(level,world(p,-24,4,64)),"An intact connected branch must eventually ignite");
        source.consume(source.fuelTicks());ForgeLighting.advance(level,source,10);
        helper.assertTrue(!lit(level,pos)&&!lit(level,world(p,-24,4,64))&&!source.ignited(),"Exhaustion must extinguish the complete network");
        helper.assertTrue(ModBlocks.FORGE_HEARTH.get().defaultBlockState().getLightEmission()==1,"The First Flame remains absent");
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=180)
    public static void damageRepairsAndMiningControlDownstreamFire(GameTestHelper helper) {
        var level=helper.getLevel();var p=plan(helper,250);placeAll(level,p);var source=service(level,p,ForgeFixtures.SOURCE);
        source.addFuel(12000);source.ignite();source.toggleAirflow();advance(level,source);
        var west=world(p,-44,5,72);var east=world(p,43,8,80);var shrine=world(p,7,5,107);
        helper.assertTrue(!lit(level,west)&&!lit(level,east)&&!lit(level,shrine),"Ash, cracks and missing runners must each block their branch");
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ChannelKeeper"));
        var ash=world(p,ForgeFixtures.ASH);level.getBlockState(ash).useWithoutItem(level,player,hit(ash));advance(level,source);
        helper.assertTrue(lit(level,west)&&!lit(level,east),"Clearing one physical blockage cannot wake unrelated branches");
        var crack=world(p,ForgeFixtures.CRACK);var powder=new ItemStack(Items.GUNPOWDER,2);
        level.getBlockState(crack).useItemOn(powder,level,player,InteractionHand.MAIN_HAND,hit(crack));advance(level,source);
        helper.assertTrue(powder.getCount()==1&&lit(level,east),"A short cracked runner consumes one ignition trail and conducts again");
        var missing=world(p,ForgeFixtures.MISSING);level.setBlock(missing,ModBlocks.FORGE_EMBER_CHANNEL.get().defaultBlockState(),3);
        ModBlocks.FORGE_EMBER_CHANNEL.get().setPlacedBy(level,missing,level.getBlockState(missing),player,new ItemStack(ModBlocks.FORGE_EMBER_CHANNEL.get()));
        advance(level,source);helper.assertTrue(lit(level,shrine),"A placed replacement must bind to its neighbours and restore the shrine path");
        helper.assertTrue(source.energized().size()==ForgeFixtures.ALL.size(),"All authored fixtures must be physically reachable once the three failures are repaired");
        level.setBlock(crack,Blocks.AIR.defaultBlockState(),3);ForgeLighting.advance(level,source,10);
        helper.assertTrue(!lit(level,east)&&lit(level,west),"Mining a runner must immediately cut its downstream branch");
        helper.assertTrue(level.getBlockState(crack).isAir(),"Recalculation may never reconstruct a mined channel");
        level.setBlock(source.getBlockPos(),Blocks.AIR.defaultBlockState(),3);
        service(level,p,new BlockPos(-44,5,72)).reconcile(level);
        helper.assertTrue(!lit(level,west),"A terminal must go dark when its source is removed");
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=120)
    public static void fuelAndRepairsPersistButLightRequiresReconnection(GameTestHelper helper) {
        var level=helper.getLevel();var p=plan(helper,500);placeAll(level,p);var source=service(level,p,ForgeFixtures.SOURCE);
        source.addFuel(24000);source.ignite();source.toggleAirflow();advance(level,source);
        var tag=source.saveWithFullMetadata(level.registryAccess());
        var loaded=(ForgeServiceBlockEntity)BlockEntity.loadStatic(source.getBlockPos(),source.getBlockState(),tag,level.registryAccess());
        helper.assertTrue(loaded!=null&&loaded.belongsTo(p)&&loaded.fuelTicks()==source.fuelTicks()&&loaded.burning(),"Brazier fuel, draught, ignition and binding must survive actual NBT");
        helper.assertTrue(loaded.energized().isEmpty(),"Reload must survey physical connections, not replay permanent light bits");
        helper.assertTrue(!source.addFuel(ForgeServiceBlockEntity.MAX_FUEL),"A full fuel charge cannot overflow the reservoir");
        var lamp=service(level,p,new BlockPos(-24,4,64));
        var lampTag=lamp.saveWithFullMetadata(level.registryAccess());
        helper.assertTrue(!lampTag.contains("Fuel"),"Branch fixtures cannot duplicate a fuel reservoir");
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=120)
    public static void unreachableChunksAndForeignForgesCannotSupplyFire(GameTestHelper helper) {
        var level=helper.getLevel();var p=new ForgePlan(512000,512000,180,0);
        for(var local:List.of(ForgeFixtures.SOURCE,new BlockPos(-5,0,52),new BlockPos(-5,0,53)))place(level,p,ForgeFixtures.NETWORK.get(local));
        var far=world(p,43,8,80);
        helper.assertTrue(level.getChunkSource().getChunkNow(far.getX()>>4,far.getZ()>>4)==null,"Far branch starts unloaded");
        var source=service(level,p,ForgeFixtures.SOURCE);source.addFuel(12000);source.ignite();source.toggleAirflow();advance(level,source);
        helper.assertTrue(level.getChunkSource().getChunkNow(far.getX()>>4,far.getZ()>>4)==null,"Propagation may not load distant chunks");
        place(level,p,ForgeFixtures.NETWORK.get(new BlockPos(43,8,80)));service(level,p,new BlockPos(43,8,80)).reconcile(level);
        helper.assertTrue(!lit(level,far),"A late-loaded terminal with no connecting blocks stays dark");
        var disconnected=helper.absolutePos(new BlockPos(1,2,1));level.setBlock(disconnected,ModBlocks.FORGE_SERVICE_SWITCH.get().defaultBlockState(),3);
        helper.assertTrue(!((ForgeServiceBlockEntity)level.getBlockEntity(disconnected)).addFuel(12000),"An unbound item cannot create another Forge reservoir");
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void arrivalGuidanceStillRequiresTheHearthsHeight(GameTestHelper helper) {
        var player=new FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"ArrivalReader"));
        var target=new BlockPos(32,100,32);player.setPos(32.5,100,32.5);
        helper.assertTrue(dev.elysium.item.ResonanceItem.arrived(player,target),"Standing at the remembered hearth must be recognised");
        player.setPos(32.5,172,32.5);
        helper.assertTrue(!dev.elysium.item.ResonanceItem.arrived(player,target),"Being at the deeper Forge's mountain entrance is not arrival at its hearth");
        helper.succeed();
    }
    private static ForgePlan plan(GameTestHelper h,int offset) {var p=h.absolutePos(new BlockPos(offset,105,0));return new ForgePlan(p.getX(),p.getZ(),p.getY(),0);}
    private static BlockPos world(ForgePlan p,int u,int h,int v) {return new BlockPos(p.worldX(u,v),p.floor()+h,p.worldZ(u,v));}
    private static BlockPos world(ForgePlan p,BlockPos local) {return world(p,local.getX(),local.getY(),local.getZ());}
    private static ForgeServiceBlockEntity service(ServerLevel l,ForgePlan p,BlockPos local) {return (ForgeServiceBlockEntity)Objects.requireNonNull(l.getBlockEntity(world(p,local)));}
    private static BlockHitResult hit(BlockPos p) {return new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false);}
    private static boolean lit(ServerLevel l,BlockPos p) {var state=l.getBlockState(p);return state.getBlock() instanceof ForgeServiceBlock&&state.getValue(ForgeServiceBlock.LIT);}
    private static void advance(ServerLevel l,ForgeServiceBlockEntity source) {for(int i=0;i<100;i++)ForgeLighting.advance(l,source,10);}
    private static void placeAll(ServerLevel l,ForgePlan p) {for(var f:ForgeFixtures.ALL)if(!f.local().equals(ForgeFixtures.MISSING))place(l,p,f);}
    private static void place(ServerLevel l,ForgePlan p,ForgeFixtures.Fixture f) {var pos=f.worldPosition(p);l.setBlock(pos,ForgeFixtures.state(p,f),3);((ForgeServiceBlockEntity)Objects.requireNonNull(l.getBlockEntity(pos))).bind(p);}
    private ForgeLightingGameTests() {}
}
