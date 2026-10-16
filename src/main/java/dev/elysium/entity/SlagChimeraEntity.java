package dev.elysium.entity;

import dev.elysium.block.DepthsControlBlockEntity;
import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModEntities;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/** An immobile, tar-heavy body with an independently targetable, lowering eye. */
public final class SlagChimeraEntity extends Mob {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(SlagChimeraEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(SlagChimeraEntity.class,EntityDataSerializers.INT);
    private final ChimeraEyePart eye=new ChimeraEyePart(this);
    private final PartEntity<?>[] parts={eye};
    private final ServerBossEvent boss=new ServerBossEvent(Component.translatable("entity.elysium.slag_chimera"),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.PROGRESS);
    private BlockPos control;
    private Vec3 resting;
    private int nextAttack;
    private boolean rewarded;
    private Vec3 lockedAim;
    private final Set<UUID> struck=new HashSet<>();
    public SlagChimeraEntity(EntityType<? extends SlagChimeraEntity> type,Level level) {
        super(type,level);setPersistenceRequired();setNoGravity(true);xpReward=100;boss.setVisible(false);
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,360).add(Attributes.ARMOR,8)
                .add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.FOLLOW_RANGE,48);
    }
    @Override protected void registerGoals() {}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);builder.define(PHASE,0);builder.define(AGE,0);
    }
    public void bind(BlockPos origin,Vec3 home){control=origin.immutable();resting=home;setPos(home);}
    public BlockPos control(){return control;}
    public int phase(){return entityData.get(PHASE);}
    public int phaseAge(){return entityData.get(AGE);}
    public boolean vulnerable(){return phase()==4;}
    public void phase(int phase,int age){entityData.set(PHASE,phase);entityData.set(AGE,age);if(age==0)struck.clear();}
    public double eyeHeightOffset(float partial) {
        double dip=phase()==4?Math.sin(Math.min(60,phaseAge()+partial)*Math.PI/60):0;
        return 10-6*dip*dip;
    }
    /** Shared by the rendered socket, its hitbox, its light and the projectile muzzle. */
    public Vec3 eyeLocal(float partial) {
        return new Vec3(.42*Math.sin((tickCount+partial)*.027),eyeHeightOffset(partial),6.25);
    }
    public Vec3 localToWorld(Vec3 local) {
        double angle=Math.toRadians(getYRot()),s=Math.sin(angle),c=Math.cos(angle);
        return position().add(local.x*c-local.z*s,local.y,local.z*c+local.x*s);
    }
    public Vec3 eyeCenter(float partial){return localToWorld(eyeLocal(partial));}
    public double tailAngle(float partial) {
        if(phase()!=2)return .08*Math.sin((tickCount+partial)*.024);
        double age=phaseAge()+partial;
        return age<35?-.95*Math.min(1,age/25):-.95+1.9*Mth.clamp((age-35)/14,0,1);
    }
    public double pulseRadius(float partial){return 10+10*Mth.clamp((phaseAge()+partial-45)/22,0,1);}
    public Vec3 tailJoint(double t,float partial) {
        double drop=phase()==2&&phaseAge()>=35?Math.sin(Mth.clamp((phaseAge()+partial-35)/14,0,1)*Math.PI):0;
        return new Vec3(.6+.8*Math.sin(t*Math.PI),2.8+8*Math.sin(t*1.6)-2.2*Math.pow(t,4)-drop*7*t,
                -3.2-7.7*Math.sin(t*2.0)-drop*3*t);
    }
    public Vec3 tailTip(float partial) {
        var local=tailJoint(1,partial).add(-.55,-2.2,2);double a=tailAngle(partial),s=Math.sin(a),c=Math.cos(a);
        return localToWorld(new Vec3(local.x*c-local.z*s,local.y,local.z*c+local.x*s));
    }
    @Override public boolean isMultipartEntity(){return true;}
    @Override public PartEntity<?>[] getParts(){return parts;}
    @Override public void setId(int id){super.setId(id);if(eye!=null)eye.setId(id+1);}
    public ChimeraEyePart eye(){return eye;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public void push(double x,double y,double z) {}
    @Override public void tick() {
        setDeltaMovement(Vec3.ZERO);super.tick();setDeltaMovement(Vec3.ZERO);
        if(!level().isClientSide&&resting!=null)setPos(resting);
        Vec3 center=eyeCenter(0);
        eye.xo=eye.getX();eye.yo=eye.getY();eye.zo=eye.getZ();
        eye.setPos(center.x,center.y-1.5,center.z);
        if(!(level() instanceof ServerLevel server)||!isAlive())return;
        boss.setProgress(getHealth()/getMaxHealth());
        if(tickCount%10==0) {
            var light=BlockPos.containing(center);
            if(server.hasChunkAt(light)&&server.getBlockState(light).isAir())server.setBlock(light,ModBlocks.CHIMERA_GLOW.get().defaultBlockState(),3);
        }
        Player target=server.getNearestPlayer(getX(),getY()+2,getZ(),46,p->p instanceof Player candidate&&!candidate.isCreative()&&!candidate.isSpectator()&&candidate.isAlive()&&Math.abs(candidate.getY()-getY())<30);
        if(target==null||server.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){phase(0,0);boss.setVisible(false);return;}
        boss.setVisible(true);
        if(phase()==0) {phase(1,0);return;}
        int age=phaseAge()+1;entityData.set(AGE,age);
        if(phase()==1&&age<=30||phase()==3&&age<=14) {
            double dx=target.getX()-getX(),dz=target.getZ()-getZ();
            setYRot(Mth.approachDegrees(getYRot(),(float)Math.toDegrees(Math.atan2(-dx,dz)),phase()==1?6:2));setYHeadRot(getYRot());
            lockedAim=target.position().add(0,target.getBbHeight()*.55,0);
        }
        if(phase()!=4&&age==1)server.playSound(null,blockPosition(),
                phase()==1?SoundEvents.BLAZE_BURN:SoundEvents.IRON_GOLEM_STEP,SoundSource.HOSTILE,1.2f,.5f);
        if(phase()!=4&&age%5==0) {
            var e=eyeCenter(0);server.sendParticles(ParticleTypes.SMALL_FLAME,e.x,e.y,e.z,4,.7,.7,.7,.01);
            if(phase()==3&&age<45) {ring(server,10,false);ring(server,20,false);}
        }
        if(phase()==1) {
            if(age==45||age==51||age==57)fireBolt(server,(age-51)*.006);
            if(age>=64)phase(4,0);
        } else if(phase()==2) {
            if(age==35)server.playSound(null,blockPosition(),SoundEvents.IRON_GOLEM_ATTACK,SoundSource.HOSTILE,2,.55f);
            if(age>=35&&age<=49)sweep(server);
            if(age>=50)phase(4,0);
        } else if(phase()==3) {
            if(age==45)server.playSound(null,blockPosition(),SoundEvents.IRON_GOLEM_ATTACK,SoundSource.HOSTILE,2,.4f);
            if(age>=45&&age<=67) {
                if(age%2==1)ring(server,pulseRadius(0),true);
                for(Player player:server.players())if(canStrike(player,3)
                        &&Math.abs(player.position().subtract(position()).horizontalDistance()-pulseRadius(0))<1.05)damageOnce(player,7);
            }
            if(age>=68)phase(4,0);
        } else if(phase()==4&&age>=60){nextAttack=(nextAttack+1)%3;phase(nextAttack+1,0);}
    }
    /** Both warning and damage use the same facing. Solid cover blocks every attack. */
    public boolean canStrike(Player player,int attack) {
        if(player.isCreative()||player.isSpectator()||!player.isAlive())return false;
        Vec3 delta=player.position().subtract(position());double distance=delta.horizontalDistance();
        if(Math.abs(delta.y)>8||distance>(attack==1?46:attack==2?13:20))return false;
        Vec3 facing=new Vec3(-Math.sin(Math.toRadians(getYRot())),0,Math.cos(Math.toRadians(getYRot())));
        double dot=new Vec3(delta.x,0,delta.z).normalize().dot(facing);
        if(attack==1&&dot<.93||attack==2&&dot>.3||attack==3&&distance<10)return false;
        Vec3 from=attack==1?eyeCenter(0):position().add(0,2,0);
        return level().clip(new ClipContext(from,player.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;
    }
    private void fireBolt(ServerLevel server,double spread) {
        Vec3 from=eyeCenter(0),aim=lockedAim==null?localToWorld(new Vec3(0,1,30)):lockedAim;
        Vec3 d=aim.subtract(from).normalize();
        d=new Vec3(d.x*Math.cos(spread)-d.z*Math.sin(spread),d.y,d.z*Math.cos(spread)+d.x*Math.sin(spread));
        var bolt=ModEntities.CINDER_BOLT.get().create(server);
        if(bolt==null)return;
        bolt.setOwner(this);bolt.setPos(from);bolt.setDeltaMovement(d.scale(.8));server.addFreshEntity(bolt);
        server.playSound(null,BlockPos.containing(from),SoundEvents.BLAZE_SHOOT,SoundSource.HOSTILE,1.5f,.65f);
        server.sendParticles(ParticleTypes.FLAME,from.x,from.y,from.z,12,.35,.35,.1,.02);
    }
    private void sweep(ServerLevel server) {
        Vec3 tip=tailTip(0);
        var swept=net.minecraft.world.phys.AABB.ofSize(tip,4.8,4,4.8);
        for(Player player:server.players()) {
            if(canStrike(player,2)&&player.getBoundingBox().intersects(swept))damageOnce(player,9);
        }
        server.sendParticles(ParticleTypes.FLAME,tip.x,Math.max(tip.y,getY()+.2),tip.z,5,.5,.3,.5,.01);
    }
    private void damageOnce(Player player,float damage) {
        if(!struck.add(player.getUUID()))return;
        if(player.hurt(damageSources().mobAttack(this),damage)) {
            Vec3 d=player.position().subtract(position()).normalize();player.push(d.x*.8,.3,d.z*.8);
        }
    }
    private void ring(ServerLevel server,double radius,boolean hot) {
        for(int i=0;i<48;i++) {
            double angle=i*Math.PI/24;
            Vec3 p=position().add(Math.cos(angle)*radius,.18,Math.sin(angle)*radius);
            if(level().clip(new ClipContext(position().add(0,2,0),p,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS)
                server.sendParticles(hot?ParticleTypes.FLAME:ParticleTypes.ASH,p.x,p.y,p.z,1,.05,.02,.05,.005);
        }
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        return super.hurt(source,source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)?amount:amount*.2f);
    }
    public boolean hurtEye(DamageSource source,float amount) {return super.hurt(source,vulnerable()?amount:amount*.3f);}
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);boss.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);boss.removePlayer(player);}
    @Override public void die(DamageSource source) {
        super.die(source);boss.setVisible(false);
        if(!rewarded&&level() instanceof ServerLevel server&&control!=null&&server.getBlockEntity(control) instanceof DepthsControlBlockEntity heart) {
            rewarded=true;heart.defeat(getUUID());
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);if(control!=null)tag.putLong("Control",control.asLong());
        if(resting!=null){tag.putDouble("RestX",resting.x);tag.putDouble("RestY",resting.y);tag.putDouble("RestZ",resting.z);}
        tag.putBoolean("Rewarded",rewarded);tag.putInt("NextAttack",nextAttack);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);control=tag.contains("Control")?BlockPos.of(tag.getLong("Control")):null;
        resting=tag.contains("RestX")?new Vec3(tag.getDouble("RestX"),tag.getDouble("RestY"),tag.getDouble("RestZ")):position();
        rewarded=tag.getBoolean("Rewarded");nextAttack=Math.floorMod(tag.getInt("NextAttack"),3);phase(0,0);
    }
}
