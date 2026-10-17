package dev.elysium.entity;

import dev.elysium.block.PrometheusBlockEntity;
import dev.elysium.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** A tethered airborne warden with readable dives, real pinions and long ground-level recovery. */
public final class BronzeEagleEntity extends Mob {
    public static final int PERCH=0,CIRCLE=1,TELL=2,DIVE=3,RECOVER=4,PINIONS=5;
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(BronzeEagleEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(BronzeEagleEntity.class,EntityDataSerializers.INT);
    private final ServerBossEvent boss=new ServerBossEvent(Component.translatable("entity.elysium.bronze_eagle"),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
    private BlockPos control;private Vec3 home,aim=Vec3.ZERO,direction=Vec3.ZERO;private boolean alternate,struck;
    public BronzeEagleEntity(EntityType<? extends BronzeEagleEntity> type,Level level){super(type,level);setNoGravity(true);setPersistenceRequired();xpReward=80;boss.setVisible(false);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,240).add(Attributes.ARMOR,6).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,64).add(Attributes.MOVEMENT_SPEED,0);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PHASE,PERCH);b.define(AGE,0);}
    public void bind(BlockPos controller,BlockPos arena){control=controller.immutable();home=Vec3.atBottomCenterOf(arena);setPos(home.add(0,26,0));}
    public BlockPos control(){return control;}
    public Vec3 home(){return home==null?position():home;}
    public int phase(){return entityData.get(PHASE);}public int phaseAge(){return entityData.get(AGE);}
    public void phase(int phase,int age){entityData.set(PHASE,phase);entityData.set(AGE,age);if(age==0)struck=false;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public void push(double x,double y,double z){}
    @Override public void tick(){
        setDeltaMovement(Vec3.ZERO);super.tick();setDeltaMovement(Vec3.ZERO);
        if(!(level() instanceof ServerLevel server)||!isAlive())return;
        if(home==null)home=position();
        if(control!=null&&server.hasChunkAt(control)&&(!(server.getBlockEntity(control) instanceof PrometheusBlockEntity prison)||prison.defeated())){discard();return;}
        Player target=server.getNearestPlayer(home.x,home.y,home.z,32,e->e instanceof Player p&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&Math.abs(p.getY()-home.y)<18);
        boss.setProgress(getHealth()/getMaxHealth());boss.setVisible(target!=null&&server.getDifficulty()!=Difficulty.PEACEFUL);
        if(target==null||server.getDifficulty()==Difficulty.PEACEFUL){phase(PERCH,0);moveToward(home.add(0,26,0),.25);return;}
        if(phase()==PERCH){phase(CIRCLE,0);return;}
        int age=phaseAge()+1;entityData.set(AGE,age);
        if(phase()==CIRCLE){
            double angle=tickCount*.025;moveToward(home.add(Math.cos(angle)*12,10,Math.sin(angle)*12),.40);
            if(age>=70){aim=target.position().add(0,.7,0);phase(alternate?PINIONS:TELL,0);alternate=!alternate;playSound(SoundEvents.PHANTOM_AMBIENT,1.4f,.55f);}
        }else if(phase()==TELL){
            if(age<20)aim=target.position().add(0,.7,0);
            face(aim.subtract(position()));
            if(age%4==0)server.sendParticles(ParticleTypes.ASH,aim.x,aim.y,aim.z,8,.5,.05,.5,.005);
            if(age>=36){direction=aim.subtract(position()).normalize();phase(DIVE,0);playSound(SoundEvents.PHANTOM_SWOOP,1.7f,.6f);}
        }else if(phase()==DIVE){
            var before=getBoundingBox();move(MoverType.SELF,direction.scale(.95));face(direction);
            if(!struck&&before.minmax(getBoundingBox()).inflate(.2).intersects(target.getBoundingBox())&&visible(target)){
                struck=true;if(target.hurt(damageSources().mobAttack(this),9))target.push(direction.x*.6,.28,direction.z*.6);
            }
            if(age>=27||horizontalCollision||verticalCollision||position().y<=home.y+.15||position().distanceToSqr(home)>32*32)phase(RECOVER,0);
        }else if(phase()==RECOVER){
            // A sword can reach the breast for four seconds, including after a successfully dodged dive.
            var flat=position().subtract(home);if(flat.horizontalDistance()>14)flat=flat.normalize().scale(14);
            moveToward(new Vec3(home.x+flat.x,home.y+.2,home.z+flat.z),.30);
            if(age>=85)phase(CIRCLE,0);
        }else if(phase()==PINIONS){
            face(aim.subtract(position()));
            if(age==1)playSound(SoundEvents.CHAIN_HIT,1.4f,.55f);
            if(age==32||age==38||age==44)fire(server,(age-38)*.014);
            if(age>=50)phase(RECOVER,0);
        }
    }
    private void moveToward(Vec3 goal,double speed){Vec3 delta=goal.subtract(position());if(delta.lengthSqr()<.01)return;Vec3 step=delta.normalize().scale(Math.min(speed,delta.length()));move(MoverType.SELF,step);face(step);}
    private void face(Vec3 d){if(d.horizontalDistanceSqr()<.0001)return;setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));setYHeadRot(getYRot());yBodyRot=getYRot();}
    private boolean visible(Player target){return level().clip(new ClipContext(position().add(0,1,0),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;}
    private void fire(ServerLevel level,double spread){
        var from=position().add(0,1.3,0);var d=aim.subtract(from).normalize();
        var feather=ModEntities.BRONZE_FEATHER.get().create(level);if(feather==null)return;
        feather.setOwner(this);feather.setPos(from);feather.setDeltaMovement(new Vec3(d.x*Math.cos(spread)-d.z*Math.sin(spread),d.y,d.z*Math.cos(spread)+d.x*Math.sin(spread)).scale(.9));level.addFreshEntity(feather);
        playSound(SoundEvents.TRIDENT_THROW,1,.6f);
    }
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);boss.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);boss.removePlayer(p);}
    @Override public void die(DamageSource source){super.die(source);boss.setVisible(false);if(level() instanceof ServerLevel server&&control!=null&&server.getBlockEntity(control) instanceof PrometheusBlockEntity prison)prison.defeat(getUUID());}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(control!=null)t.putLong("Control",control.asLong());if(home!=null){t.putDouble("HomeX",home.x);t.putDouble("HomeY",home.y);t.putDouble("HomeZ",home.z);}t.putBoolean("Alternate",alternate);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);control=t.contains("Control")?BlockPos.of(t.getLong("Control")):null;home=t.contains("HomeX")?new Vec3(t.getDouble("HomeX"),t.getDouble("HomeY"),t.getDouble("HomeZ")):position();alternate=t.getBoolean("Alternate");phase(PERCH,0);}
}
