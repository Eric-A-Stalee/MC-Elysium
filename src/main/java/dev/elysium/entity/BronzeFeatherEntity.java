package dev.elysium.entity;

import dev.elysium.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.EventHooks;

/** A physical pinion: finite travel, cover interception, and a short pinning wound. */
public final class BronzeFeatherEntity extends Projectile implements ItemSupplier {
    public BronzeFeatherEntity(EntityType<? extends BronzeFeatherEntity> type,Level level){super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override public ItemStack getItem(){return new ItemStack(ModItems.BRONZE_PINION.get());}
    @Override protected boolean canHitEntity(Entity e){return super.canHitEntity(e)&&!(e instanceof BronzeEagleEntity)&&(!(e instanceof Player p)||!p.isCreative()&&!p.isSpectator());}
    @Override public void tick(){
        super.tick();
        if(!level().isClientSide){
            if(tickCount>90||level().getDifficulty()==Difficulty.PEACEFUL||!level().hasChunkAt(BlockPos.containing(position().add(getDeltaMovement())))){discard();return;}
            var hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
            if(hit.getType()!=HitResult.Type.MISS&&!EventHooks.onProjectileImpact(this,hit)){onHit(hit);return;}
        }else if(tickCount%2==0)level().addParticle(ParticleTypes.SMOKE,getX(),getY(),getZ(),0,0,0);
        setPos(position().add(getDeltaMovement()));
    }
    @Override protected void onHit(HitResult hit){
        if(!(level() instanceof ServerLevel server))return;
        if(hit instanceof EntityHitResult e&&e.getEntity() instanceof LivingEntity target&&getOwner() instanceof LivingEntity owner)
            if(target.hurt(damageSources().mobProjectile(this,owner),6))target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,45,0));
        var p=hit.getLocation();server.sendParticles(ParticleTypes.ASH,p.x,p.y,p.z,8,.15,.15,.15,.01);discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putInt("Age",tickCount);}
    @Override protected void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);tickCount=t.getInt("Age");}
}
