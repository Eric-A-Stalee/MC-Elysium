package dev.elysium.entity;

import dev.elysium.block.BindingBlockEntity;
import dev.elysium.registry.ModItems;
import dev.elysium.story.Nightfall;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** A sentence still walking: slow pursuit, a locked chain lane, then a long exposed recovery. */
public final class ChainWardenEntity extends Monster {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(ChainWardenEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(ChainWardenEntity.class,EntityDataSerializers.INT);
    private BlockPos control,home;
    public ChainWardenEntity(EntityType<? extends ChainWardenEntity> type,Level l){super(type,l);xpReward=20;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,90).add(Attributes.ARMOR,10).add(Attributes.MOVEMENT_SPEED,.18).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,28).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.STEP_HEIGHT,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PHASE,0);b.define(AGE,0);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new Sentence());goalSelector.addGoal(5,new RandomStrollGoal(this,.65));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true,e->!(level() instanceof ServerLevel l)||!Nightfall.protectedAt(l,e.blockPosition())));
    }
    public void bind(BlockPos core,BlockPos resting){control=core.immutable();home=resting.immutable();restrictTo(home,24);setPersistenceRequired();setPos(Vec3.atBottomCenterOf(resting));}
    public BlockPos control(){return control;}public int phase(){return entityData.get(PHASE);}public int phaseAge(){return entityData.get(AGE);}
    public void phase(int phase,int age){entityData.set(PHASE,phase);entityData.set(AGE,age);}
    private Vec3 forward(){double a=Math.toRadians(getYRot());return new Vec3(-Math.sin(a),0,Math.cos(a));}
    public Vec3 chainEnd(){return position().add(forward().scale(8)).add(0,.2,0);}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;
        if(control!=null&&l.hasChunkAt(control)&&(!(l.getBlockEntity(control) instanceof BindingBlockEntity core)||core.sealed())){discard();return;}
        if(getTarget() instanceof Player p&&(p.isCreative()||p.isSpectator()||Nightfall.protectedAt(l,p.blockPosition()))){setTarget(null);phase(0,0);getNavigation().stop();}
        if(phase()==0)return;
        getNavigation().stop();int age=phaseAge()+1;entityData.set(AGE,age);
        if(phase()==1){
            if(age%5==0)for(int i=1;i<=8;i++){var v=position().add(forward().scale(i));l.sendParticles(ParticleTypes.ASH,v.x,getY()+.15,v.z,2,.4,.03,.4,.005);}
            if(age>=36){phase(2,0);playSound(SoundEvents.IRON_GOLEM_ATTACK,1.4f,.55f);}
        }else if(phase()==2){
            if(age==1){
                for(var p:l.getEntitiesOfClass(Player.class,getBoundingBox().inflate(9,4,9)))if(canStrike(p)){
                    if(p.hurt(damageSources().mobAttack(this),10)){p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,0));p.push(forward().x*.5,.18,forward().z*.5);}
                }
                var end=chainEnd();l.sendParticles(ParticleTypes.LARGE_SMOKE,end.x,end.y,end.z,18,.8,.2,.8,.03);
            }
            if(age>=10)phase(3,0);
        }else if(age>=55)phase(0,0);
    }
    public boolean canStrike(Player p){
        if(!p.isAlive()||p.isCreative()||p.isSpectator()||!(level() instanceof ServerLevel l)||Nightfall.protectedAt(l,p.blockPosition()))return false;
        var delta=p.position().subtract(position());double along=delta.dot(forward());
        if(along<0||along>8.7||Math.abs(delta.y)>3||delta.multiply(1,0,1).subtract(forward().scale(along)).lengthSqr()>2.25)return false;
        return l.clip(new ClipContext(position().add(0,1.5,0),p.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;
    }
    @Override public void die(DamageSource source){super.die(source);if(level() instanceof ServerLevel l&&control!=null&&l.getBlockEntity(control) instanceof BindingBlockEntity core)core.wardenDefeated(getUUID());}
    @Override protected void dropCustomDeathLoot(ServerLevel l,DamageSource s,boolean hit){super.dropCustomDeathLoot(l,s,hit);if(hit){spawnAtLocation(Items.CHAIN,2+random.nextInt(3));spawnAtLocation(ModItems.CINDER_GLASS.get(),2);}}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(control!=null)t.putLong("Control",control.asLong());if(home!=null)t.putLong("Home",home.asLong());}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);control=t.contains("Control")?BlockPos.of(t.getLong("Control")):null;home=t.contains("Home")?BlockPos.of(t.getLong("Home")):null;if(home!=null)restrictTo(home,24);phase(0,0);}
    private final class Sentence extends Goal {
        Sentence(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return getTarget()!=null&&getTarget().isAlive();}
        @Override public void tick(){
            var target=getTarget();if(target==null||phase()!=0)return;
            if(home!=null&&target.blockPosition().distSqr(home)>32*32){setTarget(null);getNavigation().moveTo(home.getX(),home.getY(),home.getZ(),1);return;}
            getLookControl().setLookAt(target,25,25);
            if(distanceToSqr(target)<8*8&&hasLineOfSight(target)){
                var d=target.position().subtract(position());setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));setYHeadRot(getYRot());yBodyRot=getYRot();phase(1,0);getNavigation().stop();playSound(SoundEvents.CHAIN_PLACE,1.5f,.5f);
            }else getNavigation().moveTo(target,1);
        }
        @Override public void stop(){getNavigation().stop();}
    }
}
