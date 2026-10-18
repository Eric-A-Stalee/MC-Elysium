package dev.elysium.entity;

import dev.elysium.registry.ModItems;
import dev.elysium.story.Nightfall;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** False warmth over drowned grief. Its lament marks a place, never follows a dodging player. */
public final class MireMournerEntity extends Monster {
    public static final int WARNING=40;
    private static final EntityDataAccessor<Integer> CHANT=SynchedEntityData.defineId(MireMournerEntity.class,EntityDataSerializers.INT);
    private Vec3 lament;private int cooldown;
    public MireMournerEntity(EntityType<? extends MireMournerEntity> type,Level level){super(type,level);xpReward=9;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,34).add(Attributes.MOVEMENT_SPEED,.19).add(Attributes.FOLLOW_RANGE,24).add(Attributes.STEP_HEIGHT,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CHANT,0);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(5,new RandomStrollGoal(this,.5,120));goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,18));}
    public int chant(){return entityData.get(CHANT);}public Vec3 lament(){return lament;}
    public boolean sheltered(Player p){
        return p.getMainHandItem().is(ModItems.HOPES_EMBER.get())||p.getOffhandItem().is(ModItems.HOPES_EMBER.get())
            ||level().getBrightness(LightLayer.BLOCK,p.blockPosition())>=10
            ||level() instanceof ServerLevel l&&Nightfall.protectedAt(l,p.blockPosition());
    }
    public boolean canLament(Player p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&!sheltered(p)&&distanceToSqr(p)<18*18&&hasLineOfSight(p);}
    public boolean beginLament(Player p){if(chant()!=0||cooldown>0||!canLament(p))return false;lament=p.position();entityData.set(CHANT,1);getNavigation().stop();playSound(SoundEvents.ALLAY_HURT,.75f,.4f);return true;}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;
        if(cooldown>0)cooldown--;
        if(l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){entityData.set(CHANT,0);lament=null;return;}
        if(chant()>0&&lament!=null){
            getNavigation().stop();setDeltaMovement(getDeltaMovement().multiply(0,1,0));int age=chant();
            if(age%5==0)for(int i=0;i<16;i++){double a=i*Math.PI/8;l.sendParticles(ParticleTypes.ASH,lament.x+2.4*Math.cos(a),lament.y+.15,lament.z+2.4*Math.sin(a),1,0,.04,0,0);}
            if(age>=WARNING){
                for(var p:l.getEntitiesOfClass(Player.class,AABB.ofSize(lament,5,5,5)))if(canLament(p)&&p.position().subtract(lament).horizontalDistanceSqr()<2.4*2.4){
                    if(p.hurt(damageSources().mobAttack(this),6)){p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,70,1));var pull=position().subtract(p.position()).multiply(1,0,1).normalize();p.push(pull.x*.4,.12,pull.z*.4);}
                }
                l.sendParticles(ParticleTypes.LARGE_SMOKE,lament.x,lament.y+.5,lament.z,24,1.3,.4,1.3,.02);playSound(SoundEvents.SOUL_ESCAPE.value(),1,.5f);
                entityData.set(CHANT,0);lament=null;cooldown=100;
            }else entityData.set(CHANT,age+1);
        }else if(!isNoAi()&&tickCount%10==0){
            var p=l.getNearestPlayer(getX(),getY(),getZ(),18,e->e instanceof Player target&&canLament(target));
            if(p!=null)beginLament(p);
        }
    }
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean hit){super.dropCustomDeathLoot(l,s,hit);if(hit)spawnAtLocation(ModItems.CINDER_GLASS.get(),1+random.nextInt(2));}
}
