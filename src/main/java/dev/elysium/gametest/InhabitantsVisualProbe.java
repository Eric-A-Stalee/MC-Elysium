package dev.elysium.gametest;

import dev.elysium.block.*;
import dev.elysium.entity.*;
import dev.elysium.registry.*;
import dev.elysium.story.RealmCycle;
import dev.elysium.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Actual client inspection of naturally generated prisons and explicitly posed living creatures. */
final class InhabitantsVisualProbe {
    static final String[] SCENES={"inhabitant_captive","inhabitant_face","inhabitant_command","inhabitant_dark","inhabitant_mourner","inhabitant_moths","inhabitant_hunger","inhabitant_sentenced","inhabitant_eagle","inhabitant_perch"};
    private static DeepBindingPiece[] bindings;private static PrometheusPrisonPiece prison;
    private static MireMournerEntity mourner;private static AshenHoundEntity hound;private static ChainWardenEntity warden;
    static void prepare(ServerLevel l){
        bindings=BindingNaturalGenerationProbe.generateAndVerify(l);prison=PrometheusNaturalGenerationProbe.generateAndVerify(l);
        var cycle=RealmCycle.get(l);cycle.awaken(bindings[2].core());
        var core=(BindingBlockEntity)l.getBlockEntity(bindings[2].core());
        if(!core.summonTitan()&&core.titan()==null)throw new IllegalStateException("Natural Dominion has no room for its living captive");
        var titan=(CrownlessEntity)l.getEntity(core.titan());
        if(!l.noCollision(titan))throw new IllegalStateException("Natural captive intersects the prison");
        mourner=ModEntities.MIRE_MOURNER.get().create(l);pose(l,mourner,bindings[1].core().south(9));
        hound=ModEntities.ASHEN_HOUND.get().create(l);pose(l,hound,bindings[0].core().south(9));
        warden=ModEntities.CHAIN_WARDEN.get().create(l);pose(l,warden,bindings[2].core().south(9));
        var lamp=bindings[1].core().south(17);l.setBlock(lamp,ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);
        for(int i=0;i<4;i++){var moth=ModEntities.ASH_MOTH.get().create(l);moth.setPos(Vec3.atCenterOf(lamp).add((i%2)*2-1,1.5,(i/2)*2-1));moth.setPersistenceRequired();l.addFreshEntity(moth);moth.findLight();}
        com.mojang.logging.LogUtils.getLogger().info("ELYSIUM_INHABITANTS_NATURAL captive={} dimensions={}x{}",titan.position(),titan.getBbWidth(),titan.getBbHeight());
    }
    private static void pose(ServerLevel l,Mob mob,BlockPos at){mob.setPos(Vec3.atBottomCenterOf(at));mob.setNoAi(true);mob.setNoGravity(true);mob.setPersistenceRequired();mob.setYRot(0);mob.setYHeadRot(0);l.addFreshEntity(mob);}
    static void scene(ServerPlayer p,ServerLevel l,String scene){
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,12000,0,false,false));
        Vec3 camera,target;
        var core=bindings[2].core();var home=Vec3.atBottomCenterOf(core.offset(0,0,-23));
        switch(scene){
            case "inhabitant_captive"->{camera=home.add(35,18,56);target=home.add(0,19,0);}
            case "inhabitant_face"->{camera=home.add(12,28,20);target=home.add(0,27,0);}
            case "inhabitant_command"->{
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,2400,4,false,false));
                p.addEffect(new MobEffectInstance(MobEffects.SATURATION,2400,4,false,false));
                p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();
                // The central surveyed approach is guaranteed clear; the side ruins are cover.
                camera=home.add(0,3,40);target=home.add(0,24,0);
            }
            case "inhabitant_dark"->{p.removeEffect(MobEffects.NIGHT_VISION);l.setBlock(core.south(3),ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),3);camera=Vec3.atCenterOf(core).add(3,1,8);target=home.add(0,22,0);}
            case "inhabitant_mourner"->{camera=mourner.position().add(4,1,7);target=mourner.position().add(0,1.7,0);}
            case "inhabitant_moths"->{var lamp=Vec3.atCenterOf(bindings[1].core().south(17));camera=lamp.add(4,1,5);target=lamp.add(0,1.3,0);}
            case "inhabitant_hunger"->{camera=hound.position().add(4,1,5);target=hound.position().add(0,.7,0);}
            case "inhabitant_sentenced"->{camera=warden.position().add(5,1,9);target=warden.position().add(0,2.5,0);}
            default->{
                var controller=(PrometheusBlockEntity)l.getBlockEntity(prison.voice());controller.summon();var eagle=(BronzeEagleEntity)l.getEntity(controller.eagle());
                if(eagle==null)throw new IllegalStateException("No natural eagle for the scale inspection");
                eagle.setNoAi(true);eagle.setYRot(0);eagle.setYHeadRot(0);eagle.setPos(Vec3.atBottomCenterOf(prison.arena()).add(0,26,0));
                eagle.phase(scene.equals("inhabitant_perch")?BronzeEagleEntity.PERCH:BronzeEagleEntity.CIRCLE,0);
                camera=eagle.position().add(scene.equals("inhabitant_perch")?9:16,5,scene.equals("inhabitant_perch")?18:27);target=eagle.position().add(0,2,0);
            }
        }
        var d=target.subtract(camera.add(0,1.62,0));p.teleportTo(l,camera.x,camera.y,camera.z,(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));
        if(scene.equals("inhabitant_command")){
            var binding=(BindingBlockEntity)l.getBlockEntity(core);
            var titan=(CrownlessEntity)l.getEntity(binding.titan());
            var hit=l.clip(new net.minecraft.world.level.ClipContext(titan.position().add(0,3,7),p.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,titan));
            com.mojang.logging.LogUtils.getLogger().info("ELYSIUM_CAPTIVE_OBSERVER player={} targetable={} phase={} released={} difficulty={} cover={}",p.position(),titan.canCommand(p),titan.phase(),RealmCycle.get(l).released(),l.getDifficulty(),hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS?"none":hit.getBlockPos());
            if(!titan.canCommand(p))throw new IllegalStateException("The natural central approach obstructs the captive's command at "+hit.getBlockPos());
        }
        com.mojang.logging.LogUtils.getLogger().info("ELYSIUM_INHABITANT_SCENE {} nightVision={}",scene,!scene.equals("inhabitant_dark"));
    }
    private InhabitantsVisualProbe(){}
}
