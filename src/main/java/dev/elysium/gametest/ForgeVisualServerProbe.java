package dev.elysium.gametest;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import dev.elysium.Elysium;
import dev.elysium.forge.ForgeLighting;
import dev.elysium.block.ForgeServiceBlock;
import dev.elysium.block.ForgeServiceBlockEntity;
import dev.elysium.structure.ForgeFixtures;
import dev.elysium.registry.ModBlocks;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.structure.ForgePlan;
import dev.elysium.structure.HephaestusForgePiece;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in visual evidence harness. This entire package is excluded from the player jar. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class ForgeVisualServerProbe {
    private static final boolean ENABLED=Boolean.getBoolean("elysium.visualServer");
    private static ForgePlan PLAN;
    private static dev.elysium.structure.WatchfirePiece[] WATCHES;
    private static final java.util.Map<dev.elysium.worldgen.TartarusTerrain.Region,BlockPos> REGIONS=new java.util.EnumMap<>(dev.elysium.worldgen.TartarusTerrain.Region.class);

    private static dev.elysium.structure.TartarusDepthsPlan DEPTHS;
    private static dev.elysium.structure.PrometheusPrisonPiece PRISON;
    private static dev.elysium.structure.DeepBindingPiece[] BINDINGS;
    private static dev.elysium.story.RupturePlan RUPTURE;
    private static boolean depthsReady;
    private static void prepareDepths(ServerLevel level) {
        if(depthsReady)return;
        DEPTHS=TartarusNaturalGenerationProbe.generateAndVerify(level);
        depthsReady=true;
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!ENABLED)return;
        var level=event.getServer().getLevel(ElysiumTravel.DIMENSION);
        if(level==null)throw new IllegalStateException("Missing Elysium dimension");
        if("bindings".equals(System.getenv("ELYSIUM_VISUAL_FOCUS"))){
            BINDINGS=BindingNaturalGenerationProbe.generateAndVerify(level);RUPTURE=findRupture(level);
            var cycle=dev.elysium.story.RealmCycle.get(level);cycle.awaken(BINDINGS[0].core());for(int i=0;i<6500;i++)cycle.advance(true);
            LogUtils.getLogger().info("ELYSIUM_FORGE_VISUAL_READY");return;
        }
        if("prison".equals(System.getenv("ELYSIUM_VISUAL_FOCUS"))){PRISON=PrometheusNaturalGenerationProbe.generateAndVerify(level);LogUtils.getLogger().info("ELYSIUM_FORGE_VISUAL_READY");return;}
        prepareDepths(level);
        if("underworld".equals(System.getenv("ELYSIUM_VISUAL_FOCUS"))) {
            prepareUnderworld(level);
            LogUtils.getLogger().info("ELYSIUM_FORGE_VISUAL_READY");return;
        }
        PLAN=ForgeNaturalGenerationProbe.generateAndVerify(level);
        WATCHES=WatchfireNaturalGenerationProbe.generateAndVerify(level,PLAN.entrance());
        LogUtils.getLogger().info("ELYSIUM_FORGE_VISUAL_READY");
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        if(!ENABLED)return;
        event.getDispatcher().register(Commands.literal("forge_probe")
                .then(Commands.argument("scene",StringArgumentType.word()).executes(context->{
                    scene(context.getSource().getPlayerOrException(),StringArgumentType.getString(context,"scene"));return 1;
                })));
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(ENABLED&&event.getEntity() instanceof ServerPlayer player)scene(player,BINDINGS!=null?"binding_hunger":PRISON!=null?"prison_overview":"inspection_entry");
    }

    private static void scene(ServerPlayer player,String scene) {
        var level=player.getServer().getLevel(ElysiumTravel.DIMENSION);
        if(level==null)return;
        player.setGameMode(GameType.SPECTATOR);
        if(scene.startsWith("binding_")||scene.startsWith("rupture_")){bindingCamera(player,level,scene);return;}
        if(scene.startsWith("prison_")){prisonCamera(player,level,scene);return;}
        if(scene.startsWith("underworld_")){underworldCamera(player,level,scene);return;}
        switch(scene) {
            case "entrance" -> camera(player,level,-7,PLAN.entranceFloor()+5,-18,-2,PLAN.entranceFloor()+3,1);
            case "shaft" -> camera(player,level,-10,PLAN.floor()+48,22,4,PLAN.floor()+26,20);
            case "overlook" -> camera(player,level,0,PLAN.floor()+13,49,0,PLAN.floor()+6,76);
            case "dark" -> camera(player,level,-11,PLAN.floor()+2.5,57,0,PLAN.floor()+11,76);
            case "ignition" -> {
                ignite(level);
                camera(player,level,-10,PLAN.floor()+2,50,-4,PLAN.floor()+.5,56);
            }
            case "foundry" -> {
                ignite(level);
                camera(player,level,-11,PLAN.floor()+2.5,57,0,PLAN.floor()+11,76);
            }
            case "workshop" -> {
                repair(level,ForgeFixtures.ASH);
                camera(player,level,-36,PLAN.floor()+2,59,-41,PLAN.floor()+3,72);
            }
            case "retorts" -> {repair(level,ForgeFixtures.CRACK);camera(player,level,34,PLAN.floor()+2,69,43,PLAN.floor()+4,79);}
            case "lit" -> {
                repair(level,ForgeFixtures.MISSING);
                camera(player,level,-11,PLAN.floor()+2.5,57,0,PLAN.floor()+11,76);
            }
            case "gallery" -> camera(player,level,22,PLAN.floor()+9,66,0,PLAN.floor()+8,79);
            case "collapse" -> camera(player,level,-7,PLAN.floor()+2,92,-22,PLAN.floor()+9,82);
            case "inspection_entry" -> {
                level.setBlock(DEPTHS.at(9,-47,127),ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);
                depthsCamera(player,level,10,-47,124,10,-48,138);
            }
            case "lower_descent" -> {
                level.setBlock(DEPTHS.at(-51,DEPTHS.path(-50,132).floor()+1,132),ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);
                depthsCamera(player,level,-50,-170,119,-46,-184,146);
            }
            case "descent" -> {
                level.setBlock(DEPTHS.at(-28,-183,164),ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);
                depthsCamera(player,level,-31,-182,161,-26,-184,180);
            }
            case "crossing" -> {
                level.setBlock(DEPTHS.at(-3,-182,187),ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);
                depthsCamera(player,level,-6,-182,190,19,-193,196);
            }
            case "chimera_dark" -> {
                var heart=(dev.elysium.block.DepthsControlBlockEntity)level.getBlockEntity(DEPTHS.heart());
                heart.use(player,new net.minecraft.world.item.ItemStack(dev.elysium.registry.ModItems.CHAIN_RESONATOR.get()));
                depthsCamera(player,level,116,-230,246,128,-226,220);
            }
            case "chimera_lamp" -> {
                level.setBlock(DEPTHS.at(122,-231,231),ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);
                level.setBlock(DEPTHS.at(132,-231,229),ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);
                depthsCamera(player,level,116,-230,246,128,-226,220);
            }
            case "chimera_eye" -> depthsCamera(player,level,121,-223,234,128,-221,220);
            case "chimera_attack" -> {
                depthsCamera(player,level,116,-230,246,128,-225,220);
                player.setGameMode(GameType.SURVIVAL);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,2400,4,false,false));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SATURATION,2400,4,false,false));
                player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.onUpdateAbilities();
            }
            case "forge_service" -> {
                ignite(level);repair(level,ForgeFixtures.ASH);repair(level,ForgeFixtures.CRACK);repair(level,ForgeFixtures.MISSING);
                camera(player,level,-10,PLAN.floor()+2,50,-2,PLAN.floor()+1,61);
            }
            case "forge_restored" -> {
                var hearth=ForgeFixtures.hearth(PLAN);
                boolean accepted=ModBlocks.FORGE_HEARTH.get().receiveFlame(player,hearth,
                        new net.minecraft.world.item.ItemStack(dev.elysium.registry.ModItems.FIRST_FLAME.get()));
                if(!accepted)throw new IllegalStateException("Natural Forge refused its First Flame");
                camera(player,level,-10,PLAN.floor()+2,50,-2,PLAN.floor()+1,61);
            }
            case "forge_full_hall" -> camera(player,level,-11,PLAN.floor()+2.5,57,0,PLAN.floor()+11,76);
            case "forge_banked" -> {
                var source=(ForgeServiceBlockEntity)level.getBlockEntity(ForgeFixtures.source(PLAN));
                if(source.airflow())source.toggleAirflow();
                camera(player,level,-10,PLAN.floor()+2,50,-2,PLAN.floor()+1,61);
            }
            case "hammer_strike" -> {
                ignite(level);
                var source=(ForgeServiceBlockEntity)level.getBlockEntity(ForgeFixtures.source(PLAN));
                for(int i=0;i<100;i++)ForgeLighting.advance(level,source,0);
                var press=(dev.elysium.block.ForgePressBlockEntity)level.getBlockEntity(dev.elysium.forge.ForgeHammer.world(PLAN,-9,1,76));
                player.setGameMode(GameType.CREATIVE);
                if(!press.queue(player,dev.elysium.forge.ForgeRecipes.byId("sunwrought"))||!press.start())throw new IllegalStateException("Natural press refused its first bill");
                for(int i=0;i<7;i++)press.advance();press.pause();
                if(press.drop()!=4||dev.elysium.story.RealmCycle.get(level).phase()!=dev.elysium.story.RealmCycle.Phase.PARADISE)throw new IllegalStateException("An unfinished first strike must leave the vessel sealed");
                player.setGameMode(GameType.SPECTATOR);
                camera(player,level,-12,PLAN.floor()+5,65,0,PLAN.floor()+9,76);
            }
            case "harvest_sunset" -> {
                var press=(dev.elysium.block.ForgePressBlockEntity)level.getBlockEntity(dev.elysium.forge.ForgeHammer.world(PLAN,-9,1,76));press.start();
                for(int i=0;i<40&&press.output().isEmpty();i++)press.advance();
                if(!dev.elysium.story.RealmCycle.get(level).released())throw new IllegalStateException("Completed work failed to open the vessel");
                watchCamera(player,level,0);
            }
            case "harvest_night" -> {
                var cycle=dev.elysium.story.RealmCycle.get(level);
                // Only the disposable review clock is accelerated, through the ordinary state transition.
                while(cycle.time()<18000)cycle.advance(true);
                dev.elysium.story.RealmClock.sync(player);watchCamera(player,level,0);
            }
            case "harvest_answered" -> {answer(player,level,0);watchCamera(player,level,0);}
            case "stray_cinder" -> {
                if(!dev.elysium.story.RealmCycle.get(level).released()) {
                    // A focused client review still earns the phase through the actual Forge machinery.
                    scene(player,"forge_service");scene(player,"forge_restored");scene(player,"hammer_strike");scene(player,"harvest_sunset");
                    var cycle=dev.elysium.story.RealmCycle.get(level);while(cycle.time()<18000)cycle.advance(true);
                    dev.elysium.story.RealmClock.sync(player);
                }
                var pos=WATCHES[0].socket();
                var wisp=dev.elysium.registry.ModEntities.CINDER_WISP.get().create(level);
                wisp.setPos(pos.getX()+3.5,pos.getY()+5,pos.getZ()+10.5);level.addFreshEntity(wisp);
                player.teleportTo(level,pos.getX()+3.5,pos.getY()+4.5,pos.getZ()+16.5,180,0);
                player.setGameMode(GameType.SURVIVAL);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,2400,4,false,false));
                player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.onUpdateAbilities();
            }
            case "water_answered" -> {answer(player,level,1);watchCamera(player,level,1);}
            case "high_answered" -> {answer(player,level,2);watchCamera(player,level,2);}
            case "governor" -> camera(player,level,6,PLAN.floor()+3,98,0,PLAN.floor()+2,104);
            case "first_dawn" -> {
                // Review fixture: model the completed deep repairs; encounter tests cover earning them.
                for(int kind=0;kind<3;kind++)dev.elysium.story.RealmCycle.get(level).bindDeep(kind);
                player.setGameMode(GameType.CREATIVE);
                if(!ModBlocks.CELESTIAL_GOVERNOR.get().choose(player,dev.elysium.block.CelestialGovernorBlock.position(PLAN),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CLOCK),true))throw new IllegalStateException("Governor did not accept the repaired living sky");
                var cycle=dev.elysium.story.RealmCycle.get(level);while(cycle.time()<24500)cycle.advance(true);
                dev.elysium.story.RealmClock.sync(player);player.setGameMode(GameType.SPECTATOR);watchCamera(player,level,0);
            }
            case "chosen_hour" -> {
                player.setGameMode(GameType.CREATIVE);
                if(!ModBlocks.CELESTIAL_GOVERNOR.get().choose(player,dev.elysium.block.CelestialGovernorBlock.position(PLAN),new net.minecraft.world.item.ItemStack(dev.elysium.registry.ModItems.ELYSIUM_FRAGMENT.get()),true))throw new IllegalStateException("Governor could not restore chosen golden hour");
                player.setGameMode(GameType.SPECTATOR);watchCamera(player,level,0);
            }
            default -> throw new IllegalArgumentException("Unknown review scene: "+scene);
        }
        LogUtils.getLogger().info("ELYSIUM_FORGE_SCENE {}",scene);
    }

    private static void answer(ServerPlayer player,ServerLevel level,int kind) {
        player.setGameMode(GameType.CREATIVE);
        var fire=(dev.elysium.block.WatchfireBlockEntity)level.getBlockEntity(WATCHES[kind].socket());
        if(!fire.repair(player,new net.minecraft.world.item.ItemStack(dev.elysium.registry.ModItems.WATCHFIRE_SEAL.get())))throw new IllegalStateException("Natural watch refused its repair: "+kind);
        player.setGameMode(GameType.SPECTATOR);
    }
    private static void watchCamera(ServerPlayer player,ServerLevel level,int kind) {
        var pos=WATCHES[kind].socket();
        double x=pos.getX()+10.5,y=pos.getY()+5,z=pos.getZ()+14.5;
        if(kind==1) {
            // Find a real view through the watermeadow canopy; never clear vegetation for a screenshot.
            var target=pos.getCenter().add(0,1,0);
            search:for(double height:new double[]{2.5,5,9})for(int[] offset:new int[][]{{4,7},{-4,7},{4,-7},{-4,-7},{8,0},{-8,0},{0,8},{0,-8}}) {
                double cx=pos.getX()+offset[0]+.5,cy=pos.getY()+height,cz=pos.getZ()+offset[1]+.5;
                var eye=new net.minecraft.world.phys.Vec3(cx,cy+1.62,cz);
                if(!level.getBlockState(BlockPos.containing(eye)).isAir())continue;
                var ray=level.clip(new net.minecraft.world.level.ClipContext(eye,target,net.minecraft.world.level.ClipContext.Block.VISUAL,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
                if(ray.getType()==net.minecraft.world.phys.HitResult.Type.MISS){x=cx;y=cy;z=cz;break search;}
            }
        }
        double dx=pos.getX()+.5-x,dz=pos.getZ()+.5-z,dy=pos.getY()+2-y-1.62;
        player.teleportTo(level,x,y,z,(float)(Math.toDegrees(Math.atan2(dz,dx))-90),(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
    }

    private static void ignite(ServerLevel level) {
        var source=(ForgeServiceBlockEntity)level.getBlockEntity(ForgeFixtures.source(PLAN));
        if(source.fuelTicks()==0)source.addFuel(24000);source.ignite();if(!source.airflow())source.toggleAirflow();
    }
    private static void repair(ServerLevel level,BlockPos local) {
        var f=ForgeFixtures.NETWORK.get(local);var pos=f.worldPosition(PLAN);
        level.setBlock(pos,ForgeFixtures.state(PLAN,f).setValue(ForgeServiceBlock.DECAY,0),3);
        ((ForgeServiceBlockEntity)level.getBlockEntity(pos)).bind(PLAN);
    }

    private static void depthsCamera(ServerPlayer player,ServerLevel level,int u,double y,int v,int tu,double ty,int tv) {
        double x=DEPTHS.x+u+.5,z=DEPTHS.z+v+.5,dx=tu-u,dz=tv-v,dy=ty-y-1.62;
        player.teleportTo(level,x,y,z,(float)(Math.toDegrees(Math.atan2(dz,dx))-90),(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
    }
    private static void prepareUnderworld(ServerLevel level) {
        var generator=(dev.elysium.worldgen.StratifiedChunkGenerator)level.getChunkSource().getGenerator();
        var terrain=generator.terrain(level.getChunkSource().randomState());
        for(int radius=0;radius<=48&&REGIONS.size()<4;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            int x=DEPTHS.x+dx*128,z=DEPTHS.z+dz*128;var column=terrain.column(x,z);var kind=column.region;
            if(REGIONS.containsKey(kind))continue;
            var pos=new BlockPos(x,column.floor,z);
            if(kind==dev.elysium.worldgen.TartarusTerrain.Region.CHAIN_FOREST||kind==dev.elysium.worldgen.TartarusTerrain.Region.TITAN_GRAVE){
                var landmark=terrain.landmark(kind,x,z);if(landmark.isEmpty())continue;pos=landmark.get();
            }else if(kind==dev.elysium.worldgen.TartarusTerrain.Region.HOLLOW_MIRE){
                if(column.floor>dev.elysium.worldgen.TartarusTerrain.WATERLINE-6)continue;pos=pos.atY(dev.elysium.worldgen.TartarusTerrain.WATERLINE);
            }else{
                if(column.floor<=dev.elysium.worldgen.TartarusTerrain.WATERLINE||!column.at(column.floor+1).isAir())continue;
                if(terrain.region(x+64,z)!=kind||terrain.region(x-64,z)!=kind||terrain.region(x,z+64)!=kind||terrain.region(x,z-64)!=kind)continue;
            }
            level.getChunk(pos);REGIONS.put(kind,pos);LogUtils.getLogger().info("ELYSIUM_UNDERWORLD_REGION {} landmark={} roof={}",kind,pos,terrain.roof(pos.getX(),pos.getZ()));
        }
        if(REGIONS.size()!=4)throw new IllegalStateException("Bounded underworld survey found only "+REGIONS.keySet());
    }
    private static dev.elysium.story.RupturePlan findRupture(ServerLevel l){
        var g=l.getChunkSource().getGenerator();
        for(int r=0;r<18;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;int x=dx*96,z=dz*96;
            int y=g.getBaseHeight(x,z,net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,l,l.getChunkSource().randomState());if(y<66||y>110)continue;
            for(int cx=(x-12)>>4;cx<=(x+12)>>4;cx++)for(int cz=(z-12)>>4;cz<=(z+12)>>4;cz++)l.getChunk(cx,cz);
            var plan=dev.elysium.story.RupturePlan.survey(l,x,z);if(plan.isPresent()){
                for(int cx=(x-40)>>4;cx<=(x+40)>>4;cx++)for(int cz=(z-40)>>4;cz<=(z+40)>>4;cz++){l.getChunk(cx,cz);l.setChunkForced(cx,cz,true);}
                LogUtils.getLogger().info("ELYSIUM_NATURAL_RUPTURE_SURVEY core={}",plan.get().core());return plan.get();
            }
        }
        throw new IllegalStateException("No safe natural rupture site in bounded review survey");
    }
    private static void bindingCamera(ServerPlayer player,ServerLevel level,String scene){
        net.minecraft.world.phys.Vec3 camera,target;
        if(scene.startsWith("rupture_")){
            player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            var centre=RUPTURE.core().above(6);camera=net.minecraft.world.phys.Vec3.atCenterOf(centre).add(12,6,16);target=net.minecraft.world.phys.Vec3.atCenterOf(centre).add(0,-2,0);
            player.teleportTo(level,camera.x,camera.y,camera.z,0,0);
            if(scene.equals("rupture_open")){
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING).set(true,level.getServer());level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(true,level.getServer());
                if(!dev.elysium.story.RuptureDirector.open(level,RUPTURE,2))throw new IllegalStateException("Reviewed rupture refused its survey");
                var core=(dev.elysium.block.RiftBlockEntity)level.getBlockEntity(RUPTURE.core());for(int i=0;i<80;i++)core.advance();if(!core.opened())throw new IllegalStateException("Rupture warning did not lead to a real wound");
                var spawned=level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(centre).inflate(32));
                if(spawned.stream().noneMatch(e->e instanceof dev.elysium.entity.ChainWardenEntity)||spawned.stream().filter(e->e instanceof dev.elysium.entity.AshenHoundEntity).count()<2)throw new IllegalStateException("Natural rupture did not release its actual creatures");
                // Keep real spawned positions for evidence; the warning/strike behavior has separate damage tests.
                for(var e:spawned)e.setNoAi(true);
            }else if(scene.equals("rupture_sealed"))((dev.elysium.block.RiftBlockEntity)level.getBlockEntity(RUPTURE.core())).use(player,new net.minecraft.world.item.ItemStack(dev.elysium.registry.ModItems.BINDING_NAIL.get()));
        }else{
            int kind=scene.equals("binding_hunger")?0:scene.equals("binding_sorrow")||scene.equals("binding_dark")?1:2;var core=BINDINGS[kind].core();
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,12000,0,false,false));
            camera=net.minecraft.world.phys.Vec3.atCenterOf(core).add(kind==1?-26:26,18,34);target=net.minecraft.world.phys.Vec3.atCenterOf(core).add(0,8,-9);
            if(kind==2){var control=(dev.elysium.block.BindingBlockEntity)level.getBlockEntity(core);if(!control.summon()&&control.warden()==null)throw new IllegalStateException("Natural binding cannot house its jailer");var e=level.getEntity(control.warden());if(e instanceof net.minecraft.world.entity.Mob mob)mob.setNoAi(true);}
            if(scene.equals("binding_warden")){camera=net.minecraft.world.phys.Vec3.atCenterOf(core).add(16,2,10);target=net.minecraft.world.phys.Vec3.atCenterOf(core).add(8,1,-3);}
            if(scene.equals("binding_dark")){player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);level.setBlock(core.south(3),ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);camera=net.minecraft.world.phys.Vec3.atCenterOf(core).add(1,1,7);target=net.minecraft.world.phys.Vec3.atCenterOf(core).add(-7,5,-17);}
        }
        var d=target.subtract(camera.add(0,1.62,0));player.teleportTo(level,camera.x,camera.y,camera.z,(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));
        LogUtils.getLogger().info("ELYSIUM_BINDING_SCENE {} nightVision={}",scene,scene.startsWith("binding_")&&!scene.equals("binding_dark"));
    }
    private static void prisonCamera(ServerPlayer player,ServerLevel level,String scene){
        var arena=PRISON.arena();var voice=PRISON.voice();
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,12000,0,false,false));
        var controller=(dev.elysium.block.PrometheusBlockEntity)level.getBlockEntity(voice);controller.summon();
        net.minecraft.world.phys.Vec3 camera,target;
        if(scene.equals("prison_eagle")){camera=net.minecraft.world.phys.Vec3.atCenterOf(arena).add(7,29,14);target=net.minecraft.world.phys.Vec3.atCenterOf(arena).add(0,28,0);}
        else if(scene.equals("prison_teacher")){camera=net.minecraft.world.phys.Vec3.atCenterOf(voice).add(-12,16,31);target=net.minecraft.world.phys.Vec3.atCenterOf(voice).add(-18,25,-15);}
        else if(scene.equals("prison_dark")){player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);level.setBlock(voice.south(2),ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);camera=net.minecraft.world.phys.Vec3.atCenterOf(voice).add(4,1,7);target=net.minecraft.world.phys.Vec3.atCenterOf(voice).add(-8,5,-8);}
        else{camera=net.minecraft.world.phys.Vec3.atCenterOf(arena).add(25,22,43);target=net.minecraft.world.phys.Vec3.atCenterOf(arena).add(-24,17,-20);}
        var d=target.subtract(camera.add(0,1.62,0));player.teleportTo(level,camera.x,camera.y,camera.z,(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));
        LogUtils.getLogger().info("ELYSIUM_PRISON_SCENE {} nightVision={}",scene,!scene.equals("prison_dark"));
    }
    private static void underworldCamera(ServerPlayer player,ServerLevel level,String scene) {
        // Geometry inspection is explicitly night-vision assisted; the final lamp view is normal darkness.
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,12000,0,false,false));
        if(scene.equals("underworld_descent")) {
            player.teleportTo(level,DEPTHS.x+140.5,-270,DEPTHS.z+236.5,-90,58);return;
        }
        var kind=switch(scene) {
            case "underworld_chains"->dev.elysium.worldgen.TartarusTerrain.Region.CHAIN_FOREST;
            case "underworld_mire"->dev.elysium.worldgen.TartarusTerrain.Region.HOLLOW_MIRE;
            case "underworld_grave"->dev.elysium.worldgen.TartarusTerrain.Region.TITAN_GRAVE;
            default->dev.elysium.worldgen.TartarusTerrain.Region.ASH_WASTES;
        };
        var pos=REGIONS.get(kind);
        if(scene.equals("underworld_lamp")) {
            player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            level.setBlock(pos.above(),ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);
            player.teleportTo(level,pos.getX()+.5,pos.getY()+2,pos.getZ()+5.5,180,12);
        }else if(kind==dev.elysium.worldgen.TartarusTerrain.Region.TITAN_GRAVE){
            var c=net.minecraft.world.phys.Vec3.atCenterOf(pos).add(48,36,-90);var t=net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0,15,-25);var d=t.subtract(c.add(0,1.62,0));player.teleportTo(level,c.x,c.y,c.z,(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));
        }else if(kind==dev.elysium.worldgen.TartarusTerrain.Region.CHAIN_FOREST)player.teleportTo(level,pos.getX()+24.5,pos.getY()+25,pos.getZ()+30.5,141,-15);
        else if(kind==dev.elysium.worldgen.TartarusTerrain.Region.HOLLOW_MIRE)player.teleportTo(level,pos.getX()+.5,pos.getY()+7,pos.getZ()+.5,-45,5);
        else player.teleportTo(level,pos.getX()+.5,pos.getY()+14,pos.getZ()+.5,-45,12);
        LogUtils.getLogger().info("ELYSIUM_UNDERWORLD_SCENE {} at={} nightVision={}",scene,pos,!scene.equals("underworld_lamp"));
    }
    private static void camera(ServerPlayer player,ServerLevel level,int u,double y,int v,int tu,double ty,int tv) {
        double x=PLAN.worldX(u,v)+.5,z=PLAN.worldZ(u,v)+.5;
        double dx=PLAN.worldX(tu,tv)+.5-x,dz=PLAN.worldZ(tu,tv)+.5-z,dy=ty-y-1.62;
        float yaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90);
        float pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
        player.teleportTo(level,x,y,z,yaw,pitch);
    }
    private ForgeVisualServerProbe() {}
}
