package dev.elysium.gametest;

import com.mojang.authlib.GameProfile;
import dev.elysium.block.ForgeServiceBlock;
import dev.elysium.block.ForgeServiceBlockEntity;
import dev.elysium.forge.ForgeLighting;
import dev.elysium.item.ResonanceItem;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModBlocks;
import dev.elysium.structure.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
    public static void ruinStartsDarkWithConcealedAccessAndCompleteFixtures(GameTestHelper helper) {
        for(int turn=0;turn<4;turn++) {
            var p=new ForgePlan(240,-192,140,turn);var piece=new HephaestusForgePiece(p);
            for(var f:ForgeFixtures.ALL) {
                var state=piece.materialAt(f.u(),p.floor()+f.h(),f.v());
                helper.assertTrue(state!=null&&state.is((f.controller()?ModBlocks.FORGE_SERVICE_SWITCH:ModBlocks.FORGE_SERVICE_LAMP).get()),
                        "Every authored fixture must survive walls, ceilings and rubble: "+f);
                helper.assertTrue(!state.getValue(ForgeServiceBlock.LIT),"The service network must start dormant");
            }
            int air=0,stone=0;
            for(int u=-4;u<=4;u++)for(int e=1;e<=5;e++) {
                var state=piece.materialAt(u,p.entranceFloor()+e,1);
                if(state!=null&&state.isAir())air++;else stone++;
            }
            helper.assertTrue(air<=8&&stone>=35,"Most of the old doorway must be visibly buried, leaving a narrow survivor's route");
            helper.assertTrue(piece.materialAt(0,p.entranceFloor()+1,1)!=null&&!piece.materialAt(0,p.entranceFloor()+1,1).isAir(),
                    "The original doorway centre must be blocked by scree");
            helper.assertTrue(piece.materialAt(-25,p.floor()+7,85)!=null&&!piece.materialAt(-25,p.floor()+7,85).isAir(),
                    "Rockfall must bury the failed gallery beneath its breach");
            helper.assertTrue(piece.materialAt(0,p.floor()+5,76).is(Blocks.IRON_BLOCK),"Hammer and anvil must be vertically aligned");
            helper.assertTrue(piece.materialAt(11,p.floor()+9,69).is(ModBlocks.TARTARUS_CHAIN_BLOCK.get()),"An unfinished link must remain legible beside the hammer");
            helper.assertTrue(piece.materialAt(0,p.entranceFloor()+10,76)==null,"The broken vault must not excavate open sky through the mountain");
        }
        helper.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=180)
    public static void serviceCircuitsAreSharedPersistentAndDoNotWakeTheHearth(GameTestHelper helper) {
        var realm=Objects.requireNonNull(helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION));
        var plan=new ForgePlan(220000,220000,140,0);
        var controller=ForgeFixtures.ALL.stream().filter(f->f.controller()&&f.circuit()==0).findFirst().orElseThrow();
        var lamp=ForgeFixtures.ALL.stream().filter(f->!f.controller()&&f.circuit()==0&&f.u()==-24).findFirst().orElseThrow();
        var other=ForgeFixtures.ALL.stream().filter(f->!f.controller()&&f.circuit()==1).findFirst().orElseThrow();
        for(var fixture:List.of(controller,lamp,other))place(realm,plan,fixture);
        var player=new FakePlayer(realm,new GameProfile(UUID.randomUUID(),"LightKeeper"));
        var pos=controller.worldPosition(plan);
        realm.getBlockState(pos).useWithoutItem(realm,player,new BlockHitResult(Vec3.atCenterOf(pos),Direction.EAST,pos,false));
        var data=ForgeLighting.get(realm);
        var saved=data.save(new CompoundTag(),realm.registryAccess());
        var loaded=ForgeLighting.load(saved,realm.registryAccess());
        helper.assertTrue(loaded.active(plan,0)&&!loaded.active(plan,1)&&!loaded.active(plan,2),"Only the operated circuit may persist as restored");
        helper.assertTrue(!loaded.active(new ForgePlan(220016,220000,140,0),0),"Another Forge must retain its own darkness");
        helper.assertTrue(!data.restore(plan,0),"A second player cannot restart an already restored circuit");
        var service=(ForgeServiceBlockEntity)realm.getBlockEntity(lamp.worldPosition(plan));
        var tag=service.saveWithFullMetadata(realm.registryAccess());
        var reloaded=(ForgeServiceBlockEntity)BlockEntity.loadStatic(lamp.worldPosition(plan),service.getBlockState(),tag,realm.registryAccess());
        helper.assertTrue(reloaded!=null&&reloaded.belongsTo(plan,0),"A fixture must retain its own Forge and circuit through Minecraft NBT");
        helper.runAfterDelay(60,()->{
            helper.assertTrue(realm.getBlockState(lamp.worldPosition(plan)).getValue(ForgeServiceBlock.LIT),"Loaded lamps must wake after the handwheel turns");
            helper.assertTrue(!realm.getBlockState(other.worldPosition(plan)).getValue(ForgeServiceBlock.LIT),"The next workshop circuit must stay dark");
            helper.assertTrue(realm.getBlockState(pos).getValue(ForgeServiceBlock.LIT),"The restored handwheel must show its state");
            helper.assertTrue(ModBlocks.FORGE_HEARTH.get().defaultBlockState().getLightEmission()==1,"Service power must not ignite the hearth");
            realm.setBlock(lamp.worldPosition(plan),Blocks.AIR.defaultBlockState(),3);
            ForgeLighting.restore(realm,plan,0);
            helper.assertTrue(realm.getBlockState(lamp.worldPosition(plan)).isAir(),"Restoring lights must not reconstruct a mined fixture");
            helper.succeed();
        });
    }

    @GameTest(template="portal_test_empty",timeoutTicks=120)
    public static void lateLoadedFixturesReconcileWithoutLoadingTheirNeighbours(GameTestHelper helper) {
        var realm=Objects.requireNonNull(helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION));
        var plan=new ForgePlan(224000,224000,140,1);
        var fixture=ForgeFixtures.ALL.stream().filter(f->!f.controller()&&f.circuit()==2).findFirst().orElseThrow();
        var pos=fixture.worldPosition(plan);
        helper.assertTrue(realm.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null,"The late fixture starts outside loaded chunks");
        ForgeLighting.restore(realm,plan,2);
        helper.assertTrue(realm.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null,"Operating a circuit must not force-load a distant fixture");
        place(realm,plan,fixture);
        var service=(ForgeServiceBlockEntity)realm.getBlockEntity(pos);
        service.onLoad();
        helper.runAfterDelay(5,()->{
            helper.assertTrue(realm.getBlockState(pos).getValue(ForgeServiceBlock.LIT),"A lamp loaded after restoration must recover the shared state");
            helper.succeed();
        });
    }

    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void arrivalGuidanceUsesHeightAndUnboundHandwheelsStayDisconnected(GameTestHelper helper) {
        var realm=Objects.requireNonNull(helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION));
        var player=new FakePlayer(realm,new GameProfile(UUID.randomUUID(),"ArrivalReader"));
        var target=new BlockPos(32,100,32);player.setPos(32.5,100,32.5);
        helper.assertTrue(ResonanceItem.arrived(player,target),"Standing at the remembered hearth must be recognised");
        player.setPos(32.5,125,32.5);
        helper.assertTrue(!ResonanceItem.arrived(player,target),"Being on the mountain above the hearth must not count as arrival");
        var pos=helper.absolutePos(new BlockPos(1,2,1));
        helper.getLevel().setBlock(pos,ModBlocks.FORGE_SERVICE_SWITCH.get().defaultBlockState(),3);
        var service=(ForgeServiceBlockEntity)helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(service!=null&&service.plan()==null,"An ordinary placed handwheel must not inherit any Forge binding");
        helper.succeed();
    }

    private static void place(ServerLevel level,ForgePlan plan,ForgeFixtures.Fixture fixture) {
        var pos=fixture.worldPosition(plan);
        var state=new HephaestusForgePiece(plan).materialAt(fixture.u(),plan.floor()+fixture.h(),fixture.v());
        level.setBlock(pos,state,3);
        ((ForgeServiceBlockEntity)Objects.requireNonNull(level.getBlockEntity(pos))).bind(plan,fixture.circuit());
    }
    private ForgeLightingGameTests() {}
}
