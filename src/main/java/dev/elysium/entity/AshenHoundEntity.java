package dev.elysium.entity;

import dev.elysium.registry.ModItems;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Blind hunger hunts the last sound, not the player's position after they fall silent. */
public final class AshenHoundEntity extends Monster {
    private static final EntityDataAccessor<Integer> JAW=SynchedEntityData.defineId(AshenHoundEntity.class,EntityDataSerializers.INT);
    private Vec3 heard;private int memory,biteCooldown,windup,pounce,age;private boolean struck;
    public AshenHoundEntity(EntityType<? extends AshenHoundEntity> type,Level l){super(type,l);xpReward=7;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,26).add(Attributes.MOVEMENT_SPEED,.29).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.FOLLOW_RANGE,32).add(Attributes.ARMOR,2).add(Attributes.STEP_HEIGHT,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(JAW,0);}
    public int jaw(){return entityData.get(JAW);}public Vec3 lastSound(){return heard;}public int memory(){return memory;}
    public static double hearingRadius(Player p){
        boolean charm=p.getOffhandItem().is(ModItems.HUSH_CHARM.get());
        if(p.isSprinting())return charm?14:32;
        if(p.isShiftKeyDown())return 2.5;
        return p.getDeltaMovement().horizontalDistanceSqr()>.0004?(charm?5:14):3;
    }
    public boolean hears(Player p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&distanceToSqr(p)<=Math.pow(hearingRadius(p),2)
        &&level().clip(new ClipContext(getEyePosition(),p.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;}
    public void hear(Vec3 pos){heard=pos;memory=120;}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new Hunt());goalSelector.addGoal(5,new RandomStrollGoal(this,.7,100));}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l))return;age++;
        if(biteCooldown>0)biteCooldown--;if(memory>0)memory--;else heard=null;
        if(tickCount%10==0){
            var p=l.getNearestPlayer(getX(),getY(),getZ(),32,e->e instanceof Player player&&hears(player));if(p!=null)hear(p.position());
            if(age>12000&&!hasCustomName()&&l.getNearestPlayer(getX(),getY(),getZ(),48,false)==null)discard();
        }
        if(windup>0){windup--;entityData.set(JAW,18-windup);getNavigation().stop();
            if(windup==0&&heard!=null){var d=heard.subtract(position()).multiply(1,0,1).normalize();setDeltaMovement(d.scale(.72).add(0,.35,0));pounce=16;struck=false;playSound(SoundEvents.WOLF_GROWL,.8f,.55f);}
        }else if(pounce>0){
            pounce--;if(!struck)for(var p:l.getEntitiesOfClass(Player.class,getBoundingBox().inflate(.3),this::canBite)){
                if(p.hurt(damageSources().mobAttack(this),6)){struck=true;p.push(getDeltaMovement().x*.3,.1,getDeltaMovement().z*.3);}break;
            }
            if(pounce==0){biteCooldown=32;entityData.set(JAW,0);}
        }
    }
    private boolean canBite(Player p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&level().clip(new ClipContext(getEyePosition(),p.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;}
    @Override public boolean hurt(DamageSource source,float amount){boolean hit=super.hurt(source,amount);if(hit&&source.getEntity() instanceof LivingEntity attacker&&level() instanceof ServerLevel l){hear(attacker.position());for(var pack:l.getEntitiesOfClass(AshenHoundEntity.class,getBoundingBox().inflate(18)))pack.hear(attacker.position());}return hit;}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean hit){super.dropCustomDeathLoot(l,s,hit);if(hit){spawnAtLocation(ModItems.ASHEN_FANG.get(),1+random.nextInt(2));if(random.nextBoolean())spawnAtLocation(ModItems.CINDER_GLASS.get());}}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putInt("Age",age);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);age=Math.max(0,t.getInt("Age"));heard=null;memory=0;windup=0;pounce=0;}
    private final class Hunt extends Goal {
        Hunt(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return heard!=null&&memory>0;}
        @Override public void tick(){
            if(heard==null)return;getLookControl().setLookAt(heard.x,heard.y+.5,heard.z);
            if(windup>0||pounce>0)return;
            if(tickCount%10==0)getNavigation().moveTo(heard.x,heard.y,heard.z,1.12);
            if(biteCooldown<=0&&onGround()&&heard.distanceToSqr(position())<20){windup=18;entityData.set(JAW,1);getNavigation().stop();playSound(SoundEvents.WOLF_PANT,.7f,.55f);}
        }
        @Override public void stop(){getNavigation().stop();}
    }
}
