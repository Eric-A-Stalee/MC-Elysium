package dev.elysium.entity;

import dev.elysium.block.BindingBlockEntity;
import dev.elysium.story.RealmCycle;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Dominion's living prisoner. The bindings hold his body; the First Flame lets his command escape. */
public final class CrownlessEntity extends Mob {
    public static final int DORMANT=0,WATCHING=1,INHALE=2,COMMAND=3;
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(CrownlessEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(CrownlessEntity.class,EntityDataSerializers.INT);
    private BlockPos control;private Vec3 home;private int wait=160;
    private final Set<UUID> struck=new HashSet<>();
    public CrownlessEntity(EntityType<? extends CrownlessEntity> type,Level level){super(type,level);setNoGravity(true);setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,800).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.FOLLOW_RANGE,64);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PHASE,DORMANT);b.define(AGE,0);}
    public void bind(BlockPos core){control=core.immutable();home=Vec3.atBottomCenterOf(core.offset(0,0,-23));setPos(home);}
    public BlockPos control(){return control;}public int phase(){return entityData.get(PHASE);}public int age(){return entityData.get(AGE);}
    public void phase(int value,int age){entityData.set(PHASE,value);entityData.set(AGE,age);if(age==0)struck.clear();}
    public double waveRadius(float partial){return 12+Math.min(1,(age()+partial)/50)*34;}
    public boolean canCommand(Player p){
        if(!p.isAlive()||p.isCreative()||p.isSpectator()||Math.abs(p.getY()-getY())>9||level() instanceof ServerLevel l&&dev.elysium.story.Nightfall.protectedAt(l,p.blockPosition()))return false;
        // Low origin makes ordinary ruins useful cover. A tall head must not see over every refuge.
        return level().clip(new ClipContext(position().add(0,3,7),p.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;
    }
    @Override public void tick(){
        setDeltaMovement(Vec3.ZERO);super.tick();setDeltaMovement(Vec3.ZERO);
        if(!(level() instanceof ServerLevel l))return;if(home!=null)setPos(home);
        if(control==null||!l.hasChunkAt(control))return;
        if(!(l.getBlockEntity(control) instanceof BindingBlockEntity binding)){discard();return;}
        if(binding.sealed()||RealmCycle.get(l).bindingHeld(2)||!RealmCycle.get(l).released()||l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){phase(DORMANT,0);wait=160;return;}
        var target=l.getNearestPlayer(getX(),getY(),getZ(),48,e->e instanceof Player p&&canCommand(p));
        if(target==null){phase(WATCHING,0);wait=160;return;}
        getLookControl().setLookAt(target,2,2);
        if(phase()==DORMANT)phase(WATCHING,0);
        if(phase()==WATCHING){
            if(--wait<=0){phase(INHALE,0);playSound(SoundEvents.WARDEN_HEARTBEAT,3,.5f);target.displayClientMessage(Component.translatable("message.elysium.crownless.inhale"),true);}return;
        }
        int age=age()+1;entityData.set(AGE,age);
        if(phase()==INHALE){
            if(age%6==0)l.sendParticles(ParticleTypes.ASH,getX(),getY()+24,getZ()+4,12,3,2,1,.02);
            if(age>=60){phase(COMMAND,0);playSound(SoundEvents.WARDEN_ROAR,3,.5f);
                for(var h:l.getEntitiesOfClass(AshenHoundEntity.class,getBoundingBox().inflate(48)))h.hear(target.position());
                for(var w:l.getEntitiesOfClass(ChainWardenEntity.class,getBoundingBox().inflate(48)))w.setTarget(target);
            }
        }else if(phase()==COMMAND){
            double radius=waveRadius(0);
            if(age%2==0)for(int i=0;i<64;i++){double a=i*Math.PI/32;var at=position().add(Math.cos(a)*radius,.3,Math.sin(a)*radius);if(l.hasChunkAt(BlockPos.containing(at)))l.sendParticles(ParticleTypes.ASH,at.x,at.y,at.z,2,.2,.15,.2,.01);}
            for(var p:l.players())if(canCommand(p)&&Math.abs(p.position().subtract(position()).horizontalDistance()-radius)<1.4&&struck.add(p.getUUID())){
                if(p.hurt(damageSources().mobAttack(this),5)){var d=p.position().subtract(position()).multiply(1,0,1).normalize();p.push(d.x*.7,.2,d.z*.7);}
            }
            if(age>=50){phase(WATCHING,0);wait=220;}
        }
    }
    @Override public boolean hurt(DamageSource s,float amount){return s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)&&super.hurt(s,amount);}
    @Override public boolean isPushable(){return false;}
    @Override public void push(double x,double y,double z){}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(control!=null)t.putLong("Control",control.asLong());}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);if(t.contains("Control"))bind(BlockPos.of(t.getLong("Control")));phase(DORMANT,0);}
}
