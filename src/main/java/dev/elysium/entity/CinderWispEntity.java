package dev.elysium.entity;

import dev.elysium.registry.ModItems;
import dev.elysium.story.Nightfall;
import dev.elysium.story.RealmCycle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** A stray cinder from the opened bindings. Telegraphs a short physical rush; never damages terrain. */
public final class CinderWispEntity extends Mob {
    private static final EntityDataAccessor<Boolean> CHARGING=SynchedEntityData.defineId(CinderWispEntity.class,EntityDataSerializers.BOOLEAN);
    private int age,cooldown=40,windup,rush;
    private Vec3 aim=Vec3.ZERO;
    public CinderWispEntity(EntityType<? extends CinderWispEntity> type,Level level){super(type,level);setNoGravity(true);xpReward=4;}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.FOLLOW_RANGE,32).add(Attributes.ATTACK_DAMAGE,3);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CHARGING,false);}
    public boolean charging(){return entityData.get(CHARGING);}
    @Override public void tick() {
        super.tick();
        if(level().isClientSide){if(tickCount%3==0)level().addParticle(ParticleTypes.SMALL_FLAME,getX(),getY()+.4,getZ(),0,.015,0);return;}
        var server=(ServerLevel)level();age++;
        if(age>2400||server.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||!server.dimension().equals(dev.elysium.portal.ElysiumTravel.DIMENSION)||!RealmCycle.get(server).unsettled()){discard();return;}
        var target=server.getNearestPlayer(getX(),getY(),getZ(),32,e->e instanceof Player p&&p.isAlive()&&!p.isCreative()&&!p.isSpectator());
        if(target==null){setDeltaMovement(getDeltaMovement().scale(.8));return;}
        var destination=target.position().add(0,1.5,0);var delta=destination.subtract(position());
        if(Nightfall.protectedAt(server,target.blockPosition())||dev.elysium.forge.KeeperLampLight.ward(target)) {
            windup=0;rush=0;entityData.set(CHARGING,false);setDeltaMovement(delta.normalize().scale(-.16).add(0,.025,0));return;
        }
        if(rush>0) {
            setDeltaMovement(aim.scale(.55));rush--;
            if(getBoundingBox().inflate(.35).intersects(target.getBoundingBox())&&lineOfSight(target)) {
                target.hurt(server.damageSources().mobAttack(this),3);rush=0;cooldown=70;setDeltaMovement(aim.scale(-.12));
            }
            if(horizontalCollision||verticalCollision){rush=0;cooldown=70;}
            return;
        }
        if(windup>0) {
            setDeltaMovement(Vec3.ZERO);windup--;
            if(windup==0){aim=destination.subtract(position()).normalize();rush=14;cooldown=70;entityData.set(CHARGING,false);playSound(SoundEvents.FIRECHARGE_USE,.6f,1.5f);}
            return;
        }
        cooldown--;
        if(delta.length()<10&&cooldown<=0&&lineOfSight(target)) {
            windup=24;entityData.set(CHARGING,true);playSound(SoundEvents.BLAZE_BURN,.5f,1.6f);return;
        }
        Vec3 movement=delta.length()>5?delta.normalize().scale(.10):new Vec3(-delta.z,Math.sin(tickCount*.06)*.25,delta.x).normalize().scale(.06);
        setDeltaMovement(movement);
    }
    private boolean lineOfSight(Player target){return level().clip(new ClipContext(position(),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;}
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean recentlyHit){super.dropCustomDeathLoot(level,source,recentlyHit);if(recentlyHit)spawnAtLocation(ModItems.CINDER_GLASS.get(),1);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("CinderAge",age);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);age=Math.max(0,tag.getInt("CinderAge"));}
}
