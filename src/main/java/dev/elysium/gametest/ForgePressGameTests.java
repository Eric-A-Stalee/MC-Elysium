package dev.elysium.gametest;

import com.mojang.authlib.GameProfile;
import dev.elysium.block.*;
import dev.elysium.forge.*;
import dev.elysium.registry.*;
import dev.elysium.structure.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("elysium") @PrefixGameTestTemplate(false)
public final class ForgePressGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void pressEscrowsFullBillsPausesAndCollectsOnlyOneOutput(GameTestHelper h) {
        var level=h.getLevel();var plan=plan(h,0);var press=prepare(level,plan);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"Smith"));
        player.getInventory().add(new ItemStack(Items.IRON_INGOT,3));
        h.assertTrue(!press.queue(player,ForgeRecipes.byId("sunwrought"))&&count(player,Items.IRON_INGOT)==3,"Partial bills must not consume materials");
        player.getInventory().add(new ItemStack(Items.COPPER_INGOT,2));player.getInventory().add(new ItemStack(Items.GOLD_INGOT));
        h.assertTrue(!press.queue(player,ForgeRecipes.byId("sunwrought"))&&count(player,Items.IRON_INGOT)==3,"A complete bill cannot substitute for Prometheus's teaching");
        ForgeDiscoveries.get(level).discover(player.getUUID(),ForgeDiscoveries.PROMETHEUS);
        h.assertTrue(press.queue(player,ForgeRecipes.byId("sunwrought"))&&count(player,Items.IRON_INGOT)==0,"Complete bill goes into escrow");
        h.assertTrue(!press.queue(player,ForgeRecipes.byId("sunwrought")),"Simultaneous users cannot replace queued work");
        h.assertTrue(press.start(),"A supplied press starts");
        for(int i=0;i<6;i++)press.advance();
        h.assertTrue(press.drop()==2,"The actual hammer must descend through its stroke");
        var source=ForgeLighting.loadedService(level,ForgeFixtures.source(plan));source.toggleAirflow();
        press.advance();h.assertTrue(press.drop()==2&&press.output().isEmpty(),"Power loss pauses without completing work");
        source.toggleAirflow();
        var obstacle=ForgeHammer.world(plan,0,6,76);level.setBlock(obstacle,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
        press.advance();h.assertTrue(press.drop()==2&&level.getBlockState(obstacle).is(Blocks.DIAMOND_BLOCK),"A new obstruction stops the entire movement before writes");
        level.removeBlock(obstacle,false);
        var saved=press.saveWithFullMetadata(level.registryAccess());
        var resumed=(ForgePressBlockEntity)BlockEntity.loadStatic(press.getBlockPos(),press.getBlockState(),saved,level.registryAccess());
        resumed.setLevel(level);level.setBlockEntity(resumed);
        h.assertTrue(resumed.drop()==2&&resumed.escrow().stream().mapToInt(ItemStack::getCount).sum()==6,"Physical offset and escrow survive real serialization");
        for(int i=0;i<40;i++)resumed.advance();
        h.assertTrue(resumed.drop()==0&&!resumed.running()&&resumed.output().is(ModItems.SUNWROUGHT_INGOT.get())&&resumed.output().getCount()==4,"Three strokes return the hammer and produce one batch");
        resumed.use(player,ItemStack.EMPTY);resumed.use(player,ItemStack.EMPTY);
        h.assertTrue(count(player,ModItems.SUNWROUGHT_INGOT.get())==4&&resumed.output().isEmpty(),"Collecting twice cannot duplicate output");
        h.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void hammerFitsAllRotationsAndRefusesToRebuildDamage(GameTestHelper h) {
        var level=h.getLevel();
        for(int turn=0;turn<4;turn++) {
            var base=h.absolutePos(new BlockPos(500+turn*100,120,0));var p=new ForgePlan(base.getX(),base.getZ(),base.getY(),turn);
            placeHead(level,p);
            var removed=ForgeHammer.world(p,0,10,76);level.removeBlock(removed,false);
            h.assertTrue(!ForgeHammer.move(level,p,0,1)&&level.getBlockState(removed).isAir(),"Damage cannot be silently regenerated");
            level.setBlock(removed,ForgeHammer.material(0,10,76,0),3);
            h.assertTrue(ForgeHammer.move(level,p,0,4)&&ForgeHammer.move(level,p,4,0),"The authored head and telescoping ram must work in every orientation");
        }
        var far=new ForgePlan(900000,900000,150,0);
        h.assertTrue(!ForgeHammer.move(level,far,0,4)&&level.getChunkSource().getChunkNow(far.x()>>4,far.z()>>4)==null,"A stroke cannot load missing chunks");
        h.succeed();
    }
    private static int count(FakePlayer p,Item item){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static ForgePlan plan(GameTestHelper h,int dx){var p=h.absolutePos(new BlockPos(dx,120,0));return new ForgePlan(p.getX(),p.getZ(),p.getY(),0);}
    private static void placeHead(ServerLevel l,ForgePlan p){for(int u=-4;u<=4;u++)for(int v=73;v<=79;v++)for(int y=6;y<=22;y++){var s=ForgeHammer.material(u,y,v,0);l.setBlock(ForgeHammer.world(p,u,y,v),s==null?Blocks.AIR.defaultBlockState():s,3);}}
    static ForgePressBlockEntity prepare(ServerLevel l,ForgePlan p) {
        for(var f:ForgeFixtures.ALL){var pos=f.worldPosition(p);l.setBlock(pos,ForgeFixtures.state(p,f).setValue(ForgeServiceBlock.DECAY,0),3);((ForgeServiceBlockEntity)l.getBlockEntity(pos)).bind(p);}
        l.setBlock(ForgeFixtures.hearth(p),ModBlocks.FORGE_HEARTH.get().defaultBlockState().setValue(DormantHearthBlock.FLAME,true),3);
        var source=ForgeLighting.loadedService(l,ForgeFixtures.source(p));source.addFuel(48000);source.ignite();source.toggleAirflow();for(int i=0;i<160;i++)ForgeLighting.advance(l,source,10);
        var control=ForgeHammer.world(p,-9,1,76);l.setBlock(control,ModBlocks.FORGE_PRESS.get().defaultBlockState(),3);var press=(ForgePressBlockEntity)l.getBlockEntity(control);press.bind(p);placeHead(l,p);return press;
    }
    private ForgePressGameTests(){}
}
