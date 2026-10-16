package dev.elysium.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.EventHooks;

/** A visible, finite-lived projectile. It never explodes, lights blocks or loads chunks. */
public final class CinderBoltEntity extends Projectile implements ItemSupplier {
    public CinderBoltEntity(EntityType<? extends CinderBoltEntity> type,Level level) {
        super(type,level);setNoGravity(true);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override public ItemStack getItem(){return new ItemStack(Items.FIRE_CHARGE);}
    @Override protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity)&&!(entity instanceof SlagChimeraEntity)&&!(entity instanceof ChimeraEyePart)
                &&(!(entity instanceof Player p)||!p.isCreative()&&!p.isSpectator());
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide&&(tickCount>65||level().getDifficulty()==Difficulty.PEACEFUL
                ||!level().hasChunkAt(blockPosition())||!level().hasChunkAt(net.minecraft.core.BlockPos.containing(position().add(getDeltaMovement()))))) {
            discard();return;
        }
        if(!level().isClientSide) {
            var hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
            if(hit.getType()!=HitResult.Type.MISS&&!EventHooks.onProjectileImpact(this,hit)) {onHit(hit);return;}
        } else {
            var d=getDeltaMovement();
            for(int i=0;i<3;i++)level().addParticle(ParticleTypes.FLAME,getX()-d.x*i/3,getY()-d.y*i/3,getZ()-d.z*i/3,0,.005,0);
            if(tickCount%2==0)level().addParticle(ParticleTypes.SMOKE,getX(),getY(),getZ(),0,.01,0);
        }
        setPos(position().add(getDeltaMovement()));
    }
    @Override protected void onHit(HitResult hit) {
        if(!(level() instanceof ServerLevel server))return;
        if(hit instanceof EntityHitResult entityHit&&entityHit.getEntity() instanceof LivingEntity target
                &&getOwner() instanceof LivingEntity owner) {
            target.hurt(damageSources().mobProjectile(this,owner),6);
        }
        var p=hit.getLocation();
        server.sendParticles(ParticleTypes.FLAME,p.x,p.y,p.z,12,.16,.16,.16,.025);
        server.sendParticles(ParticleTypes.SMOKE,p.x,p.y,p.z,8,.2,.2,.2,.015);
        discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("CinderAge",tickCount);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);tickCount=tag.getInt("CinderAge");}
}
